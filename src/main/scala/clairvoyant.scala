/**
 * Copyright 2013 Zhan Shi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

object clairvoyant {
  import edu.uci.ics.crawler4j.crawler.{ CrawlConfig, CrawlController, WebCrawler }
  import edu.uci.ics.crawler4j.fetcher.PageFetcher
  import edu.uci.ics.crawler4j.robotstxt.{ RobotstxtConfig, RobotstxtServer }
  import crawlers._

  private def crawlerControl(store: String, threads: Int, c: Crawlers) = {
    val config = new CrawlConfig()
    config.setCrawlStorageFolder(store)
    val pageFetcher = new PageFetcher(config)
    val robotstxtConfig = new RobotstxtConfig()
    val robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher)
    val controller = new CrawlController(config, pageFetcher, robotstxtServer)

    c.seed foreach controller.addSeed

    controller.start(c.crawler.asSubclass(classOf[WebCrawler]), threads)
  }

  val usage = "clairvoyant <local folder> <number of crawlers>"

  def main(args: Array[String]) = {
    if (args.length < 2) {
      println(usage)
    } else {
      crawlerControl(args(0), args(1).toInt, HubeiDaily)
    }
  }
}