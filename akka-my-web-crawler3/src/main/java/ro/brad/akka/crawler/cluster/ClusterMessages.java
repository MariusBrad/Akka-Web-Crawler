/**
 * 
 */
package ro.brad.akka.crawler.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import akka.actor.Address;
import ro.brad.akka.crawler.model.Page;
import ro.brad.akka.crawler.model.Results;
import ro.brad.akka.crawler.model.Globals.IndexOpenMode;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;

/**
 * @author marius
 *
 */
public interface ClusterMessages {

	public static final class StartURLScheduled {
		private final String URL;

		public StartURLScheduled(String URL) {
			this.URL = URL;
		}

		public String getURL() {
			return URL;
		}
	}

	public static final class URLCrawled {
		private final Page page;

		public URLCrawled(Page page) {
			this.page = page;
		}

		public Page getPage() {
			return page;
		}
	}

	public static final class BatchURLsReceived {
		private final Collection<String> batch;

		public BatchURLsReceived(Collection<String> batch) {
			this.batch = batch;
		}

		public Collection<String> getBatch() {
			return batch;
		}
	}

	public static final Object SCRAPED_URLs_SCHEDULED = new Object();
	public static final Object END_OF_SCHEDULE = new Object();

	public static final class WhoCrawlsDomain implements Serializable {

		private static final long serialVersionUID = 1L;
		private final String domain;
		private final Address frontend;
		private final Address storage;
		private final String reason;

		public WhoCrawlsDomain(Address frontend, Address storage, String domain, String reason) {
			this.frontend = frontend;
			this.storage = storage;
			this.domain = domain;
			this.reason = reason;
		}

		public String getDomain() {
			return domain;
		}

		public String getReason() {
			return reason;
		}

		public Address getFrontend() {
			return frontend;
		}

		public Address getStorage() {
			return storage;
		}
	}

	public static final class KillMe {
		private final String domain;
		private final Address frontend;
		private final Address storage;

		public KillMe(String domain, Address frontend, Address storage) {
			this.domain = domain;
			this.frontend = frontend;
			this.storage = storage;
		}

		public String getDomain() {
			return domain;
		}

		public Address getFrontend() {
			return frontend;
		}

		public Address getStorage() {
			return storage;
		}
	}

	public static final class CrawlNewsSite {
		private final NewsAgencies agency;
		private final String domain;
		private final List<String> sites;

		public CrawlNewsSite(NewsAgencies agency, String domain, List<String> sites) {
			this.agency = agency;
			this.domain = domain;
			this.sites = new ArrayList<String>(sites);
		}

		public NewsAgencies getAgency() {
			return agency;
		}

		public List<String> getSites() {
			return sites;
		}

		public String getDomain() {
			return domain;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Domain: " + domain)
					.append(" and categories:")
					.append(System.lineSeparator());
			for (int i = 0; i < sites.size() - 1; i++) {
				builder.append(" - ")
						.append(sites.get(i))
						.append(System.lineSeparator());
			}
			builder.append(" - ")
					.append(sites.get(sites.size() - 1));
			return builder.toString();
		}
	}

	public static final class KillCrawl {
		private final String domain;

		public KillCrawl(String domain) {
			this.domain = domain;
		}

		public String getDomain() {
			return domain;
		}
	}

	public static final class CrawlingOver implements Serializable {

		private static final long serialVersionUID = 1L;
		private final String domain;
		private final Address frontend;
		private final Address storage;

		public CrawlingOver(String domain, Address frontend, Address storage) {
			this.domain = domain;
			this.frontend = frontend;
			this.storage = storage;
		}

		public String getDomain() {
			return domain;
		}

		public Address getFrontend() {
			return frontend;
		}

		public Address getStorage() {
			return storage;
		}

	}

	public static final Object IS_CRAWLING_SESSION_FINISHED = new Object();

	public static final class ScheduleStartURL {
		private final String startURL;

		public ScheduleStartURL(String startURL) {
			this.startURL = startURL;
		}

		public String getStartURL() {
			return startURL;
		}
	}

	public static final class ScheduleScrapedURLs {
		private final Collection<String> scrapedURLs;

		public ScheduleScrapedURLs(Collection<String> scrapedURLs) {
			this.scrapedURLs = scrapedURLs;
		}

		public Collection<String> getScrapedURLs() {
			return scrapedURLs;
		}
	}

	public static final Object REQUEST_BATCH_URLs = new Object();

	public static final class CrawlNewsURL {
		private final NewsAgencies agency;
		private final String domain;
		private final String link;

		public CrawlNewsURL(NewsAgencies agency, String domain, String link) {
			this.agency = agency;
			this.domain = domain;
			this.link = link;
		}

		public NewsAgencies getAgency() {
			return agency;
		}

		public String getDomain() {
			return domain;
		}

		public String getLink() {
			return link;
		}
	}

	public static final class CrawlJob implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String domain;
		private final NewsAgencies agency;
		private final List<String> sites;
		private final Address frontend;
		private final Address storage;

		public CrawlJob(NewsAgencies agency, String domain, List<String> sites, Address frontend, Address storage) {
			this.agency = agency;
			this.domain = domain;
			this.sites = sites;
			this.frontend = frontend;
			this.storage = storage;
		}

		public NewsAgencies getAgency() {
			return agency;
		}

		public List<String> getSites() {
			return sites;
		}

		public String getDomain() {
			return domain;
		}

		public Address getFrontend() {
			return frontend;
		}

		public Address getStorage() {
			return storage;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Domain: " + domain)
					.append(" and categories:")
					.append(System.lineSeparator());
			for (int i = 0; i < sites.size() - 1; i++) {
				builder.append(" - ")
						.append(sites.get(i))
						.append(System.lineSeparator());
			}
			builder.append(" - ")
					.append(sites.get(sites.size() - 1));
			return builder.toString();
		}
	}

	public static final class AddIndexer implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;
		private final IndexOpenMode mode;

		public AddIndexer(Address frontend, IndexOpenMode mode) {
			this.frontend = frontend;
			this.mode = mode;
		}

		public Address getFrontend() {
			return frontend;
		}

		public IndexOpenMode getMode() {
			return mode;
		}
	}

	public static final class IndexPage implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;
		private final Address storage;
		private final Page page;

		public IndexPage(Address frontend, Address storage, Page page) {
			this.frontend = frontend;
			this.storage = storage;
			this.page = page;
		}

		public Page getPage() {
			return page;
		}

		public Address getFrontend() {
			return frontend;
		}

		public Address getStorage() {
			return storage;
		}
	}

	public static final class IndexCommit implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public IndexCommit(Address frontend) {
			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class IndexClose implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public IndexClose(Address frontend) {
			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class PageIndexed implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;
		private final Page page;

		public PageIndexed(Address frontend, Page page) {
			this.frontend = frontend;
			this.page = page;
		}

		public Page getPage() {
			return page;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class IndexCommitted implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public IndexCommitted(Address frontend) {

			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class IndexClosed implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public IndexClosed(Address frontend) {

			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class AddSearcher implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;
		private final String searchField;

		public AddSearcher(Address frontend, String searchField) {
			this.frontend = frontend;
			this.searchField = searchField;
		}

		public Address getFrontend() {
			return frontend;
		}

		public String getSearchField() {
			return searchField;
		}
	}

	public static final class SearchText implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;
		private final String searchString;
		private final int hitsCount;

		public SearchText(Address frontend, String searchString, int hitsCount) {
			this.frontend = frontend;
			this.searchString = searchString;
			this.hitsCount = hitsCount;
		}

		public String getSearchString() {
			return searchString;
		}

		public int getHitsCount() {
			return hitsCount;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class SearchResults implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Results results;

		public SearchResults(Results results) {
			this.results = results;
		}

		public Results getResults() {
			return results;
		}
	}

	public static final class SearchClose implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public SearchClose(Address frontend) {
			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}

	public static final class SearchClosed implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Address frontend;

		public SearchClosed(Address frontend) {
			this.frontend = frontend;
		}

		public Address getFrontend() {
			return frontend;
		}
	}
}
