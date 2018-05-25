/**
 * 
 */
package ro.brad.akka.crawler.model;


/**
 * @author marius
 *
 */
public interface Indexable {
	
    public void commit();

    public void indexDoc(Page page);
    
    public void close();
}
