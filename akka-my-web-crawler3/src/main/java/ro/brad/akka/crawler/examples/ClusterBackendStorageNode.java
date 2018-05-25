/**
 * 
 */
package ro.brad.akka.crawler.examples;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;
import ro.brad.akka.crawler.cluster.BackendIndexerActor;
import ro.brad.akka.crawler.cluster.BackendSearcherActor;
import ro.brad.akka.crawler.cluster.MetricsListenerActor;
import ro.brad.akka.crawler.model.Globals;

/**
 * @author marius
 *
 */
public class ClusterBackendStorageNode {

	public static void main(String[] args) {
		// Override the configuration of the port when specified as program argument
		final String port = args.length > 0 ? args[0] : "0";
		final Config config = ConfigFactory
				.parseString("akka.remote.netty.tcp.port=" + port + "\n" + "akka.remote.artery.canonical.port=" + port)
				.withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend-storage]"))
				.withFallback(ConfigFactory.load("cluster"));

		ActorSystem system = ActorSystem.create(Globals.CLUSTER_SYSTEM, config);

		system.actorOf(Props.create(BackendIndexerActor.class), Globals.BACKEND_INDEXER);
		system.actorOf(Props.create(BackendSearcherActor.class), Globals.BACKEND_SEARCHER);
		system.actorOf(Props.create(MetricsListenerActor.class), Globals.METRICS_LISTENER);
	}

}
