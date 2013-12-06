/**
 * Hubei Daily crawler
 */
package crawlers

import java.util.regex.Pattern
import edu.uci.ics.crawler4j.crawler.{ WebCrawler, Page }
import edu.uci.ics.crawler4j.parser.HtmlParseData
import edu.uci.ics.crawler4j.url.WebURL

/**
 * @author ShiZhan
 * Hubei Daily crawler
 */
class HubeiDaily extends WebCrawler {
  private val FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g|png|tiff?|mid"
    + "|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz))$")

  override def shouldVisit(url: WebURL): Boolean = {
    val href = url.getURL.toLowerCase
    !FILTERS.matcher(href).matches() && href.startsWith("http://www.ics.uci.edu/")
  }

  override def visit(page: Page): Unit = {
    val url = page.getWebURL.getURL
    println("URL: " + url)

    val parseData = page.getParseData
    if (parseData.isInstanceOf[HtmlParseData]) {
      val htmlParseData = page.getParseData.asInstanceOf[HtmlParseData]
      val text = htmlParseData.getText
      val html = htmlParseData.getHtml
      val links = htmlParseData.getOutgoingUrls

      println("Text length: " + text.length)
      println("Html length: " + html.length)
      println("Number of outgoing links: " + links.size)
    }
  }
}

object HubeiDaily extends CrawlerObject(
  List(
    "http://www.ics.uci.edu/~welling/",
    "http://www.ics.uci.edu/~lopes/"),
  classOf[HubeiDaily])