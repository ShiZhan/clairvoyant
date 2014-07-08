/**
 * Spider implementation
 */
package kernel

/**
 * @author ShiZhan
 * Spider implementation
 */
object Spider extends helper.Logging {
  import java.io.{ File, PrintWriter }
  import collection.JavaConversions._
  import actors.Actor
  import actors.Actor._
  import org.apache.commons.codec.digest.DigestUtils
  import org.apache.commons.validator.routines.UrlValidator
  import org.jsoup.Jsoup
  import org.jsoup.nodes.Document

  implicit class FilterOps(filters: List[(util.matching.Regex, String)]) {
    def check(url: String) = filters
      .filterNot { case (r, s) => r.findAllIn(url).isEmpty }
      .map { case (r, s) => s }
  }

  private val httpSchemes = Array("http", "https")
  private val httpValidator = new UrlValidator(httpSchemes)

  case class Page(url: String, document: Document) {
    def getLink =
      Link(document.select("a").map(_.attr("abs:href"))
        .filter(httpValidator.isValid).toList)

    def getLinkWith(filters: List[(util.matching.Regex, String)]) =
      Link(filters.check(url).flatMap { selector =>
        document.select(selector).map(_.attr("abs:href"))
      }.filter(httpValidator.isValid))

    def storeTo(folder: String) = {
      val file = new File(folder + "/" + DigestUtils.md5Hex(url) + ".html")
      val writer = new PrintWriter(file)
      writer.println("<!-- " + url + " -->")
      writer.println(document.html)
      writer.close
    }
  }

  case class Link(links: List[String]) { def isEmpty = links.isEmpty }

  case class HALT

  case class STOP

  class Instance(startURLs: List[String], concurrency: Int, delay: Int, timeout: Int,
    filters: List[(util.matching.Regex, String)], folder: String) {
    private var traveled = collection.mutable.HashSet[String]()
    private def crawled(url: String) = traveled.contains(url)

    val loaders = Array.tabulate(concurrency)(i =>
      actor {
        loop {
          react {
            case url: String => {
              logger.info("Loader [{}]: {}", i, url)
              try {
                val doc = Jsoup.parse(new java.net.URL(url), timeout)
                val page = Page(url, doc)
                val links = page.getLinkWith(filters)
                page.storeTo(folder)
                if (links.isEmpty) sender ! HALT else sender ! links
                traveled += url
              } catch {
                case e: Exception => println(e)
              }
            }
            case STOP => exit
          }
        }
      })

    val controller = actor {
      loop {
        react {
          case Link(urls) =>
            urls.grouped(concurrency).foreach {
              _.zipWithIndex.foreach {
                case (url, index) =>
                  if (!crawled(url)) {
                    loaders(index) ! url
                    Thread.sleep(delay)
                  }
              }
            }
          case HALT if (loaders.forall(_.getState == State.Suspended)) => {
            loaders.foreach(_ ! STOP)
            val filterTotal = filters.length
            val traveledURLs = traveled.size
            val summary = s"""------
Start URL: $startURLs
concurrency: $concurrency, delay: $delay ms, timeout: $timeout ms,
Filter total: $filterTotal
Folder: $folder
Traveled URLs: $traveledURLs"""
            println(summary)
            exit
          }
        }
      }
    }

    def run = controller ! Link(startURLs)
  }
}