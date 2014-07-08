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
import kernel.Console

object clairvoyant {
  import kernel.{ Parser, Spider }
  import helper.Resource

  val usage = "clairvoyant <spider>\n\nSpider JSON example:\n" +
    Resource.getString("demo.json")

  def main(args: Array[String]) = args.toList match {
    case spiderFile :: Nil => Parser.load(spiderFile) match {
      case spider: Spider.Instance => spider.run
      case e: Exception => println(e)
    }
    case _ => println(usage)
  }
}