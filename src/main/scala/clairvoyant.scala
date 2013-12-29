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
trait Logging {
  val logger = Logging.getLogger(this)
}

object Logging {
  import org.slf4j.LoggerFactory

  def loggerNameForClass(className: String) =
    if (className endsWith "$")
      className.substring(0, className.length - 1)
    else
      className

  def getLogger(c: AnyRef) =
    LoggerFactory.getLogger(loggerNameForClass(c.getClass.getName))
}

object Hash {
  private val md5Instance = java.security.MessageDigest.getInstance("MD5")

  def md5(input: String) =
    md5Instance.digest(input.getBytes).map("%02x".format(_)).mkString
}

case class Page(url: String, content: String) {
  import java.io.{ File, PrintWriter }

  def storeTo(folder: String) = {
    val file = new File(folder + "/" + Hash.md5(url) + ".html")
    val writer = new PrintWriter(file)
    writer.println("<!-- " + url + " -->")
    writer.println(content)
    writer.close
  }
}

case class Link(links: List[String]) {
  def isEmpty = links.isEmpty
}

case class STOP

case class Parse(url: String) {
  import collection.JavaConversions._

  private val doc = org.jsoup.Jsoup.connect(url).get

  private def urlValid(url: String) = {
    try {
      new java.net.URL(url)
      true
    } catch {
      case _: Exception => false
    }
  }

  def getPage = Page(url, doc.html)

  def getLink =
    Link(doc.select("a").iterator.map(_.attr("abs:href")).filter(urlValid).toList)

  def getLinkWith(filters: Spider.Filters) =
    Link(filters.check(url).flatMap { selector =>
      doc.select(selector)
        .select("a").iterator.map(_.attr("abs:href")).filter(urlValid).toList
    })
}

object Spider extends Logging {
  import java.io.File
  import io.Source
  import collection.mutable.HashSet
  import util.parsing.json.JSON
  import actors.Actor
  import actors.Actor._

  case class SpiderInstance(writer: Actor, loaders: Seq[Actor], controller: Actor) {
    def stop = {
      controller ! STOP
      loaders.foreach(_ ! STOP)
      writer ! STOP

      logger.info("STOP signal has been sent to all actors ...")
    }
  }

  val filterNone = (".*".r, "#ThisIdMatchNothing")
  case class Filters(filters: List[(util.matching.Regex, String)]) {
    def check(url: String) =
      filters.filterNot { case (r, s) => r.findAllIn(url).isEmpty }
        .map { case (r, s) => s }
  }

  case class Spider(
    startURLs: List[String], concurrency: Int, filters: Filters, folder: String) {
    private var traveled = HashSet[String]()
    def crawled(url: String) = traveled.contains(url)
    def allURLs = traveled.iterator

    def run = {
      val writer = actor {
        loop {
          react {
            case page: Page => page.storeTo(folder)
            case STOP => exit
          }
        }
      }

      val loaders = (0 to concurrency - 1) map { i =>
        actor {
          loop {
            react {
              case url: String => {
                logger.info("Loader [{}]: {}", i, url)

                try {
                  val result = Parse(url)
                  val page = result.getPage
                  val links = result.getLinkWith(filters)
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
        }
      }

      val controller = actor {
        loop {
          react {
            case Link(urls) =>
              urls.filter(!crawled(_)).grouped(concurrency).foreach {
                _.zipWithIndex.foreach { case (url, index) => loaders(index) ! url }
              }
            case STOP => exit
          }
        }
      }

      controller ! Link(startURLs)

      SpiderInstance(writer, loaders, controller)
    }
  }

  def initialize(fileName: String) = {
    try {
      val fileContent = Source.fromFile(new File(fileName)).mkString
      val config = JSON.parseFull(fileContent).get.asInstanceOf[Map[String, Any]]
      val startURLs = config.get("start").get.asInstanceOf[List[String]]
      val concurrency = config.get("concurrency").get.asInstanceOf[Double].toInt
      val filters = config.get("filters").get.asInstanceOf[Map[String, String]].toList
        .map { case (r, s) => (r.r, s) }
      val _fname = config.get("store").get.toString
      val _folder = new File(_fname)
      val folder =
        if (_folder.exists)
          new File(_fname + "_" + Hash.md5(compat.Platform.currentTime.toString))
        else _folder
      folder.mkdir
      Spider(startURLs, concurrency, Filters(filters), folder.getAbsolutePath)
    } catch {
      case e: Exception => {
        println(e)
        Spider(List[String](), 1, Filters(List(filterNone)), ".")
      }
    }
  }
}

object Console {
  val prompt = "> "

  def console: Unit = {
    print(prompt)
    for (line <- io.Source.stdin.getLines) {
      val output = line.split(" ").toList match {
        case "exit" :: Nil => return
        case "" :: Nil => null
        case _ => "Unrecognized command: [%s]".format(line)
      }
      if (null != output) println(output)
      print(prompt)
    }
  }
}

object clairvoyant extends Logging {
  val usage = """clairvoyant <spider>
spider example:
{
  "start": ["http://shizhan.github.io/archive.html"],
  "concurrency": 2,
  "dalay": 1500,
  "timeout": 10000,
  "filters": {
    "^http://shizhan.github.io/archive.html$": "div.content",
    "^http://shizhan.github.io/\d{4}/\d{2}/\d{2}/.*": "div.page-header"
  },
  "store": "r:/shizhan_github_io"
}
"""

  def main(args: Array[String]) =
    if (args.length < 1) println(usage)
    else {
      val spider = Spider.initialize(args(0))
      if (!spider.startURLs.isEmpty) {
        val spiderInstance = spider.run
        Console.console
        spiderInstance.stop
        println("Traveled URI: " + spider.allURLs.length)
      } else println("spider config error")
    }
}