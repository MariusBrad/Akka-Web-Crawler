/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.io.Serializable;

/**
 * @author marius
 *
 */
public class Result implements Serializable{

	private static final long serialVersionUID = 1L;
	private final String url;
	private final String articleTitle;
	private final String author;
	private final String publishedOn;
	private float score;

	public Result(String url, String articleTitle, String author, String publishedOn) {
		this.url = url;
		this.articleTitle = articleTitle;
		this.author = author;
		this.publishedOn = publishedOn;
	}

	public Result(String url, String articleTitle, String author, String publishedOn, float score) {
		super();
		this.url = url;
		this.articleTitle = articleTitle;
		this.author = author;
		this.publishedOn = publishedOn;
		this.score = score;
	}

	public String getUrl() {
		return url;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public String getAuthor() {
		return author;
	}

	public String getPublishedOn() {
		return publishedOn;
	}

	public float getScore() {
		return score;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return new StringBuilder().append(articleTitle)
				.append(" - ")
				.append("By ")
				.append(author)
				.append(", published on ")
				.append(publishedOn)
				.append(" at ")
				.append(url)
				.append(" with score ")
				.append(String.valueOf(score))
				.toString();
	}

}
