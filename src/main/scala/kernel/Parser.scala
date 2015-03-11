package kernel

object Parser {
  import java.io.File
  import com.typesafe.config.{ Config, ConfigFactory }
  import Spider.Instance

  def load(fileName: String) =
    try {
      val fileContent = io.Source.fromFile(new File(fileName)).mkString
      val config = ConfigFactory.load(fileName)
      val startURLs = config.getList("start").asInstanceOf[List[String]]
      val concurrency = config.getInt("concurrency")
      val delay = config.getInt("delay")
      val timeout = config.getInt("timeout")
      val filters = config.getList("filters").asInstanceOf[Map[String, String]].toList
        .map { case (r, s) => (r.r, s) }
      val _fname = config.getString("store")
      val _folder = new File(_fname)
      val folder =
        if (_folder.exists) new File(_fname + "_" + System.currentTimeMillis.toString)
        else _folder
      folder.mkdir

      Some(new Instance(startURLs, concurrency, delay, timeout, filters, folder.getAbsolutePath))
    } catch {
      case e: Exception =>
        println(e)
        None
    }
}