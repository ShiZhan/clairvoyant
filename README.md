clairvoyant
===========
A conservative spider [download](https://dl.dropboxusercontent.com/u/70916622/clairvoyant-assembly-1.0.jar).

The word **conservative** here stands for:

1. links will only be gathered from specified document area, e.g.: a few "div"s or "table"s, HTML parsing based on [jsoup](http://jsoup.org/).
2. use only **white list** to direct the crawling path, the spider will not go elsewhere, strictly limit the searching boundary.
3. simple to use, locate and type `java -jar clairvoyant-assembly-1.0.jar` to see the help information, write **json**, then go.

spider format
=============
1. start: starting URLs
2. concurrency: maximum concurrent threads
3. delay: the wait in milliseconds before next crawl operation 
4. timeout: connection time out
5. filters: a list of filter `TUPLE(a valid regex for matching URLs, a JQuery-style selector for designating an area in HTML page)`
6. store: provide a local directory to store crawled HTML pages

author
======
[ShiZhan](http://shizhan.github.io/) (c) 2013 [Apache License Version 2.0](http://www.apache.org/licenses/)