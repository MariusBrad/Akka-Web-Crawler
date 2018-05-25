/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.io.Serializable;
import java.util.Set;

/**
 * @author marius
 *
 */
public class Page implements Serializable {

	private static final long serialVersionUID = 1L;
	protected String URL;
    protected String title;
    protected String html;
    protected String bodyText;
    protected Set<String> URLsToFollow;
    
    public Page(String URL, String title, String html, String bodyText, Set<String> urlsToFollow) {
    	this.URL = URL;
    	this.title = title;
    	this.html = html;
    	this.bodyText = bodyText;
    	this.URLsToFollow = urlsToFollow;
    }
    
    public String getURL() {
		return URL;
	}

	public String getTitle() {
		return title;
	}

	public String getHtml() {
		return html;
	}

	public String getBodyText() {
		return bodyText;
	}

	public Set<String> getURLsToFollow() {
		return URLsToFollow;
	}
}
