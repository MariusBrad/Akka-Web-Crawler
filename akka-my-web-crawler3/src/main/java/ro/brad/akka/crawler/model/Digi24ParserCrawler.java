/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author marius
 *
 */
public class Digi24ParserCrawler implements Crawlable {

	@Override
	public Page fetchPage(String domain, String currentURL) {

		NewsArticlePage newsPage = null;
		Page page = new HTMLParserCrawler().fetchPage(domain, currentURL);
		if (page != null) {
			newsPage = new NewsArticlePage(page);
			Document doc = Jsoup.parse(page.getHtml());
			Element head = doc.head();
			Element body = doc.body();

			if (head.select("meta[name='cXenseParse:pageclass']")
					.attr("content")
					.equals(Globals.ARTICLE_TYPE)) {

				String articleTitle = null;
				String author = null;
				LocalDateTime publishedOn = null;
				String articleContent = null;
				try {
					articleTitle = body
							.select("div#article-content main.site-content article header h1[itemprop='headline']")
							.first()
							.text();

					author = head.select("meta[property='cXenseParse:author']")
							.first()
							.attr("content");

					publishedOn = LocalDateTime.parse(body.select("div#article-content time#itemprop-datePublished")
							.first()
							.attr("datetime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

					articleContent = new StringBuilder()
							.append(buildCorpus(body.select(
									"div#article-content main.site-content article div[itemprop='articleBody'] > p")))
							.toString();

					newsPage.setArticleTitle(articleTitle)
							.setAuthor(author)
							.setPublishedOn(publishedOn)
							.setArticleContent(articleContent);
				} catch (DateTimeParseException e) {
					// System.err.println(
					// "News article's PublishedOn timestamp could not be parsed at URL: " +
					// newsPage.getURL());
				} catch (Exception e) {
				}
				newsPage.setArticleTitle(articleTitle)
						.setAuthor(author)
						.setPublishedOn(publishedOn)
						.setArticleContent(articleContent);
			}
		}

		return newsPage;
	}

	private StringBuilder buildCorpus(Elements select) {
		// TODO Auto-generated method stub
		StringBuilder corpus = new StringBuilder();
		for (Iterator<Element> iterator = select.iterator(); iterator.hasNext();) {
			Element element = iterator.next();
			corpus.append(element.text());
		}
		return corpus;
	}
}
