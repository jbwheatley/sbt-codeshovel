package sbt.codeshovel

import com.felixgrund.codeshovel.changes.Ychange
import com.felixgrund.codeshovel.entities.Ycommit
import com.felixgrund.codeshovel.exceptions.ParseException
import com.felixgrund.codeshovel.parser.{AbstractParser, Yfunction, Yparser}
import com.felixgrund.codeshovel.util.Utl
import com.felixgrund.codeshovel.wrappers.{Commit, StartEnvironment}
import sbt.codeshovel.ScalaParser.ACCEPTED_FILE_EXTENSION

import java.util
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.meta.Defn.Def
import scala.meta.parsers.Parsed
import scala.meta.{ParseException as _, *}

class ScalaParser(startEnv: StartEnvironment, filePath: String, fileContent: String, commit: Commit)
    extends AbstractParser(startEnv, filePath, fileContent, commit)
    with Yparser {

  override def getScopeSimilarity(function: Yfunction, compareFunction: Yfunction): Double = if (
    Utl.parentNamesMatch(function, compareFunction)
  ) 1.0
  else 0.0

  override def getAcceptedFileExtension: String = ACCEPTED_FILE_EXTENSION

  override protected def parseMethods(): util.List[Yfunction] =
    fileContent.parse[Source] match {
      case Parsed.Error(_, _, _) =>
        System.err.println(
          "ScalaParser::parseMethods() - parse error. path: " + this.filePath + "; content:\n" + this.fileContent
        )
        throw new ParseException("Error during parsing of content", this.filePath, this.fileContent)
      case Parsed.Success(tree) =>
        val functions: ListBuffer[Yfunction] = ListBuffer.empty
        def traverse(tree: Tree): Unit =
          tree.children.foreach {
            case d: Def =>
              functions.append(new ScalaFunction(d, commit, filePath, fileContent))
              traverse(d)
            case other => traverse(other)
          }
        traverse(tree)
        functions.asJava
      case _ => ???
    }

  override def getMinorChanges(commit: Ycommit, compareFunction: Yfunction): util.List[Ychange] = {
    val changes              = new util.ArrayList[Ychange]
    val yreturntypechange    = getReturnTypeChange(commit, compareFunction)
    val ymodifierchange      = getModifiersChange(commit, compareFunction)
    val yexceptionschange    = getExceptionsChange(commit, compareFunction)
    val ybodychange          = getBodyChange(commit, compareFunction)
    val yformatchange        = getFormatChange(commit, compareFunction)
    val yparametermetachange = getParametersMetaChange(commit, compareFunction)
    val yannotationchange    = getAnnotationChange(commit, compareFunction)
    val ydocchange           = getDocChange(commit, compareFunction)
    if (yreturntypechange != null) changes.add(yreturntypechange)
    if (ymodifierchange != null) changes.add(ymodifierchange)
    if (yexceptionschange != null) changes.add(yexceptionschange)
    if (ybodychange != null) changes.add(ybodychange)
    if (yparametermetachange != null) changes.add(yparametermetachange)
    if (yannotationchange != null) changes.add(yannotationchange)
    if (ydocchange != null) changes.add(ydocchange)
    if (yformatchange != null) changes.add(yformatchange)
    changes
  }
}

object ScalaParser {
  val ACCEPTED_FILE_EXTENSION = ".*\\.sc(ala)?$"
}
