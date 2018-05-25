/**
 * 
 */
package ro.brad.akka.crawler.model;


/**
 * @author marius
 *
 */
public interface Searchable {

	public Results searchResults(String searchString, int hitsCount);
	
	public void close();

}
