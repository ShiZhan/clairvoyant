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
case class Page(uri: String, content: String)
case class Link(links: List[String])
case class STOP

trait Parser {
  private var traveled = collection.mutable.HashSet[String]()
  def crawled(url: String) = traveled.contains(url)
  def interested(url: String) = true
  def allURLs = traveled.iterator

  private val linkRegex = "(?i)<a.+?href=\"(http.+?)\".*?>(.+?)</a>".r
  def load(url: String) = {
    traveled += url
    try {
      val content = io.Source.fromURL(url).mkString
      val links = linkRegex.findAllIn(content).matchData.toList.map(_.group(1)).distinct
      (Page(url, content), Link(links))
    } catch {
      case e: Exception =>
        System.err.println(e)
        (Page(url, ""), Link(List[String]()))
    }
  }
}

object Demo extends Parser {}

object Console {
  def console: Unit = {
    val prompt = "> "
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

object clairvoyant {
  import actors.Actor._
  import Demo._
  import Console.console

  val usage = "clairvoyant <local folder> <threads> <url ...>"

  def main(args: Array[String]) = if (args.length < 3) println(usage)
  else {
    val store = args(0)
    val threads = args(1).toShort
    val startURLs = args.drop(2).toList

    val writer = actor {
      loop {
        react {
          case Page(url, content) => {}
          case STOP => exit
        }
      }
    }

    val loaders = (0 to threads - 1) map { i =>
      actor {
        loop {
          react {
            case url: String => {
              val (page, links) = load(url)
              println(url)
              sender ! links
              writer ! page
            }
            case STOP => exit
          }
        }
      }
    }

    val controller = actor {
      loop {
        react {
          case Link(urls) => urls.filter(u => !crawled(u) & interested(u))
            .grouped(threads).foreach {
              _.zipWithIndex.foreach {
                case (url, index) => loaders(index) ! url
              }
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

    println("Traveled URI: " + allURLs.length)
  }
}