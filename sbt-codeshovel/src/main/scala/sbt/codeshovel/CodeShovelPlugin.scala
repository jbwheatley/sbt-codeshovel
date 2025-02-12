package sbt.codeshovel

import _root_.codeshovel.Execution
import sbt.Keys.*
import sbt.codeshovel.Shovel.shovelParser
import sbt.complete.DefaultParsers.*
import sbt.complete.Parser
import sbt.{Def, *}

object CodeShovelPlugin extends AutoPlugin {

  lazy val shovel = Command("shovel")(_ => shovelParser) { (s, c) =>
    Shovel.run(Project.extract(s).get(name), s, c)
    s
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    commands += shovel
  )
}

object Shovel {
  case class ShovelCommand(filePath: File, functionName: String, startLine: Int = -1, startCommit: String = "HEAD")
  private val directory: Parser[File] = Space ~> StringBasic.map(p => new File(p))
  private val fn: Parser[String]      = Space ~> StringBasic
  private val sl: Parser[Int]         = Space ~> IntBasic
  private val sc: Parser[String]      = Space ~> StringBasic
  lazy val shovelParser: Parser[ShovelCommand] = for {
    file <- directory
    fn   <- fn
    sl   <- sl
    sc   <- sc
  } yield ShovelCommand(file, fn, sl, sc)

  def run(repoName: String, state: State, input: ShovelCommand): Unit = {
    val baseDir                 = Project.extract(state).get(baseDirectory).getAbsolutePath
    val filePath                = input.filePath
    val functionName            = input.functionName
    val functionStartLine       = input.startLine
    val startCommitName: String = input.startCommit
    Execution.run(repoName, baseDir, filePath.getPath, functionName, functionStartLine, startCommitName)
  }
}
