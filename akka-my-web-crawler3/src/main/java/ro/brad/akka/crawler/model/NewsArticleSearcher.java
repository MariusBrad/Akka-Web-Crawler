package ro.brad.akka.crawler.model;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class NewsArticleSearcher implements Searchable {

	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private Directory dir;
	private Analyzer analyzer;
	private QueryParser parser;
	protected Date startDate;
	protected Date endDate;

	public NewsArticleSearcher(String indexPath, String searchField) {
		startDate = new Date();
		try {
			this.dir = FSDirectory.open(Paths.get(indexPath));
			this.indexReader = DirectoryReader.open(dir);
			this.indexSearcher = new IndexSearcher(indexReader);
			this.analyzer = new RomanianAnalyzer();
			this.parser = new QueryParser(searchField, analyzer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Results searchResults(String searchString, int hitsCount) {
		// TODO Auto-generated method stub
		Query query;
		TopDocs topDocs;
		ScoreDoc[] scores;
		ArrayList<Result> results = new ArrayList<Result>();

		try {
			query = parser.parse(searchString);
			topDocs = indexSearcher.search(query, hitsCount);
			scores = topDocs.scoreDocs;
			for (ScoreDoc score : scores) {
				String url = indexSearcher.doc(score.doc)
						.get(Globals.URL_FIELD);
				String articleTitle = indexSearcher.doc(score.doc)
						.get(Globals.ARTICLE_TITLE_FIELD);
				String author = indexSearcher.doc(score.doc)
						.get(Globals.AUTHOR_FIELD);
				String publishedOn = indexSearcher.doc(score.doc)
						.get(Globals.PUBLISHED_ON_FIELD);

				results.add(new Result(url, articleTitle, author, publishedOn, score.score));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Results(results, searchString);
	}

	@Override
	public void close() {
		try {
			indexReader.close();
			endDate = new Date();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
