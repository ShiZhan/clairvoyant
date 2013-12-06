package crawlers

import edu.uci.ics.crawler4j.crawler.{ CrawlConfig, CrawlController, WebCrawler }
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.robotstxt.{ RobotstxtConfig, RobotstxtServer }

case class CrawlerObject(seed: List[String], crawler: Class[_])

class Control(crawlerObject: CrawlerObject, store: String, threads: Int) {
  val config = new CrawlConfig()
  config.setCrawlStorageFolder(store)
  val pageFetcher = new PageFetcher(config)
  val robotstxtConfig = new RobotstxtConfig()
  val robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher)
  val controller = new CrawlController(config, pageFetcher, robotstxtServer)
  crawlerObject.seed foreach controller.addSeed
  val crawlerClass = crawlerObject.crawler.asSubclass(classOf[WebCrawler])
  def start = controller.start(crawlerClass, threads)
  def shutdown = controller.shutdown
}

object Crawlers {
  val list = List(
    HubeiDaily)
    .map { c => (c.getClass.getName.substring(9).dropRight(1) -> c) } toMap

  def setup(crawlerName: String, store: String, threads: Int) = {
    val c = list.getOrElse(crawlerName, DefaultCrawler)
    new Control(c, store, threads)
  }
}