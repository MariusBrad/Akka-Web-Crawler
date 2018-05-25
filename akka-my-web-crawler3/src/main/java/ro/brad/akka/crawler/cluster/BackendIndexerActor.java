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
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexPage;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexClose;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexCommit;
import ro.brad.akka.crawler.cluster.ClusterMessages.PageIndexed;
import ro.brad.akka.crawler.cluster.ClusterMessages.AddIndexer;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexClosed;
import ro.brad.akka.crawler.cluster.ClusterMessages.IndexCommitted;
import ro.brad.akka.crawler.model.Globals;
import ro.brad.akka.crawler.model.NewsArticleIndexer;
import ro.brad.akka.crawler.model.PageIndexer;

/**
 * @author marius
 *
 */
public class BackendIndexerActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private Map<Address, PageIndexer> frontend2Indexer = new HashMap<>();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Backend Crawler =>
				.match(AddIndexer.class, this::onAddIndexer)
				.match(IndexPage.class, this::onIndexPage)
				.match(IndexClose.class, this::onIndexClose)
				.match(IndexCommit.class, this::onIndexCommit)
				// Cluster Events =>
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

	private void onAddIndexer(AddIndexer mess) {
		if (!frontend2Indexer.containsKey(mess.getFrontend())) {
			String localPath = Globals.INDEX_DIR + "@" + mess.getFrontend()
					.hostPort();
			File indexPath = new File(System.getProperty("java.io.tmpdir"), localPath);

			frontend2Indexer.put(mess.getFrontend(),
					new NewsArticleIndexer(indexPath.getAbsolutePath(), mess.getMode()));
		}
	}

	private void onIndexPage(IndexPage mess) {
		if (frontend2Indexer.containsKey(mess.getFrontend())) {
			log.info("Indexing " + mess.getPage()
					.getURL());
			frontend2Indexer.get(mess.getFrontend())
					.indexDoc(mess.getPage());
			getSender().tell(new PageIndexed(mess.getFrontend(), mess.getPage()), getSelf());
		} else
			log.info("This frontend {} has not yet been registered for indexing", mess.getFrontend()
					.toString());
	}

	private void onIndexCommit(IndexCommit mess) {
		if (frontend2Indexer.containsKey(mess.getFrontend())) {
			frontend2Indexer.get(mess.getFrontend())
					.commit();
			getSender().tell(new IndexCommitted(mess.getFrontend()), getSelf());
		} else
			log.info("This frontend {} has not yet been registered for indexing", mess.getFrontend()
					.toString());
	}

	private void onIndexClose(IndexClose mess) {
		if (frontend2Indexer.containsKey(mess.getFrontend())) {
			frontend2Indexer.get(mess.getFrontend())
					.close();
			frontend2Indexer.remove(mess.getFrontend());
			getSender().tell(new IndexClosed(mess.getFrontend()), getSelf());
		} else
			log.info("This frontend {} has not yet been registered for indexing", mess.getFrontend()
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
		super.preStart();
		cluster.subscribe(self(), MemberEvent.class, ReachabilityEvent.class);
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
		cluster.unsubscribe(self());
		super.postStop();
	}

}
