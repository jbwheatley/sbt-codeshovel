package codeshovel

import com.felixgrund.codeshovel.execution.ShovelExecution
import com.felixgrund.codeshovel.services.impl.CachingRepositoryService
import com.felixgrund.codeshovel.util.Utl
import com.felixgrund.codeshovel.wrappers.{Commit, StartEnvironment}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository

object Execution {
  def run(
      repositoryName: String,
      baseDir: String,
      filePath: String,
      functionName: String,
      startLine: Int,
      startCommitName: String
  ): Unit = {
    val repository: Repository = Utl.createRepository(baseDir + "/.git")
    val git: Git               = new Git(repository)
    val repositoryService: CachingRepositoryService =
      new CachingRepositoryService(git, repository, repositoryName, baseDir)
    val startCommit: Commit = repositoryService.findCommitByName(startCommitName)

    val startEnv: StartEnvironment = new StartEnvironment(repositoryService)
    startEnv.setRepositoryPath(baseDir)
    startEnv.setFilePath(filePath)
    startEnv.setFunctionName(functionName)
    startEnv.setFunctionStartLine(startLine)
    startEnv.setStartCommitName(startCommitName)
    startEnv.setStartCommit(startCommit)
    startEnv.setFileName(Utl.getFileName(startEnv.getFilePath))
    startEnv.setOutputFilePath(
      baseDir + s"/target/shovel-$functionName-$startLine-$startCommitName.json"
    )

    ShovelExecution.runSingle(startEnv, startEnv.getFilePath, true)
    ()
  }
}
