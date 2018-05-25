/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author marius
 *
 */
public class NewsArticlePage extends Page {

	private static final long serialVersionUID = 1L;
	private String articleTitle;
	private String author;
	private LocalDateTime publishedOn;
	private String articleContent;

	public NewsArticlePage(String URL, String title, String html, String bodyText, Set<String> linksToFollow) {
		super(URL, title, html, bodyText, linksToFollow);
		this.articleTitle = null;
		this.author = null;
		this.publishedOn = null;
		this.articleContent = null;
	}

	public NewsArticlePage(Page page) {
		super(page.getURL(), page.getTitle(), page.getHtml(), page.getBodyText(), page.getURLsToFollow());
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public NewsArticlePage setArticleTitle(String articleTitle) {
		this.articleTitle = articleTitle;
		return this;
	}

	public String getAuthor() {
		return author;
	}

	public NewsArticlePage setAuthor(String author) {
		this.author = author;
		return this;
	}

	public LocalDateTime getPublishedOn() {
		return publishedOn;
	}

	public NewsArticlePage setPublishedOn(LocalDateTime publishedOn) {
		this.publishedOn = publishedOn;
		return this;
	}

	public String getArticleContent() {
		return articleContent;
	}

	public NewsArticlePage setArticleContent(String articleContent) {
		this.articleContent = articleContent;
		return this;
	}

}
