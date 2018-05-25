/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.metrics.ClusterMetricsChanged;
import akka.cluster.metrics.ClusterMetricsExtension;
import akka.cluster.metrics.NodeMetrics;
import akka.cluster.metrics.StandardMetrics;
import akka.cluster.metrics.StandardMetrics.Cpu;
import akka.cluster.metrics.StandardMetrics.HeapMemory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author marius
 *
 */
public class MetricsListenerActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private final ClusterMetricsExtension extension = ClusterMetricsExtension.get(getContext().system());

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(ClusterMetricsChanged.class, this::onClusterMetricsChanged)
				.match(CurrentClusterState.class, this::onCurrentClusterState)
				.build();
	}

	private void onClusterMetricsChanged(ClusterMetricsChanged mess) {
		for (NodeMetrics nodeMetrics : mess.getNodeMetrics()) {
			// Log System Health Metrics only for the current node in the cluster
			if (nodeMetrics.address()
					.equals(cluster.selfAddress())) {
				logHeap(nodeMetrics);
				logCpu(nodeMetrics);
			}
		}
	}
	
	private void onCurrentClusterState(CurrentClusterState mess) {

	}

	private void logHeap(NodeMetrics nodeMetrics) {
		HeapMemory heap = StandardMetrics.extractHeapMemory(nodeMetrics);
		if (heap != null) {
			log.info("Used heap: {} MB", ((double) heap.used()) / 1024 / 1024);
		}
	}

	private void logCpu(NodeMetrics nodeMetrics) {
		Cpu cpu = StandardMetrics.extractCpu(nodeMetrics);
		if (cpu != null && cpu.systemLoadAverage()
				.isDefined()) {
			log.info("Load: {} ({} processors)", cpu.systemLoadAverage()
					.get(), cpu.processors());
		}
	}

	// Subscribe unto ClusterMetricsEvent events.
	@Override
	public void preStart() throws Exception {
		extension.subscribe(self());
	}

	@Override
	public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
		super.preRestart(reason, message);
	}

	@Override
	public void postRestart(Throwable reason) throws Exception {
		super.postRestart(reason);
	}

	// Unsubscribe from ClusterMetricsEvent events.
	@Override
	public void postStop() throws Exception {
		extension.unsubscribe(self());
	}

}
