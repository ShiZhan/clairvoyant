/**
 * Console loop
 */
package kernel

/**
 * @author ShiZhan
 * Console loop for interacting with running spider process
 */
object Console {
  val prompt = "\n> "

  def run: Unit =
    for (line <- io.Source.stdin.getLines) {
      val output = line.split(" ").toList match {
        case "exit" :: Nil => return
        case "" :: Nil => ""
        case _ => "Unrecognized command: [%s]".format(line)
      }
      print(output + prompt)
    }
}