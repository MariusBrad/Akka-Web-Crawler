/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Kill;
import akka.actor.Terminated;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.ReachabilityEvent;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import ro.brad.akka.crawler.cluster.ClusterMessages.KillMe;
import ro.brad.akka.crawler.cluster.ClusterMessages.WhoCrawlsDomain;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexPage;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlingOver;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlJob;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.WatchedActors;

/**
 * @author marius
 *
 */
public class BackendCrawlerActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	// get cluster context
	private final Cluster cluster = Cluster.get(getContext().system());
	// Keep track of all domains
	private Set<String> allCrawlingSites = new HashSet<String>();
	private Set<String> crawlingSitesInProgress = new HashSet<String>();
	// trace storage nodes addresses
	private final Set<Address> storageNodes = new HashSet<Address>();

	// private Address NodeAddress;
	// private Address searcherNodeAddress;

	private Router crawlerCoordinatorRouter;
	private List<Routee> routees;

	// Use a map to distinguish between watched actors using their names
	private Map<ActorRef, WatchedActors> ref2Name = new HashMap<>();

	public BackendCrawlerActor() {
		this.routees = new ArrayList<Routee>();
		for (int i = 0; i < 3; i++) {
			ActorRef coordinator = getContext().actorOf(ClusterCoordinatorActor.props(),
					Globals.CRAWLER_COORDINATOR_ACTOR + i);
			getContext().watch(coordinator);
			this.ref2Name.put(coordinator, WatchedActors.CRAWLER_COORDINATOR);
			this.routees.add(new ActorRefRoutee(coordinator));
		}
		this.crawlerCoordinatorRouter = new Router(new RoundRobinRoutingLogic(), routees);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Frontend =>
				.match(CrawlJob.class, this::onCrawlJob)
				.match(WhoCrawlsDomain.class, this::onWhoCrawlsDomain)
				// Crawler Coordinator =>
				.match(IndexPage.class, this::onIndexPage)
				.match(CrawlingOver.class, this::onCrawlingOver)
				.match(KillMe.class, this::onKillMe)
				// Backend Indexer =>
				// DeathWatch =>
				.match(Terminated.class, this::onTerminated)
				// Backend Searcher =>
				// Cluster Events
				.match(CurrentClusterState.class, this::onCurrentClusterState)
				.match(MemberUp.class, this::onMemberUp)
				.match(UnreachableMember.class, this::onUnreachableMember)
				.match(MemberRemoved.class, this::onMemberRemoved)
				.match(ReachableMember.class, this::onReachableMember)
				.match(MemberEvent.class, message -> {
					// ignore
				})
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onCrawlJob(CrawlJob mess) {
		if (!allCrawlingSites.contains(mess.getDomain())) {
			allCrawlingSites.add(mess.getDomain());
			log.info("Crawling news sites: " + mess.toString());
			crawlingSitesInProgress.add(mess.getDomain());
			crawlerCoordinatorRouter.route(mess, getSelf());
		} else
			log.info("Domain {} already scheduled", mess.getDomain());
	}

	private void onTerminated(Terminated mess) {
		if (ref2Name.get(mess.actor())
				.equals(WatchedActors.CRAWLER_COORDINATOR)) {
			ActorRef oldActor = mess.actor();
			String name = oldActor.path()
					.name();
			crawlerCoordinatorRouter = crawlerCoordinatorRouter.removeRoutee(oldActor);
			routees.remove(new ActorRefRoutee(oldActor));
			ref2Name.remove(oldActor);
			ActorRef newActor = getContext().actorOf(ClusterCoordinatorActor.props(), name);
			getContext().watch(newActor);
			routees.add(new ActorRefRoutee(mess.getActor()));
			ref2Name.put(newActor, WatchedActors.CRAWLER_COORDINATOR);
			crawlerCoordinatorRouter = crawlerCoordinatorRouter.addRoutee(new ActorRefRoutee(newActor));
		}
	}

	private void onWhoCrawlsDomain(WhoCrawlsDomain mess) {
		Router coordinatorBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
		coordinatorBroadcaster.route(mess, getSelf());
	}

	private void onKillMe(KillMe mess) {
		getSender().tell(Kill.getInstance(), ActorRef.noSender());
		crawlingSitesInProgress.remove(mess.getDomain());
		ActorSelection frontend = getContext().actorSelection(mess.getFrontend() + Globals.FRONTEND_PATH);
		frontend.tell(new CrawlingOver(mess.getDomain(), mess.getFrontend(), mess.getStorage()), getSelf());
	}

	private void onCrawlingOver(CrawlingOver mess) {
		getSender().tell(Kill.getInstance(), ActorRef.noSender());
		crawlingSitesInProgress.remove(mess.getDomain());
		ActorSelection frontend = getContext().actorSelection(mess.getFrontend() + Globals.FRONTEND_PATH);
		frontend.tell(mess, getSelf());
	}

	private void onIndexPage(IndexPage mess) {
		ActorSelection indexer = getContext().actorSelection(mess.getStorage() + Globals.BACKEND_INDEXER_PATH);
		indexer.forward(mess, getContext());
	}

	private void onCurrentClusterState(CurrentClusterState mess) {
		log.info("Current members: {}", mess.getMembers());
		storageNodes.clear();
		for (Member member : mess.getMembers()) {
			if (member.hasRole("backend-storage") && member.status()
					.equals(MemberStatus.up())) {
				storageNodes.add(member.address());
			}
		}
	}

	private void onMemberUp(MemberUp mess) {
		log.info("Member is Up: {}", mess.member());
		if (mess.member()
				.hasRole("backend-storage")) {
			storageNodes.add(mess.member()
					.address());
		}
	}

	private void onUnreachableMember(UnreachableMember mess) {
		log.info("Member detected as unreachable: {}", mess.member());
		storageNodes.remove(mess.member()
				.address());
	}

	private void onMemberRemoved(MemberRemoved mess) {
		log.info("Member is Removed: {}", mess.member());
		storageNodes.remove(mess.member()
				.address());
	}

	private void onReachableMember(ReachableMember mess) {
		log.info("Member is reachable again: {}", mess.member());
		if (mess.member()
				.hasRole("backend-storage")) {
			storageNodes.add(mess.member()
					.address());
		}
	}

	@Override
	public void preStart() throws Exception {
		cluster.subscribe(getSelf(), MemberEvent.class, ReachabilityEvent.class);
	}

	@Override
	public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
		super.preRestart(reason, message);
	}

	@Override
	public void postRestart(Throwable reason) throws Exception {
		super.postRestart(reason);
	}

	@Override
	public void postStop() throws Exception {
		cluster.unsubscribe(getSelf());
	}

}
