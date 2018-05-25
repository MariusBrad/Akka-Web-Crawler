/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import ro.brad.akka.crawler.model.Globals.IndexOpenMode;

/**
 * @author marius
 *
 */
public class PageIndexer implements Indexable {

	protected IndexWriter indexWriter;
	protected Directory dir;
	protected Analyzer analyzer;
	protected IndexWriterConfig iwc;
	protected Date startDate;
	protected Date endDate;

	public PageIndexer(String indexPath, IndexOpenMode mode) {
		startDate = new Date();
		try {
			this.dir = FSDirectory.open(Paths.get(indexPath));
			this.analyzer = new RomanianAnalyzer();
			this.iwc = new IndexWriterConfig(analyzer);
			switch (mode) {
			case APPEND:
				iwc.setOpenMode(OpenMode.APPEND);
				break;
			case CREATE:
				iwc.setOpenMode(OpenMode.CREATE);
				break;
			case CREATE_OR_APPEND:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				break;
			default:
				iwc.setOpenMode(OpenMode.CREATE);
				break;
			}
			this.indexWriter = new IndexWriter(dir, iwc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.brad.akka.crawler.basic.Indexable#commit()
	 */
	@Override
	public void commit() {
		// TODO Auto-generated method stub
		try {
			indexWriter.commit();
		} catch (CorruptIndexException ex) {
			throw new IllegalStateException(ex);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ro.brad.akka.crawler.basic.Indexable#index(ro.brad.akka.crawler.basic.Page)
	 */
	@Override
	public void indexDoc(Page page) {
		try {
			if (indexWriter.getConfig()
					.getOpenMode().equals(OpenMode.CREATE)) {
				// Create new index
				indexWriter.addDocument(toDocument(page));
			} else {
				// Update existing index
				indexWriter.updateDocument(new Term("URL", page.getURL()), toDocument(page));
			}
		} catch (CorruptIndexException ex) {
			throw new IllegalStateException(ex);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public Document toDocument(Page page) {
		Document doc = new Document();
		doc.add(new StringField(Globals.URL_FIELD, page.getURL(), Field.Store.YES));
		doc.add(new StringField(Globals.TITLE_FIELD, page.getTitle(), Field.Store.YES));
		doc.add(new TextField(Globals.HTML_CONTENT_FIELD, page.getHtml(), Field.Store.NO));
		doc.add(new TextField(Globals.TEXT_CONTENT_FIELD, page.getBodyText(), Field.Store.NO));
		return doc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.brad.akka.crawler.basic.Indexable#close()
	 */
	@Override
	public void close() {
		// TODO Auto-generated method stub
		try {
			indexWriter.close();
			endDate = new Date();
		} catch (CorruptIndexException ex) {
			throw new IllegalStateException(ex);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}

	}

}
