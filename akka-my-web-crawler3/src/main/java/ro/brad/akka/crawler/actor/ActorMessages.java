/**
 * 
 */
package ro.brad.akka.crawler.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ro.brad.akka.crawler.model.Page;
import ro.brad.akka.crawler.model.Results;
import ro.brad.akka.crawler.model.Globals.NewsAgencies;

/**
 * @author marius
 *
 */
public interface ActorMessages {

	// Define CrawlerCoordinatorActor Messages to handle
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

	public static final class WhoCrawlsDomain {
		private final String domain;
		private final String reason;

		public WhoCrawlsDomain(String domain, String reason) {
			this.domain = domain;
			this.reason = reason;
		}

		public String getDomain() {
			return domain;
		}

		public String getReason() {
			return reason;
		}
	}

	public static final class KillMe {
		private final String domain;

		public KillMe(String domain) {
			this.domain = domain;
		}

		public String getDomain() {
			return domain;
		}
	}

	
	// Define MasterActor Messages to handle
	public static final class CrawlSite {
		private final String siteURL;

		public CrawlSite(String siteURL) {
			this.siteURL = siteURL;
		}

		public String getSiteURL() {
			return siteURL;
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

	public static final class SearchResults {
		private final Results results;

		public SearchResults(Results results) {
			this.results = results;
		}

		public Results getResults() {
			return results;
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

	public static final class CrawlingOver {
		private final String domain;

		public CrawlingOver(String domain) {
			this.domain = domain;
		}

		public String getDomain() {
			return domain;
		}
	}

	public static final Object IS_CRAWLING_SESSION_FINISHED = new Object();

	
	//Define IndexerActor messages to handle
	public static final class IndexPage {
		private final Page page;

		public IndexPage(Page page) {
			this.page = page;
		}

		public Page getPage() {
			return page;
		}
	}
	
	public static final Object INDEX_CLOSE = new Object();
	public static final Object INDEX_CLOSED = new Object();
	public static final Object INDEX_COMMIT = new Object();
	public static final Object INDEX_COMMITTED = new Object();


	// Define SchedulerActor Messages to handle
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

	public static final class PageIndexed {
		private final Page page;

		public PageIndexed(Page page) {
			this.page = page;
		}

		public Page getPage() {
			return page;
		}
	}
	
	public static final Object REQUEST_BATCH_URLs = new Object();

	
	// Define SearcherActor messages to handle
	public static final class SearchText {
		private final String searchString;
		private final int hitsCount;

		public SearchText(String searchString, int hitsCount) {
			this.searchString = searchString;
			this.hitsCount = hitsCount;
		}

		public String getSearchString() {
			return searchString;
		}

		public int getHitsCount() {
			return hitsCount;
		}
	}

	public static final Object SEARCH_CLOSE = new Object();
	public static final Object SEARCH_CLOSED = new Object();


	// Define CrawlerActor Messages to handle
	public static final class CrawlURL {
		private final String domain;
		private final String link;

		public CrawlURL(String domain, String link) {
			this.domain = domain;
			this.link = link;
		}

		public String getLink() {
			return link;
		}

		public String getDomain() {
			return domain;
		}
	}

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
}
