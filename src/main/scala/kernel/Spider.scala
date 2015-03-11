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
  import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
  import org.apache.commons.codec.digest.DigestUtils
  import org.apache.commons.validator.routines.UrlValidator
  import org.jsoup.Jsoup
  import org.jsoup.nodes.Document

  type Filters = List[(util.matching.Regex, String)]
  implicit class FilterOps(filters: Filters) {
    def check(url: String) = filters
      .filterNot { case (r, s) => r.findAllIn(url).isEmpty }
      .map { case (r, s) => s }
  }

  private val httpValidator = new UrlValidator(Array("http", "https"))

  case class Page(url: String, document: Document) {
    def getLink =
      Link(document.select("a").map(_.attr("abs:href"))
        .filter(httpValidator.isValid).toList)

    def getLinkWith(filters: Filters) =
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
  case class Crawled(url: String)
  case class Crawling(url: String)
  case class STOP()

  class Loader(index: Int, timeout: Int, filters: Filters, folder: String) extends Actor {
    def receive = {
      case Crawling(url) => {
        logger.info("Loader [{}]: {}", index, url)
        try {
          val doc = Jsoup.parse(new java.net.URL(url), timeout)
          val page = Page(url, doc)
          val links = page.getLinkWith(filters)
          page.storeTo(folder)
          if (!links.isEmpty) sender ! links
          sender ! Crawled(url)
        } catch {
          case e: Exception => println(e)
        }
      }
      case STOP => sys.exit
    }
  }

  class Controller(loaders: Array[ActorRef], delay: Int) extends Actor {
    val concurrency = loaders.length
    var crawled = collection.mutable.HashSet[String]()
    var crawling = collection.mutable.HashSet[String]()
    def receive = {
      case Link(urls) => urls.grouped(concurrency).foreach {
        _.zipWithIndex.foreach {
          case (url, index) if (!crawled.contains(url)) => {
            crawling += url
            loaders(index) ! Crawling(url)
            Thread.sleep(delay)
          }
        }
      }
      case Crawled(url) => {
        crawled += url
        crawling -= url
        if (crawling.isEmpty) {
          val traveledURLs = crawled.size
          loaders.foreach(_ ! STOP)
          println(s"\nTraveled URLs: $traveledURLs")
          sys.exit
        }
      }
    }
  }

  class Instance(startURLs: List[String], concurrency: Int, delay: Int, timeout: Int,
      filters: Filters, folder: String) {
    val system = ActorSystem("CoreSystem")

    val loaders = Array.tabulate(concurrency) { index =>
      system.actorOf(Props(new Loader(index, timeout, filters, folder)),
        name = "loader" + "%03d".format(index))
    }

    val controller = system.actorOf(Props(new Controller(loaders, delay)), name = "controller")

    def run = controller ! Link(startURLs)
  }
}