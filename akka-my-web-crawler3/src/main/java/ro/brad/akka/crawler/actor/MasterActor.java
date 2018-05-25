/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
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
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.Globals.CrawlingType;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.Globals.WatchedActors;
import ro.brad.akka.crawler.model.Result;
import ro.brad.akka.crawler.model.Results;
import ro.brad.akka.crawler.actor.ActorMessages.IndexPage;
import ro.brad.akka.crawler.actor.ActorMessages.SearchText;
import ro.brad.akka.crawler.actor.ActorMessages.WhoCrawlsDomain;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsSite;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlSite;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlingOver;
import ro.brad.akka.crawler.actor.ActorMessages.KillCrawl;
import ro.brad.akka.crawler.actor.ActorMessages.KillMe;
import ro.brad.akka.crawler.actor.ActorMessages.SearchResults;

/**
 * @author marius
 *
 */
public class MasterActor extends AbstractActor {
	//Logger
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	//Actors and routers
	private final ActorRef indexer;
	private ActorRef searcher;
	private Router crawlerCoordinatorRouter;
	private List<Routee> routees;
	
	private final CrawlingType type;

	// Keep track of all domains
	private Set<String> allCrawlingSites = new HashSet<String>();
	private Set<String> crawlingSitesInProgress = new HashSet<String>();
	private boolean isCrawlingSessionFinished = false;

	// Use a map to distinguish between watched actors using their names
	private Map<ActorRef, WatchedActors> ref2Name = new HashMap<>();

	public MasterActor(IndexOpenMode mode, CrawlingType type) {
		this.type = type;
		this.routees = new ArrayList<Routee>();
		for (int i = 0; i < 3; i++) {
			ActorRef coordinator = getContext().actorOf(CrawlerCoordinatorActor.props(type),
					Globals.CRAWLER_COORDINATOR_ACTOR + i);
			getContext().watch(coordinator);
			this.ref2Name.put(coordinator, WatchedActors.CRAWLER_COORDINATOR);
			this.routees.add(new ActorRefRoutee(coordinator));
		}
		this.crawlerCoordinatorRouter = new Router(new RoundRobinRoutingLogic(), routees);

		File indexPath = new File(System.getProperty("java.io.tmpdir"), Globals.INDEX_DIR);
		this.indexer = getContext().actorOf(IndexerActor.props(indexPath.getAbsolutePath(), mode, type),
				Globals.INDEXER_ACTOR);
	}

	public static Props props(IndexOpenMode mode, CrawlingType type) {
		return Props.create(MasterActor.class, mode, type);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// ActorSystem Guardian =>
				.match(CrawlSite.class, this::onCrawlSite)
				.match(CrawlNewsSite.class, this::onCrawlNewsSite)
				.match(SearchText.class, this::onSearchText)
				.match(KillCrawl.class, this::onKillCrawl)
				.matchEquals(ActorMessages.IS_CRAWLING_SESSION_FINISHED, this::onIsCrawlingSessionFinished)
				// Crawler Coordinator =>
				.match(IndexPage.class, this::onIndexPage)
				.match(CrawlingOver.class, this::onCrawlingOver)
				.match(KillMe.class, this::onKillMe)
				// Indexer =>
				.matchEquals(ActorMessages.INDEX_COMMITTED, this::onIndexCommitted)
				.matchEquals(ActorMessages.INDEX_CLOSED, this::onIndexClosed)
				// DeathWatch =>
				.match(Terminated.class, this::onTerminated)
				// Searcher =>
				.match(SearchResults.class, this::onSearchResults)
				.matchEquals(ActorMessages.SEARCH_CLOSED, this::onSearchClosed)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onCrawlSite(CrawlSite mess) {
		if (!allCrawlingSites.contains(mess.getSiteURL())) {
			log.info("Crawling domain " + mess.getSiteURL());
			crawlingSitesInProgress.add(mess.getSiteURL());
			crawlerCoordinatorRouter.route(mess, getSelf());
		} else
			log.info("Domain {} already scheduled", mess.getSiteURL());
	}

	private void onCrawlNewsSite(CrawlNewsSite mess) {
		if (!allCrawlingSites.contains(mess.getDomain())) {
			log.info("Crawling news sites: " + mess.toString());
			crawlingSitesInProgress.add(mess.getDomain());
			crawlerCoordinatorRouter.route(mess, getSelf());
		} else
			log.info("Domain {} already scheduled", mess.getDomain());
	}

	private void onTerminated(Terminated mess) {
		if (ref2Name.get(mess.actor()).equals(WatchedActors.CRAWLER_COORDINATOR)) {
			ActorRef oldActor = mess.actor();
			String name = oldActor.path()
					.name();
			crawlerCoordinatorRouter = crawlerCoordinatorRouter.removeRoutee(oldActor);
			routees.remove(new ActorRefRoutee(oldActor));
			ref2Name.remove(oldActor);
			ActorRef newActor = getContext().actorOf(CrawlerCoordinatorActor.props(type), name);
			getContext().watch(newActor);
			routees.add(new ActorRefRoutee(mess.getActor()));
			ref2Name.put(newActor, WatchedActors.CRAWLER_COORDINATOR);
			crawlerCoordinatorRouter = crawlerCoordinatorRouter.addRoutee(new ActorRefRoutee(newActor));
		}
	}

	private void onSearchText(SearchText mess) {
		if (searcher == null) {
			File indexPath = new File(System.getProperty("java.io.tmpdir"), Globals.INDEX_DIR);
			this.searcher = getContext().actorOf(
					SearcherActor.props(indexPath.getAbsolutePath(), Globals.INDEX_SEARCH_FIELD),
					Globals.SEARCHER_ACTOR);
		}
		searcher.tell(mess, getSelf());
	}

	private void onSearchResults(SearchResults mess) {
		logResultsInFile(mess.getResults());
		searcher.tell(ActorMessages.SEARCH_CLOSE, getSelf());
	}

	private void onKillCrawl(KillCrawl mess) {
		Router coordinatorBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
		coordinatorBroadcaster.route(new WhoCrawlsDomain(mess.getDomain(), Globals.KILL_REASON), getSelf());
	}

	private void onKillMe(KillMe mess) {
		getSender().tell(Kill.getInstance(), ActorRef.noSender());
		crawlingSitesInProgress.remove(mess.getDomain());
		if (crawlingSitesInProgress.isEmpty()) {
			indexer.tell(ActorMessages.INDEX_COMMIT, getSelf());
		}
	}

	private void onCrawlingOver(CrawlingOver mess) {
		getSender().tell(Kill.getInstance(), ActorRef.noSender());
		crawlingSitesInProgress.remove(mess.getDomain());
		if (crawlingSitesInProgress.isEmpty()) {
			indexer.tell(ActorMessages.INDEX_COMMIT, getSelf());
		}
	}

	private void onIndexCommitted(Object mess) {
		log.info("Index committed");
		getSender().tell(ActorMessages.INDEX_CLOSE, getSelf());
	}

	private void onIndexClosed(Object mess) {
		log.info("Index closed");
		isCrawlingSessionFinished = true;
	}

	private void onSearchClosed(Object mess) {
		log.info("Searcher closed");
	}

	private void onIsCrawlingSessionFinished(Object mess) {
		getSender().tell(new Boolean(isCrawlingSessionFinished), getSelf());
	}

	private void onIndexPage(IndexPage mess) {
		indexer.forward(mess, getContext());
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
		super.preStart();
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
		super.postStop();
	}

}
