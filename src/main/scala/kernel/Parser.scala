package kernel

object Parser {
  import java.io.File
  import util.parsing.json.JSON
  import org.apache.commons.codec.digest.DigestUtils
  import Spider.{ Filters, Instance }

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

      Instance(startURLs, concurrency, delay, timeout, Filters(filters), folder.getAbsolutePath)
    } catch {
      case e: Exception => e
    }
  }
}