# Akka-Web-Crawler
Example of an Akka Cluster based web crawler written in Java:

The WEB Crawler provides functionalities for indexing and searching news web pages. Each news site requires an individual implementation of the Crawlable interface. The Project consists of 4 packages:
- .actor: Actors used for single JVM process 
- .cluster: Actors used for multiple JVM processes (cluster nodes)
- .examples: Java console applications for actor systems
- .model: Business model classes: parsers, indexers, searchers, pages...

1. Simple actor based web crawler:

The single JVM web crawler process consists of 1 Master Actor that routes indexing and searching requests to several Crawler Coordinators. Each Crawler Coordinator Actor is responsible for one domain crawl. A coordinator supervises several Crawler Actors and a Scheduler. The Crawlers are used for scraping news web pages: article title, author, date of publishing and article content. For scraping JSoup library is being used. The scraped pages are sent to an Indexer Actor. After indexing the Master sends a search request to the Searcher Actor. Both indexing and searching are based on Apache Lucene features.

2. The Akka Cluster web crawler:

The clusterization of the single JVM process leads to a role related separation between different node members (JVM processes). A cluster member is nothing but an ordinary Actor System. There are 3 roles defined: The Frontend, The Backend Crawler and The Backend Storage. The Frontend handles the indexing and searching requests. Using an Adaptive Load Balancing router each Frontend routes the requests to a Backend Crawler and from here to a single Backend Storage node. Frequent system health metrics (cpu, load, heap) are collected from the Backend Crawlers to efficiently route Frontend's requests. The Backend Crawler supervises several Cluster Crawler Coordinators which in turn supervise simple Cluster Crawlers. The Coordinator also uses a Scheduler Actor to keep track of all crawled links. The Backend Storage holds both an Indexer and a Searcher actor. The relationship between Frontend and Backend Storage is 1:1. 

3. The application model:

Crawling is the first phase. Each Crawler Actor returns a NewsArticlePage after scraping online articles. A NewsArticlePage contains article title, author, published on date, content and other URLs to follow for crawling. Each news site scraping logic is different from one another. Currently there are 3 logics for Tolo.ro, Digi24.ro and Evz.ro. JSoup has been used for scraping. Next comes the indexing part. Each NewsArticlePage is sent to an Indexer Actor which uses a NewsArticleIndexer. The indexing is done by creating and maintaining an Apache Lucene IndexWriter. Several fields are recorded for each indexed Document. The most important one is the Article Content field. When indexing is finished the IndexWriter must be closed in order to do the searching part. The Searcher Actor uses a NewsArticleSearcher to create and maintain a Lucene IndexReader with IndexSearcher, Romanian Analyzers and QueryParsers. The goal is here to execute a text search query in the index content field and to provide a set of article results ordered by score relevance. The results are saved in a text file in the local temporary directory in the host's file system.

4. Some examples:

RunApp1.java and RunApp2.java are Java Console Application demos for the single Actor System web crawler (single JVM process). RunCluster1.java is a demo for running 4 nodes of a cluster in a single JVM (2 Backend Crawlers, 1 Backend Storage and 1 Frontend). For a better understanding of the concept it is recommended to run each node in its own process. ConsoleApp.java is a console with menus application to run the web crawler from the terminal.
