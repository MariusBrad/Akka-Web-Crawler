/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.cluster.ClusterMessages.CrawlNewsURL;
import ro.brad.akka.crawler.cluster.ClusterMessages.URLCrawled;
import ro.brad.akka.crawler.model.Digi24ParserCrawler;
import ro.brad.akka.crawler.model.EvzParserCrawler;
import ro.brad.akka.crawler.model.ToloParserCrawler;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;

/**
 * @author marius
 *
 */
public class ClusterCrawlerActor extends AbstractActor {

	//Logger
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static Props props() {
		return Props.create(ClusterCrawlerActor.class);
	}

	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				//Crawler Coordinator
				.match(CrawlNewsURL.class, this::onCrawlNewsURL)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
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
