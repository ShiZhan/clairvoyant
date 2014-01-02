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
  import spider.Spider
  import console.Console

  val demoJson = getClass.getResourceAsStream("demo.json")
  val usage = "clairvoyant <spider>\n\nSpider JSON example:\n" +
    io.Source.fromInputStream(demoJson).mkString

  def main(args: Array[String]) =
    if (args.length < 1) println(usage)
    else
      Spider.load(args(0)) match {
        case spider: Spider.Instance => {
          spider.run
          Console.run
          spider.stop
          println(spider)
        }
        case e: Exception => println(e)
      }
}