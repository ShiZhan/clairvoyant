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
object Page {
  private val linkRegex = "(?i)<a.+?href=\"(http.+?)\".*?>(.+?)</a>".r

  def load(url: String) = {
    try {
      val content = io.Source.fromURL(url).mkString
      val links = linkRegex.findAllIn(content).matchData.toList.map(_.group(1))
      (content, links)
    } catch {
      case e: Exception =>
        System.err.println(e)
        ("", List[String]())
    }
  }
}

object clairvoyant {
  import java.io.File
  import actors.Futures

  val usage = "clairvoyant <url> <local folder>"

  def main(args: Array[String]) = {
    if (args.length < 2) {
      println(usage)
    } else {
      val store = args(1)
      val url = args(0)
      val (page, links) = Page.load(url)
      print(url + ": " + page.length)
      val pages = links.map { u => Futures.future(Page.load(u)) }
      pages.foreach { f => println(f()) }
    }
  }
}