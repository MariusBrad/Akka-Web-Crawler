/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlNewsURL;
import ro.brad.akka.crawler.actor.ActorMessages.CrawlURL;
import ro.brad.akka.crawler.actor.ActorMessages.URLCrawled;
import ro.brad.akka.crawler.model.Digi24ParserCrawler;
import ro.brad.akka.crawler.model.EvzParserCrawler;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;
import ro.brad.akka.crawler.model.HTMLParserCrawler;
import ro.brad.akka.crawler.model.ToloParserCrawler;

/**
 * @author marius
 *
 */
public class CrawlerActor extends AbstractActor {
	//Logger
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static Props props() {
		return Props.create(CrawlerActor.class);
	}

	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				//Crawler Coordinator
				.match(CrawlURL.class, this::onCrawlURL)
				.match(CrawlNewsURL.class, this::onCrawlNewsURL)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	// Callback functions for CrawlerActor messages
	private void onCrawlURL(CrawlURL mess) {
		log.info("Crawling " + mess.getLink());
		getSender().tell(new URLCrawled(new HTMLParserCrawler().fetchPage(mess.getDomain(), mess.getLink())),
				getSelf());
	}

	private void onCrawlNewsURL(CrawlNewsURL mess) {
		log.info("Crawling news link: " + mess.getLink());
		if (mess.getAgency().equals(NewsAgencies.TOLO)) {
			getSender().tell(new URLCrawled(new ToloParserCrawler().fetchPage(mess.getDomain(), mess.getLink())),
					getSelf());
		} else if (mess.getAgency().equals(NewsAgencies.DIGI24)) {
			getSender().tell(new URLCrawled(new Digi24ParserCrawler().fetchPage(mess.getDomain(), mess.getLink())),
					getSelf());
		} else if (mess.getAgency().equals(NewsAgencies.EVZ)) {
			getSender().tell(new URLCrawled(new EvzParserCrawler().fetchPage(mess.getDomain(), mess.getLink())),
					getSelf());
		}
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
