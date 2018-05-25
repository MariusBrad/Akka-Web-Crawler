/**
 * 
 */
package ro.brad.akka.crawler.examples;

/**
 * @author marius
 *
 */
public class RunCluster1 {
	public static void main(String[] args) {
	    // starting 2 backend crawler nodes, 1 backend storage node and 1 frontend node
		// in the same JVM process
	    ClusterBackendCrawlerNode.main(new String[] { "2551" });
	    ClusterBackendStorageNode.main(new String[] { "2552" });
	    ClusterBackendCrawlerNode.main(new String[0]);
	    ClusterFrontendNode.main(new String[0]);
	}
}
