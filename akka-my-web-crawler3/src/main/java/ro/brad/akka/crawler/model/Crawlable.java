/**
 * 
 */
package ro.brad.akka.crawler.model;

/**
 * @author marius
 *
 */
public interface Crawlable {
	
	Page fetchPage(String domain, String currentURL);
}
