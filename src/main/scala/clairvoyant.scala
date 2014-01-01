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
object Spider {
  import java.io.{ File, PrintWriter }
  import collection.JavaConversions._
  import util.parsing.json.JSON
  import actors.Actor
  import actors.Actor._
  import org.apache.commons.codec.digest.DigestUtils
  import org.apache.commons.validator.routines.UrlValidator
  import org.jsoup.Jsoup
  import org.jsoup.nodes.Document

  private val log = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val httpSchemes = Array("http", "https")
  private val httpValidator = new UrlValidator(httpSchemes)

  private val filterNone = (".*".r, "#ThisIdMatchNothing")
  case class Filters(filters: List[(util.matching.Regex, String)]) {
    def check(url: String) = filters
      .filterNot { case (r, s) => r.findAllIn(url).isEmpty }
      .map { case (r, s) => s }
  }

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

  case class STOP

  case class Spider(startURLs: List[String], concurrency: Int,
    delay: Int, timeout: Int, filters: Filters, folder: String) {
    private var traveled = collection.mutable.HashSet[String]()
    private def crawled(url: String) = traveled.contains(url)

    val writer = actor {
      loop {
        react {
          case page: Page => page.storeTo(folder)
          case STOP => exit
        }
      }
    }

    val loaders = (0 to concurrency - 1).map(i =>
      actor {
        loop {
          react {
            case url: String => {
              log.info("Loader [{}]: {}", i, url)
              try {
                val doc = Jsoup.parse(new java.net.URL(url), timeout)
                val page = Page(url, doc)
                val links = page.getLinkWith(filters)
                writer ! page
                if (!links.isEmpty) sender ! links
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
          case STOP => exit
        }
      }
    }

    def run = controller ! Link(startURLs)

    def stop = {
      controller ! STOP
      loaders.foreach(_ ! STOP)
      writer ! STOP
      log.info("STOP signal has been sent to all actors ...")
    }

    override def toString = {
      val filterTotal = filters.filters.length
      val traveledURLs = traveled.size
      s"""------
Start URL: $startURLs
concurrency: $concurrency, delay: $delay ms, timeout: $timeout ms,
Filter total: $filterTotal
Folder: $folder
Traveled URLs: $traveledURLs"""
    }
  }

  def load(fileName: String) = {
    try {
      val fileContent = io.Source.fromFile(new File(fileName)).mkString
      val config = JSON.parseFull(fileContent).get.asInstanceOf[Map[String, Any]]
      val startURLs = config.get("start").get.asInstanceOf[List[String]]
      val concurrency = config.get("concurrency").get.asInstanceOf[Double].toInt
      val delay = config.get("delay").get.asInstanceOf[Double].toInt
      val timeout = config.get("timeout").get.asInstanceOf[Double].toInt
      val filters = config.get("filters").get.asInstanceOf[Map[String, String]].toList
        .map { case (r, s) => (r.r, s) }
      val _fname = config.get("store").get.toString
      val _folder = new File(_fname)
      val folder =
        if (_folder.exists)
          new File(_fname + "_" +
            DigestUtils.md5Hex(compat.Platform.currentTime.toString))
        else _folder
      folder.mkdir

      Spider(startURLs, concurrency, delay, timeout,
        Filters(filters), folder.getAbsolutePath)
    } catch {
      case e: Exception => e
    }
  }
}

object Console {
  val prompt = "\n> "

  def console: Unit = {
    for (line <- io.Source.stdin.getLines) {
      val output = line.split(" ").toList match {
        case "exit" :: Nil => return
        case "" :: Nil => ""
        case _ => "Unrecognized command: [%s]".format(line)
      }
      print(output + prompt)
    }
  }
}

object clairvoyant {
  val demoJson = getClass.getResourceAsStream("demo.json")
  val usage = "clairvoyant <spider>\n\nSpider JSON example:\n" +
    io.Source.fromInputStream(demoJson).mkString

  def main(args: Array[String]) =
    if (args.length < 1) println(usage)
    else
      Spider.load(args(0)) match {
        case spider: Spider.Spider => {
          spider.run
          Console.console
          spider.stop
          println(spider)
        }
        case e: Exception => println(e)
      }
}