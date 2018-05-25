/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import ro.brad.akka.crawler.model.Globals.IndexOpenMode;

/**
 * @author marius
 *
 */
public class NewsArticleIndexer extends PageIndexer {

	public NewsArticleIndexer(String indexPath, IndexOpenMode mode) {
		super(indexPath, mode);
	}

	@Override
	public void indexDoc(Page page) {
		if (page instanceof NewsArticlePage) {
			NewsArticlePage newsPage = (NewsArticlePage)page;
			try {
				if (indexWriter.getConfig()
						.getOpenMode().equals(OpenMode.CREATE)) {
					// Create new index
					indexWriter.addDocument(toDocument(newsPage));
				} else {
					// Update existing index
					indexWriter.updateDocument(new Term("URL", newsPage.getURL()), toDocument(newsPage));
				}
			} catch (CorruptIndexException ex) {
				throw new IllegalStateException(ex);
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}else
			super.indexDoc(page);
	}

	@Override
	public Document toDocument(Page page) {
		if (page instanceof NewsArticlePage) {
			Document doc = new Document();
			NewsArticlePage newsPage = (NewsArticlePage) page;
			doc.add(new StringField(Globals.URL_FIELD, newsPage.getURL(), Field.Store.YES));
			doc.add(new StringField(Globals.TITLE_FIELD, newsPage.getTitle(), Field.Store.YES));
			doc.add(new StringField(Globals.AUTHOR_FIELD, newsPage.getAuthor(), Field.Store.YES));
			doc.add(new StringField(Globals.PUBLISHED_ON_FIELD, LocalDate.of(newsPage.getPublishedOn()
					.getYear(),
					newsPage.getPublishedOn()
							.getMonth(),
					newsPage.getPublishedOn()
							.getDayOfMonth())
					.toString(), Field.Store.YES));
			doc.add(new TextField(Globals.ARTICLE_TITLE_FIELD, newsPage.getArticleTitle(), Field.Store.YES));
			doc.add(new TextField(Globals.ARTICLE_CONTENT_FIELD, new StringReader(newsPage.getArticleContent())));
			return doc;
		} else
			return super.toDocument(page);
	}

	@Override
	public void commit() {
		super.commit();
	}

	@Override
	public void close() {
		super.close();
	}

}
