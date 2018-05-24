# Akka-Web-Crawler
Example of an Akka Cluster based web crawler written in Java:

The WEB Crawler provides functionalities for indexing and searching news web pages. Each news site requires an individual implementation of the Crawlable interface. The Project consists of 4 packages:
- .actor: Actors used for single JVM process 
- .cluster: Actors used for multiple JVM processes (cluster nodes)
- .examples: Java console applications for actor systems
- .model: Business model classes: indexers, searchers, pages...

1. Actor based web crawler:
The single JVM web crawler process consists of 1 Master Actor that routes indexing and searching requests to several Crawler Coordinators. Each Crawler Coordinator Actor is responsible for one domain crawl. A coordinator supervises several Crawler Actors and a Scheduler. The Crawlers are used for scraping news web pages: article title, author, date of publishing and article content. For scraping JSoup library is being used. The scraped pages are sent to an Indexer Actor. After indexing the Master sends a search request to the Searcher Actor. Both indexing and searching are based on Apache Lucene features.
