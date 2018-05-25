/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.actor.ActorMessages.IndexPage;
import ro.brad.akka.crawler.actor.ActorMessages.PageIndexed;
import ro.brad.akka.crawler.model.Globals.CrawlingType;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.NewsArticleIndexer;
import ro.brad.akka.crawler.model.PageIndexer;

/**
 * @author marius
 *
 */
public class IndexerActor extends AbstractActor {
	//Logger
	protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	//Indexer
	private final PageIndexer pagInd;
	
	public IndexerActor(String path, IndexOpenMode mode, CrawlingType type) {
		if (type.equals(CrawlingType.SIMPLE)) {
			this.pagInd = new PageIndexer(path, mode);
		}else if (type.equals(CrawlingType.NEWS)) {
			this.pagInd = new NewsArticleIndexer(path, mode);
		}
		else this.pagInd = new PageIndexer(path, mode);
	}
	
	public static Props props(String path, IndexOpenMode mode, CrawlingType type) {
		return Props.create(IndexerActor.class, path, mode, type);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				//Crawler Coordinator => Master =>
				.match(IndexPage.class, this::onIndexPage)
				.matchEquals(ActorMessages.INDEX_CLOSE, this::onIndexClose)
				.matchEquals(ActorMessages.INDEX_COMMIT, this::onIndexCommit)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}
	
	private void onIndexPage(IndexPage mess) {
		log.info("Indexing " + mess.getPage().getURL());
		pagInd.indexDoc(mess.getPage());
		getSender().tell(new PageIndexed(mess.getPage()), getSelf());
	}
	
	private void onIndexCommit(Object mess) {
		pagInd.commit();
		getSender().tell(ActorMessages.INDEX_COMMITTED, getSelf());
	}
	
	private void onIndexClose(Object mess) {
		pagInd.close();
		getSender().tell(ActorMessages.INDEX_CLOSED, getSelf());
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
		pagInd.close();
	}
	

}
