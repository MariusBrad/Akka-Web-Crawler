/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.model.NewsArticleSearcher;
import ro.brad.akka.crawler.actor.ActorMessages.SearchResults;
import ro.brad.akka.crawler.actor.ActorMessages.SearchText;

/**
 * @author marius
 *
 */
public class SearcherActor extends AbstractActor {
	//Logger
	protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	//Searcher
	private final NewsArticleSearcher articleSearcher;

	public SearcherActor(String path, String searchField) {

		this.articleSearcher = new NewsArticleSearcher(path, searchField);
	}

	public static Props props(String path, String searchField) {
		return Props.create(SearcherActor.class, path, searchField);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Master =>
				.match(SearchText.class, this::onSearchText)
				.matchEquals(ActorMessages.SEARCH_CLOSE, this::onSearchClose)
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onSearchText(SearchText mess) {
		log.info(
				"Searching for " + mess.getSearchString() + " with " + Integer.toString(mess.getHitsCount()) + " hits");
		getSender().tell(new SearchResults(articleSearcher.searchResults(mess.getSearchString(), mess.getHitsCount())),
				getSelf());
	}

	private void onSearchClose(Object mess) {
		articleSearcher.close();
		getSender().tell(ActorMessages.SEARCH_CLOSED, getSelf());
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
		articleSearcher.close();
	}

}
