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
  import edu.uci.ics.crawler4j.crawler.{ CrawlConfig, CrawlController }
  import edu.uci.ics.crawler4j.fetcher.PageFetcher
  import edu.uci.ics.crawler4j.robotstxt.{ RobotstxtConfig, RobotstxtServer }

  def main(args: Array[String]) = {
    val crawlStorageFolder = "/data/crawl/root"
    val numberOfCrawlers = 7

    val config = new CrawlConfig()
    config.setCrawlStorageFolder(crawlStorageFolder)

    val pageFetcher = new PageFetcher(config);
    val robotstxtConfig = new RobotstxtConfig();
    val robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
    val controller = new CrawlController(config, pageFetcher, robotstxtServer);

    controller.addSeed("http://www.ics.uci.edu/~welling/");
    controller.addSeed("http://www.ics.uci.edu/~lopes/");
    controller.addSeed("http://www.ics.uci.edu/");

    controller.start(classOf[crawlers.HubeiDaily], numberOfCrawlers);
  }
}