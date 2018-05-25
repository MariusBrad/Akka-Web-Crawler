/**
 * 
 */
package ro.brad.akka.crawler.actor;

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
import ro.brad.akka.crawler.model.Globals.CrawlingType;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;
import ro.brad.akka.crawler.model.Globals.WatchedActors;
import ro.brad.akka.crawler.model.NewsArticlePage;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.actor.ActorMessages.ScheduleStartURL;
import ro.brad.akka.crawler.actor.ActorMessages.StartURLScheduled;
import ro.brad.akka.crawler.actor.ActorMessages.URLCrawled;
import ro.brad.akka.crawler.actor.ActorMessages.WhoCrawlsDomain;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlURL;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsURL;
import ro.brad.akka.crawler.actor.ActorMessages.IndexPage;
import ro.brad.akka.crawler.actor.ActorMessages.BatchURLsReceived;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsSite;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlSite;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlingOver;
import ro.brad.akka.crawler.actor.ActorMessages.ScheduleScrapedURLs;
import ro.brad.akka.crawler.actor.ActorMessages.PageIndexed;
import ro.brad.akka.crawler.actor.ActorMessages.KillMe;

/**
 * @author marius
 *
 */
public class CrawlerCoordinatorActor extends AbstractActor {
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
	private Queue<String> domainsInProgress = new LinkedList<String>();
	private Queue<CrawlNewsSite> newsSitesInProgress = new LinkedList<CrawlNewsSite>();

	private final CrawlingType type;
	private NewsAgencies agency;
	private String currentDomain = null;

	public CrawlerCoordinatorActor(CrawlingType type) {
		this.type = type;
		this.routees = new ArrayList<Routee>();
		for (int i = 0; i < 3; i++) {
			ActorRef crawler = getContext().actorOf(CrawlerActor.props(), Globals.CRAWLER_ACTOR + i);
			getContext().watch(crawler);
			this.ref2Name.put(crawler, WatchedActors.CRAWLER);
			this.routees.add(new ActorRefRoutee(crawler));
		}
		this.crawlerRouter = new Router(new RoundRobinRoutingLogic(), routees);
		this.scheduler = getContext().actorOf(SchedulerActor.props(), Globals.SCHEDULER_ACTOR);
	}

	public static Props props(CrawlingType type) {
		return Props.create(CrawlerCoordinatorActor.class, type);
	}

	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				// Master =>
				.match(CrawlSite.class, this::onCrawlSite)
				.match(CrawlNewsSite.class, this::onCrawlNewsSite)
				.match(WhoCrawlsDomain.class, this::onWhoCrawlsDomain)
				// Scheduler =>
				.match(StartURLScheduled.class, this::onStartURLScheduled)
				.match(BatchURLsReceived.class, this::onBatchURLsReceived)
				.matchEquals(ActorMessages.SCRAPED_URLs_SCHEDULED, this::onScrapedURLsScheduled)
				.matchEquals(ActorMessages.END_OF_SCHEDULE, this::onEndOfSchedule)
				// Crawlers =>
				.match(URLCrawled.class, this::onURLCrawled)
				// Indexer => Master =>
				.match(PageIndexed.class, this::onPageIndexed)
				// DeathWatch =>
				.match(Terminated.class, this::onTerminated)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onCrawlSite(CrawlSite mess) {
		if (type.equals(CrawlingType.SIMPLE)) {
			// Check whether this site has already been crawled or not
			if (!allDomains.contains(mess.getSiteURL())) {
				domainsInProgress.add(mess.getSiteURL());
				if (currentDomain == null) {
					currentDomain = domainsInProgress.poll();
					scheduler.tell(new ScheduleStartURL(currentDomain), getSelf());
				}
			} else
				log.info("This site: {} is already being scheduled for crawling", mess.getSiteURL());
		} else {
			log.info("No" + Globals.SIMPLE_EXEC + " Simple crawling running. I'm sorry. Terminating...");
		}

	}

	private void onCrawlNewsSite(CrawlNewsSite mess) {
		if (type.equals(CrawlingType.NEWS)) {
			// Check whether this domain has already been crawled or not
			if (!allDomains.contains(mess.getDomain())) {
				newsSitesInProgress.add(mess);
				if (currentDomain == null) {
					CrawlNewsSite o = newsSitesInProgress.poll();
					currentDomain = o.getDomain();
					agency = o.getAgency();
					for (String site : o.getSites()) {
						scheduler.tell(new ScheduleStartURL(site), getSelf());
					}
				}
			} else
				log.info("This site: {} is already being scheduled for crawling", mess.getDomain());
		} else {
			log.info("No " + Globals.NEWS_EXEC + " crawling running. I'm sorry. Terminating...");
		}
	}

	private void onStartURLScheduled(StartURLScheduled mess) {
		if (type.equals(CrawlingType.SIMPLE)) {
			crawlerRouter.route(new CrawlURL(currentDomain, mess.getURL()), getSelf());
		} else if (type.equals(CrawlingType.NEWS)) {
			crawlerRouter.route(new CrawlNewsURL(agency, currentDomain, mess.getURL()), getSelf());
		}
	}

	private void onURLCrawled(URLCrawled mess) {
		if (mess.getPage() != null) {
			scheduler.tell(new ScheduleScrapedURLs(mess.getPage()
					.getURLsToFollow()), getSelf());

			if (type.equals(CrawlingType.SIMPLE)) {
				getContext().getParent()
						.tell(new IndexPage(mess.getPage()), getSelf());
			} else if (type.equals(CrawlingType.NEWS)) {
				if (mess.getPage() instanceof NewsArticlePage) {
					NewsArticlePage newsPage = (NewsArticlePage) mess.getPage();
					if (newsPage.getArticleContent() != null && newsPage.getArticleTitle() != null
							&& newsPage.getAuthor() != null && newsPage.getPublishedOn() != null) {
						getContext().getParent()
								.tell(new IndexPage(mess.getPage()), getSelf());
					}
				}
			}
		}
	}

	private void onPageIndexed(PageIndexed mess) {
		scheduler.forward(mess, getContext());
	}

	private void onBatchURLsReceived(BatchURLsReceived mess) {
		for (String url : mess.getBatch()) {
			if (type.equals(CrawlingType.SIMPLE)) {
				crawlerRouter.route(new CrawlURL(currentDomain, url), getSelf());
			} else if (type.equals(CrawlingType.NEWS)) {
				crawlerRouter.route(new CrawlNewsURL(agency, currentDomain, url), getSelf());
			}
		}
	}

	private void onTerminated(Terminated mess) {
		if (ref2Name.get(mess.actor()).equals(WatchedActors.CRAWLER)) {
			ActorRef oldActor = mess.actor();
			String name = oldActor.path()
					.name();
			crawlerRouter = crawlerRouter.removeRoutee(oldActor);
			routees.remove(new ActorRefRoutee(oldActor));
			ref2Name.remove(oldActor);
			ActorRef newActor = getContext().actorOf(CrawlerActor.props(), name);
			getContext().watch(newActor);
			routees.add(new ActorRefRoutee(mess.getActor()));
			ref2Name.put(newActor, WatchedActors.CRAWLER);
			crawlerRouter = crawlerRouter.addRoutee(new ActorRefRoutee(newActor));
		}
	}

	private void onWhoCrawlsDomain(WhoCrawlsDomain mess) {
		if (currentDomain.equals(mess.getDomain())) {
			if (mess.getReason().equals(Globals.KILL_REASON)) {
				// killing the crawlers first
				Router crawlerBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
				crawlerBroadcaster.route(Kill.getInstance(), ActorRef.noSender());
				// then kill the coordinator
				getSender().tell(new KillMe(currentDomain), getSelf());
			}
		}
	}

	private void onEndOfSchedule(Object mess) {
		getContext().getParent()
				.tell(new CrawlingOver(currentDomain), getSelf());
		if (type.equals(CrawlingType.SIMPLE)) {
			if (!domainsInProgress.isEmpty()) {
				// Get next domain in queue
				currentDomain = domainsInProgress.poll();
				scheduler.tell(new ScheduleStartURL(currentDomain), getSelf());
			} else {
				currentDomain = null;
				Router crawlerBroadcaster = new Router(new BroadcastRoutingLogic(), routees);
				crawlerBroadcaster.route(Kill.getInstance(), ActorRef.noSender());
			}
		} else if (type.equals(CrawlingType.NEWS)) {
			if (!newsSitesInProgress.isEmpty()) {
				// Get next news domain in queue
				CrawlNewsSite o = newsSitesInProgress.poll();
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
	}

	private void onScrapedURLsScheduled(Object mess) {
		scheduler.tell(ActorMessages.REQUEST_BATCH_URLs, getSelf());
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
