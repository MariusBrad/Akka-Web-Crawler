/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Props;
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
import akka.routing.FromConfig;
import ro.brad.akka.crawler.cluster.ClusterMessages.KillCrawl;
import ro.brad.akka.crawler.cluster.ClusterMessages.WhoCrawlsDomain;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlNewsSite;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlJob;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlingOver;
import ro.brad.akka.crawler.cluster.ClusterMessages.AddSearcher;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchResults;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchClosed;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchText;
import ro.brad.akka.crawler.cluster.ClusterMessages.AddIndexer;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexCommit;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexClose;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexCommitted;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexClosed;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.Result;
import ro.brad.akka.crawler.model.Results;

/**
 * @author marius
 *
 */
public class FrontendActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private final ActorRef backendCrawlerAdaptiveRouter = getContext().actorOf(FromConfig.getInstance()
			.props(Props.empty()), "BackendCrawlerAdaptiveRouter");
	private final ActorRef backendCrawlerBroadcastRouter = getContext().actorOf(FromConfig.getInstance()
			.props(Props.empty()), "BackendCrawlerBroadcastRouter");

	private final Set<Address> storageNodes = new HashSet<Address>();
	private Address storageNodeAddress = null;

	private Set<String> domainsInProgress = new HashSet<String>();
	private boolean isCrawlingSessionFinished = false;

	public FrontendActor() {

	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(CrawlNewsSite.class, this::onCrawlNewsSite)
				.match(CrawlingOver.class, this::onCrawlingOver)
				.match(IndexCommitted.class, this::onIndexCommitted)
				.match(IndexClosed.class, this::onIndexClosed)
				.match(SearchText.class, this::onSearchText)
				.match(SearchResults.class, this::onSearchResults)
				.match(SearchClosed.class, this::onSearchClosed)
				.match(KillCrawl.class, this::onKillCrawl)
				.matchEquals(ClusterMessages.IS_CRAWLING_SESSION_FINISHED, this::onIsCrawlingSessionFinished)
				// Cluster Events
				.match(CurrentClusterState.class, this::onCurrentClusterState)
				.match(MemberUp.class, this::onMemberUp)
				.match(UnreachableMember.class, this::onUnreachableMember)
				.match(MemberRemoved.class, this::onMemberRemoved)
				.match(ReachableMember.class, this::onReachableMember)
				.match(MemberEvent.class, message -> {
					// ignore
				})
				.match(ReachabilityEvent.class, mess -> {
					// ignore
				})
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onCrawlNewsSite(CrawlNewsSite mess) {
		if (!storageNodes.isEmpty()) {
			if (storageNodeAddress == null) {
				List<Address> nodesList = new ArrayList<>(storageNodes);
				storageNodeAddress = nodesList.get(ThreadLocalRandom.current()
						.nextInt(nodesList.size()));
				ActorSelection indexer = getContext().actorSelection(storageNodeAddress + Globals.BACKEND_INDEXER_PATH);
				indexer.tell(new AddIndexer(getContext().system().provider().getDefaultAddress(), IndexOpenMode.CREATE_OR_APPEND), getSelf());
			}
			if (!domainsInProgress.contains(mess.getDomain())) {
				domainsInProgress.add(mess.getDomain());
				log.info("Issuing crawl job for {} ", mess.toString());
				backendCrawlerAdaptiveRouter
						.tell(new CrawlJob(mess.getAgency(), mess.getDomain(), mess.getSites(), getContext().system().provider().getDefaultAddress(), storageNodeAddress), getSelf());
			}
		}
	}

	private void onCrawlingOver(CrawlingOver mess) {
		log.info("Crawl session for {} terminated", mess.getDomain());
		domainsInProgress.remove(mess.getDomain());
		if (domainsInProgress.isEmpty()) {
			ActorSelection indexer = getContext().actorSelection(storageNodeAddress + Globals.BACKEND_INDEXER_PATH);
			indexer.tell(new IndexCommit(getContext().system().provider().getDefaultAddress()), getSelf());
		}
	}

	private void onIndexCommitted(IndexCommitted mess) {
		log.info("Index committed");
		ActorSelection indexer = getContext().actorSelection(storageNodeAddress + Globals.BACKEND_INDEXER_PATH);
		indexer.tell(new IndexClose(getContext().system().provider().getDefaultAddress()), getSelf());
	}

	private void onIndexClosed(IndexClosed mess) {
		log.info("Index closed");
		isCrawlingSessionFinished = true;
	}

	private void onSearchClosed(SearchClosed mess) {
		log.info("Searcher closed");
	}

	private void onSearchText(SearchText mess) {
		log.info("Searching for text '{}' in crawled domains", mess.getSearchString());
		ActorSelection searcher = getContext().actorSelection(storageNodeAddress + Globals.BACKEND_SEARCHER_PATH);
		searcher.tell(new AddSearcher(getContext().system().provider().getDefaultAddress(), Globals.INDEX_SEARCH_FIELD), getSelf());
		searcher.tell(mess, getSelf());
	}

	private void onSearchResults(SearchResults mess) {
		log.info("Search results received.");
		logResultsInFile(mess.getResults());
		ActorSelection searcher = getContext().actorSelection(storageNodeAddress + Globals.BACKEND_SEARCHER_PATH);
		searcher.tell(new SearchClosed(getContext().system().provider().getDefaultAddress()), getSelf());
	}

	private void onIsCrawlingSessionFinished(Object mess) {
		getSender().tell(new Boolean(isCrawlingSessionFinished), getSelf());
	}

	private void onKillCrawl(KillCrawl mess) {
		log.info("Killing crawl session for domain {}", mess.getDomain());
		backendCrawlerBroadcastRouter.tell(new WhoCrawlsDomain(getContext().system().provider().getDefaultAddress(), storageNodeAddress, mess.getDomain(), Globals.KILL_REASON), getSelf());
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

	private void logResultsInFile(Results results) {
		try {
			File file = new File(System.getProperty("java.io.tmpdir"), Globals.RESULTS_FILENAME);
			FileWriter logSearchResults = new FileWriter(file);
			int nrt = 0;
			for (Result result : results) {
				logSearchResults
						.write(++nrt + ". " + result.toString() + System.lineSeparator() + System.lineSeparator());
			}
			logSearchResults.close();
			log.info("Results have been written into file " + file.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
