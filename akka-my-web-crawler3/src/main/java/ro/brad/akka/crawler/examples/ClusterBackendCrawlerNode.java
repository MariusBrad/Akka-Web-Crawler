/**
 * 
 */
package ro.brad.akka.crawler.examples;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;
import ro.brad.akka.crawler.cluster.BackendCrawlerActor;
import ro.brad.akka.crawler.cluster.MetricsListenerActor;
import ro.brad.akka.crawler.model.Globals;

/**
 * @author marius
 *
 */
public class ClusterBackendCrawlerNode {

	public static void main(String[] args) {
		// Override the configuration of the port when specified as program argument
		final String port = args.length > 0 ? args[0] : "0";
		final Config config = ConfigFactory
				.parseString("akka.remote.netty.tcp.port=" + port + "\n" + "akka.remote.artery.canonical.port=" + port)
				.withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend-crawler]"))
				.withFallback(ConfigFactory.load("cluster"));

		ActorSystem system = ActorSystem.create(Globals.CLUSTER_SYSTEM, config);

		system.actorOf(Props.create(BackendCrawlerActor.class), Globals.BACKEND_CRAWLER);
		system.actorOf(Props.create(MetricsListenerActor.class), Globals.METRICS_LISTENER);
	}

}
