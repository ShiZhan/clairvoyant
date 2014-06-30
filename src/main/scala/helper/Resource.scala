/**
 * API for getting build-in resources
 */
package helper

/**
 * @author ShiZhan
 * API for getting build-in resources
 */
object Resource {
  def getInputStream(name: String) =
    getClass.getClassLoader.getResourceAsStream(name)

  def getString(name: String) =
    io.Source.fromInputStream(getInputStream(name)).mkString
}