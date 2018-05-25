/**
 * 
 */
package ro.brad.akka.crawler.model;

/**
 * @author marius
 *
 */
public class Globals {
	// Enumerations
	public enum IndexOpenMode {
		APPEND, CREATE, CREATE_OR_APPEND
	}

	public enum NewsAgencies {
		EVZ, TOLO, DIGI24
	}

	public enum WatchedActors {
		CRAWLER, CRAWLER_COORDINATOR
	}

	public enum CrawlingType {
		SIMPLE, NEWS
	}
	//News Agencies
	public final static String TOLO = "Tolo.ro";
	public final static String EVZ = "Evz.ro";
	public final static String DIGI24 = "Digi24.ro";
	// Actor names
	public final static String ACTOR_SYSTEM = "Hello-Web-Crawlers";
	public final static String MASTER_ACTOR = "Master";
	public final static String CRAWLER_COORDINATOR_ACTOR = "Crawler_Coordinator";
	public final static String CRAWLER_ACTOR = "Crawler";
	public final static String INDEXER_ACTOR = "Indexer";
	public final static String SCHEDULER_ACTOR = "Scheduler";
	public final static String SEARCHER_ACTOR = "Searcher";
	// Cluster names
	public final static String CLUSTER_SYSTEM = "ClusterCrawler";
	public final static String FRONTEND = "Frontend";
	public final static String BACKEND_CRAWLER = "Backend_Crawler";
	public final static String BACKEND_INDEXER = "Backend_Indexer";
	public final static String BACKEND_SEARCHER = "Backend_Searcher";
	public final static String METRICS_LISTENER = "Metrics_Listener";
	public final static String BACKEND_SEARCHER_PATH = "/user/Backend_Searcher";
	public final static String BACKEND_INDEXER_PATH = "/user/Backend_Indexer";
	public final static String FRONTEND_PATH = "/user/Frontend";
	// Indexing
	public final static String INDEX_DIR = "web-crawler-index";
	public final static String INDEX_SEARCH_FIELD = "ArticleContent";
	public final static String URL_FIELD = "URL";
	public final static String TITLE_FIELD = "Title";
	public final static String HTML_CONTENT_FIELD = "HTMLContent";
	public final static String TEXT_CONTENT_FIELD = "TextContent";
	public final static String AUTHOR_FIELD = "Author";
	public final static String PUBLISHED_ON_FIELD = "PublishedOn";
	public final static String ARTICLE_TITLE_FIELD = "ArticleTitle";
	public final static String ARTICLE_CONTENT_FIELD = "ArticleContent";
	// Searching
	public final static String RESULTS_FILENAME = "log_results.txt";
	// Killing Actors
	public final static String KILL_REASON = "kill";
	// News article parsing
	public final static String ARTICLE_TYPE = "article";
	// Hits to be returned when searching for results
	public final int HITS_COUNT = 10;
	// Command line terms
	public final static String SIMPLE_EXEC = "simple";
	public final static String NEWS_EXEC = "news";
	public final static String DOMAIN_OPT = "-domain";
	public final static String SITES_OPT = "-sites";
	public final static String UPDATE_OPT = "-update";
	public final static String HITS_OPT = "-hits";
	public final static String SIMPLE_USAGE = "java ro.brad.akka.crawler.actor.App [simple] [-domain SITE_URL] [-update]"
			+ System.lineSeparator() + System.lineSeparator()
			+ "This variant runs a simple web domain crawler starting with SITE_URL and creates or updates a Lucene index accordingly.";
	public final static String NEWS_USAGE = "java ro.brad.akka.crawler.actor.App [news] [-hits NR] [-update]"
			+ System.lineSeparator() + System.lineSeparator()
			+ "This variant runs a news article web crawler. It crawls articles from various news agency sites and stores the"
			+ System.lineSeparator()
			+ "content into an index (created or updated). At the end of the indexing phase you can search for text and obtain"
			+ System.lineSeparator()
			+ "relevant score for each hit. You need to provide a number of hits to be returned.";
	//Console application
	public final static String PROMPT_SIGN = "=>";

}
