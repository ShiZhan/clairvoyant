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

case class Page(uri: String, content: String)
case class Link(links: List[String]) { def isEmpty = links.isEmpty }
case class STOP

object Parser {
  import scala.collection.JavaConversions._
  import collection.mutable.HashSet
  import java.net.URL
  import org.jsoup.Jsoup

  private var traveled = HashSet[String]()
  def crawled(url: String) = traveled.contains(url)
  def allURLs = traveled.iterator

  def urlValid(url: String) = {
    try {
      new URL(url)
      true
    } catch {
      case _: Exception => false
    }
  }

  def load(url: String) = {
    val doc = Jsoup.connect(url).get
    // TODO: add more link filter here, using selector
    //       filters can be triggered by specified url
    //       maybe a json configuration file is required
    val area =
      if (url == """http://shizhan.github.io/archive.html""")
        doc.select("div.content")
      else
        doc.select("div.page-header")
    val links =
      area.select("a").iterator.map(_.attr("abs:href")).filter(urlValid).toList
    traveled += url
    (Page(url, doc.data), Link(links))
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
  import actors.Actor._
  import Parser._
  import Console.console

  val usage = "clairvoyant <local folder> <concurrency> <url ...>"

  def main(args: Array[String]) = if (args.length < 3) println(usage)
  else {
    val store = args(0)
    val concurrency = args(1).toShort
    val startURLs = args.drop(2).toList

    val writer = actor {
      loop {
        react {
          case Page(url, content) => {}
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
                val (page, links) = load(url)
                if (!links.isEmpty) sender ! links
                writer ! page
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
          case Link(urls) => urls.filter(u => !crawled(u)).grouped(concurrency)
            .foreach {
              _.zipWithIndex.foreach { case (url, index) => loaders(index) ! url }
            }
          case STOP => exit
        }
      }
    }

    controller ! Link(startURLs)

    console

    controller ! STOP
    loaders.foreach(_ ! STOP)
    writer ! STOP

    logger.info("Traveled URI: [{}]", allURLs.length)
  }
}