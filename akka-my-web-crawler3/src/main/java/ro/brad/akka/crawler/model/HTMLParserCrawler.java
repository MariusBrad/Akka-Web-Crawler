/**
 * 
 */
package ro.brad.akka.crawler.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author marius
 *
 */
public class HTMLParserCrawler implements Crawlable {

	@Override
	public Page fetchPage(String domain, String currentURL) {

		try {
			Document doc = Jsoup.connect(currentURL)
					.get();
			String title = doc.title();
			String html = doc.html();
			Elements bodys = doc.getElementsByTag("body");
			Element body = bodys.first();
			String bodyText = body.text();

			Elements urlElements = doc.select("a[href]");
			Set<String> urlsToFollow = new HashSet<String>();
			for (Element element : urlElements) {
				String url = element.absUrl("href");
				if (url.startsWith(domain) && isCleanURL(url)) {
					urlsToFollow.add(url);
				}
			}
			return new Page(currentURL, title, html, bodyText, urlsToFollow);
		} catch (IOException e) {

		} catch (Exception e) {

		}
		return null;
	}

	private boolean isCleanURL(String url) {
		return !url.contains("#") && // Ignore URIs with fragments
				!url.contains("?"); // Ignore URIs with queries
	}

}
