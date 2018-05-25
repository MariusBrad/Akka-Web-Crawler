/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.ReachabilityEvent;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchText;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchResults;
import ro.brad.akka.crawler.cluster.ClusterMessages.AddSearcher;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchClose;
import ro.brad.akka.crawler.cluster.ClusterMessages.SearchClosed;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.NewsArticleSearcher;
import ro.brad.akka.crawler.model.Results;

/**
 * @author marius
 *
 */
public class BackendSearcherActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private Map<Address, NewsArticleSearcher> frontend2Searcher = new HashMap<>();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Backend Crawler
				.match(AddSearcher.class, this::onAddSearcher)
				.match(SearchText.class, this::onSearchText)
				.match(SearchClose.class, this::onSearchClose)
				// Cluster Events
				.match(CurrentClusterState.class, this::onCurrentClusterState)
				.match(MemberUp.class, this::onMemberUp)
				.match(UnreachableMember.class, this::onUnreachableMember)
				.match(MemberRemoved.class, this::onMemberRemoved)
				.match(MemberEvent.class, message -> {
					// ignore
				})
				.matchAny(mess -> log.info(getSelf().path()
						.name() + " received unknown message"))
				.build();
	}

	private void onAddSearcher(AddSearcher mess) {
		if (!frontend2Searcher.containsKey(mess.getFrontend())) {
			String localPath = Globals.INDEX_DIR + "@" + mess.getFrontend()
					.hostPort();
			File searchPath = new File(System.getProperty("java.io.tmpdir"), localPath);

			frontend2Searcher.put(mess.getFrontend(),
					new NewsArticleSearcher(searchPath.getAbsolutePath(), mess.getSearchField()));
		}

	}

	private void onSearchText(SearchText mess) {
		if (frontend2Searcher.containsKey(mess.getFrontend())) {
			log.info("Searching for '" + mess.getSearchString() + "' with " + Integer.toString(mess.getHitsCount())
					+ " hits");
			Results resultSet = frontend2Searcher.get(mess.getFrontend())
					.searchResults(mess.getSearchString(), mess.getHitsCount());
			getSender().tell(new SearchResults(resultSet), getSelf());
		} else
			log.info("This frontend {} has not yet been registered for searching", mess.getFrontend()
					.toString());
	}

	private void onSearchClose(SearchClose mess) {
		if (frontend2Searcher.containsKey(mess.getFrontend())) {
			frontend2Searcher.get(mess.getFrontend()).close();
			frontend2Searcher.remove(mess.getFrontend());
			getSender().tell(new SearchClosed(mess.getFrontend()), getSelf());
		} else
			log.info("This frontend {} has not yet been registered for searching", mess.getFrontend()
					.toString());
	}

	private void onCurrentClusterState(CurrentClusterState mess) {
		log.info("Current members: {}", mess.getMembers());
	}

	private void onMemberUp(MemberUp mess) {
		log.info("Member is Up: {}", mess.member());
	}

	private void onUnreachableMember(UnreachableMember mess) {
		log.info("Member detected as unreachable: {}", mess.member());
	}

	private void onMemberRemoved(MemberRemoved mess) {
		log.info("Member is Removed: {}", mess.member());
	}

	@Override
	public void preStart() throws Exception {
		cluster.subscribe(self(), MemberEvent.class, ReachabilityEvent.class);
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
		cluster.unsubscribe(self());
	}
}
