/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.actor.ActorMessages.StartURLScheduled;
import ro.brad.akka.crawler.actor.ActorMessages.BatchURLsReceived;
import ro.brad.akka.crawler.actor.ActorMessages.PageIndexed;
import ro.brad.akka.crawler.actor.ActorMessages.ScheduleScrapedURLs;
import ro.brad.akka.crawler.actor.ActorMessages.ScheduleStartURL;

/**
 * @author marius
 *
 */
public class SchedulerActor extends AbstractActor {
	//Logger
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	//Track all links to be crawled
	private Set<String> linksToVisit = new HashSet<String>();
	private Set<String> linksInProgress = new HashSet<String>();
	private Set<String> allLinks = new HashSet<String>();

	public static Props props() {
		return Props.create(SchedulerActor.class);
	}
	
	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				//Crawler Coordinator
				.match(ScheduleStartURL.class, this::onScheduleStartURL)
				.match(ScheduleScrapedURLs.class, this::onScheduleScrapedURLs)
				.matchEquals(ActorMessages.REQUEST_BATCH_URLs, this::onRequestBatchURLs)
				//Indexer => Master => Crawler Coordinator
				.match(PageIndexed.class, this::onPageIndexed)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onScheduleStartURL(ScheduleStartURL mess) {
		log.info("Scheduling first URL: " + mess.getStartURL());
		addLink(mess.getStartURL());
		getSender().tell(new StartURLScheduled(getNext()), getSelf());
	}
	
	private void onScheduleScrapedURLs(ScheduleScrapedURLs mess) {
		log.info("Scheduling " + mess.getScrapedURLs().size() + " new scraped URLs");
		addAllLinks(mess.getScrapedURLs());
		getSender().tell(ActorMessages.SCRAPED_URLs_SCHEDULED, getSelf());
	}
	
	private void onRequestBatchURLs(Object mess) {
		if (!this.isFinished()) {
			getSender().tell(new BatchURLsReceived(getNextBatch()), getSelf());
		}else {
			getSender().tell(ActorMessages.END_OF_SCHEDULE, getSelf());
		}
	}
	
	private void onPageIndexed(PageIndexed mess) {
		finishedLink(mess.getPage().getURL());
	}
	
	public void addLink(String link) {
		if (!allLinks.contains(link)) {
			linksToVisit.add(link);
			allLinks.add(link);
		}
	}

	public void addAllLinks(Collection<String> links) {
		for (String link : links) {
			addLink(link);
		}
	}

	public void finishedLink(String page) {
		linksInProgress.remove(page);
	}

	public String getNext() {
		if (linksToVisit.isEmpty()) {
			return null;
		} else {
			String next = linksToVisit.iterator()
					.next();
			linksToVisit.remove(next);
			linksInProgress.add(next);
			return next;
		}
	}

	public Collection<String> getNextBatch() {
		Set<String> links = new HashSet<String>();
		links.addAll(linksToVisit);
		linksToVisit.clear();
		linksInProgress.addAll(links);
		return links;
	}

	public boolean isFinished() {
		return linksToVisit.isEmpty() && linksInProgress.isEmpty();
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
