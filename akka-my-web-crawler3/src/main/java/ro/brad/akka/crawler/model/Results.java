/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author marius
 *
 */
public class Results extends ArrayList<Result> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String searchString;

	public Results(Collection<? extends Result> arg0, String searchString) {
		super(arg0);
		// TODO Auto-generated constructor stub
		this.searchString = searchString;
	}

	public String getSearchString() {
		return searchString;
	}
	
}
