/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Kill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import ro.brad.akka.crawler.cluster.ClusterMessages.BatchURLsReceived;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlNewsURL;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlJob;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlingOver;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexPage;
import ro.brad.akka.crawler.cluster.ClusterMessages.KillMe;
import ro.brad.akka.crawler.cluster.ClusterMessages.PageIndexed;
import ro.brad.akka.crawler.cluster.ClusterMessages.ScheduleScrapedURLs;
import ro.brad.akka.crawler.cluster.ClusterMessages.ScheduleStartURL;
import ro.brad.akka.crawler.cluster.ClusterMessages.StartURLScheduled;
import ro.brad.akka.crawler.cluster.ClusterMessages.URLCrawled;
import ro.brad.akka.crawler.cluster.ClusterMessages.WhoCrawlsDomain;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.NewsArticlePage;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;
import ro.brad.akka.crawler.model.Globals.WatchedActors;

/**
 * @author marius
 *
 */
public class ClusterCoordinatorActor extends AbstractActor {

	// Logger
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	// Actors and routers
	private final ActorRef scheduler;
	private Router crawlerRouter;
	private List<Routee> routees;

	// Use a map to distinguish between watched actors using their names
	private Map<ActorRef, WatchedActors> ref2Name = new HashMap<>();

	// Track crawling requests over domains
	private Set<String> allDomains = new HashSet<String>();
	private Queue<CrawlJob> jobsInProgress = new LinkedList<CrawlJob>();

	private NewsAgencies agency;
	private String currentDomain = null;
	private Address currentFrontend = null;
	private Address currentStorage = null;

	public ClusterCoordinatorActor() {
		this.routees = new ArrayList<Routee>();
		for (int i = 0; i < 3; i++) {
			ActorRef crawler = getContext().actorOf(ClusterCrawlerActor.props(), Globals.CRAWLER_ACTOR + i);
			getContext().watch(crawler);
			this.ref2Name.put(crawler, WatchedActors.CRAWLER);
			this.routees.add(new ActorRefRoutee(crawler));
		}
		this.crawlerRouter = new Router(new RoundRobinRoutingLogic(), routees);
		this.scheduler = getContext().actorOf(ClusterSchedulerActor.props(), Globals.SCHEDULER_ACTOR);
	}

	public static Props props() {
		return Props.create(ClusterCoordinatorActor.class);
	}

	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				// Master =>
				.match(CrawlJob.class, this::onCrawlJob)
				.match(WhoCrawlsDomain.class, this::onWhoCrawlsDomain)
				// Scheduler =>
				.match(StartURLScheduled.class, this::onStartURLScheduled)
				.match(BatchURLsReceived.class, this::onBatchURLsReceived)
				.matchEquals(ClusterMessages.SCRAPED_URLs_SCHEDULED, this::onScrapedURLsScheduled)
				.matchEquals(ClusterMessages.END_OF_SCHEDULE, this::onEndOfSchedule)
				// Crawlers =>
				.match(URLCrawled.class, this::onURLCrawled)
				// Backend Indexer
				.match(PageIndexed.class, this::onPageIndexed)
				// DeathWatch =>
				.match(Terminated.class, this::onTerminated)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onCrawlJob(CrawlJob mess) {
		// Check whether this domain has already been crawled or not
		if (!allDomains.contains(mess.getDomain())) {
			jobsInProgress.add(mess);
			if (currentDomain == null) {
				CrawlJob o = jobsInProgress.poll();
				currentFrontend = o.getFrontend();
				currentStorage = o.getStorage();
				currentDomain = o.getDomain();
				agency = o.getAgency();
				for (String site : o.getSites()) {
					scheduler.tell(new ScheduleStartURL(site), getSelf());
				}
			}
		} else
			log.info("This site: {} is already being scheduled for crawling", mess.getDomain());

	}

	private void onStartURLScheduled(StartURLScheduled mess) {
		crawlerRouter.route(new CrawlNewsURL(agency, currentDomain, mess.getURL()), getSelf());
	}

	private void onURLCrawled(URLCrawled mess) {
		if (mess.getPage() != null) {
			scheduler.tell(new ScheduleScrapedURLs(mess.getPage()
					.getURLsToFollow()), getSelf());

			if (mess.getPage() instanceof NewsArticlePage) {
				NewsArticlePage newsPage = (NewsArticlePage) mess.getPage();
				if (newsPage.getArticleContent() != null && newsPage.getArticleTitle() != null
						&& newsPage.getAuthor() != null && newsPage.getPublishedOn() != null) {
					getContext().getParent()
							.tell(new IndexPage(currentFrontend, currentStorage, mess.getPage()), getSelf());
				}
			}
		}
	}

	private void onPageIndexed(PageIndexed mess) {
		scheduler.forward(mess, getContext());
	}

	private void onBatchURLsReceived(BatchURLsReceived mess) {
		for (String url : mess.getBatch()) {
			crawlerRouter.route(new CrawlNewsURL(agency, currentDomain, url), getSelf());
		}
	}

	private void onTerminated(Terminated mess) {
		if (ref2Name.get(mess.actor())
				.equals(WatchedActors.CRAWLER)) {
			ActorRef oldActor = mess.actor();
			String name = oldActor.path()
					.name();
			crawlerRouter = crawlerRouter.removeRoutee(oldActor);
			routees.remove(new ActorRefRoutee(oldActor));
			ref2Name.remove(oldActor);
			ActorRef newActor = getContext().actorOf(ClusterCrawlerActor.props(), name);
			getContext().watch(newActor);
			routees.add(new ActorRefRoutee(mess.getActor()));
			ref2Name.put(newActor, WatchedActors.CRAWLER);
			crawlerRouter = crawlerRouter.addRoutee(new ActorRefRoutee(newActor));
		}
	}

	private void onWhoCrawlsDomain(WhoCrawlsDomain mess) {
		if (currentDomain != null) {
			if (currentDomain.equals(mess.getDomain()) && currentFrontend.equals(mess.getFrontend())
					&& currentStorage.equals(mess.getStorage())) {
				if (mess.getReason()
						.equals(Globals.KILL_REASON)) {
					// killing the crawlers first
					Router crawlerBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
					crawlerBroadcaster.route(Kill.getInstance(), ActorRef.noSender());
					// then kill the coordinator
					getSender().tell(new KillMe(currentDomain, currentFrontend, currentStorage), getSelf());
				}
			}
		}
	}

	private void onEndOfSchedule(Object mess) {
		getContext().getParent()
				.tell(new CrawlingOver(currentDomain, currentFrontend, currentStorage), getSelf());
		if (!jobsInProgress.isEmpty()) {
			// Get next news domain in queue
			CrawlJob o = jobsInProgress.poll();
			currentFrontend = o.getFrontend();
			currentStorage = o.getStorage();
			currentDomain = o.getDomain();
			agency = o.getAgency();
			for (String site : o.getSites()) {
				scheduler.tell(new ScheduleStartURL(site), getSelf());
			}
		} else {
			currentDomain = null;
			Router crawlerBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
			crawlerBroadcaster.route(Kill.getInstance(), ActorRef.noSender());
		}
	}

	private void onScrapedURLsScheduled(Object mess) {
		scheduler.tell(ClusterMessages.REQUEST_BATCH_URLs, getSelf());
	}

	@Override
	public void preStart() throws Exception {
		// TODO Auto-generated method stub
		super.preStart();
	}

	@Override
	public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
		// TODO Auto-generated method stub
		super.preRestart(reason, message);
	}

	@Override
	public void postRestart(Throwable reason) throws Exception {
		// TODO Auto-generated method stub
		super.postRestart(reason);
	}

	@Override
	public void postStop() throws Exception {
		// TODO Auto-generated method stub
		super.postStop();
	}
}
