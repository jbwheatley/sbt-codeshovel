package sbt.codeshovel

import com.felixgrund.codeshovel.execution.ShovelExecution
import com.felixgrund.codeshovel.services.impl.CachingRepositoryService
import com.felixgrund.codeshovel.util.Utl
import com.felixgrund.codeshovel.wrappers.StartEnvironment
import org.eclipse.jgit.api.Git
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
  val directory: Parser[File]    = Space ~> StringBasic.map(p => new File(p))
  private val fn: Parser[String] = Space ~> StringBasic
  private val sl: Parser[Int]    = Space ~> IntBasic
  private val sc: Parser[String] = Space ~> StringBasic
  lazy val shovelParser: Parser[ShovelCommand] = for {
    file <- directory
    fn   <- fn
    sl   <- sl
    sc   <- sc
  } yield ShovelCommand(file, fn, sl, sc)

  def run(name: String, state: State, input: ShovelCommand): Unit = {
    val baseDir                 = Project.extract(state).get(baseDirectory).getAbsolutePath
    val repositoryName          = name
    val filePath                = input.filePath
    val functionName            = input.functionName
    val functionStartLine       = input.startLine
    val startCommitName: String = input.startCommit
    val repository              = Utl.createRepository(baseDir + "/.git")
    val git                     = new Git(repository)
    val repositoryService       = new CachingRepositoryService(git, repository, repositoryName, baseDir)
    val startCommit             = repositoryService.findCommitByName(startCommitName)

    val startEnv = new StartEnvironment(repositoryService)
    startEnv.setRepositoryPath(baseDir)
    startEnv.setFilePath(filePath.getPath)
    startEnv.setFunctionName(functionName)
    startEnv.setFunctionStartLine(functionStartLine)
    startEnv.setStartCommitName(startCommitName)
    startEnv.setStartCommit(startCommit)
    startEnv.setFileName(Utl.getFileName(startEnv.getFilePath))
    startEnv.setOutputFilePath(
      baseDir + s"/target/shovel-$functionName-$functionStartLine-$startCommitName.json"
    )

    ShovelExecution.runSingle(startEnv, startEnv.getFilePath, true)
    ()
  }
}
