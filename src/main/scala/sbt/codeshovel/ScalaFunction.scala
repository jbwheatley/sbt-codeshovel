package sbt.codeshovel

import com.felixgrund.codeshovel.entities.{Yexceptions, Ymodifiers, Yparameter}
import com.felixgrund.codeshovel.parser.{AbstractFunction, Yfunction}
import com.felixgrund.codeshovel.wrappers.Commit

import java.util
import scala.jdk.CollectionConverters.*
import scala.meta.Defn.Def
import scala.meta.Member.Term
import scala.meta.Mod.Annot
import scala.meta.contrib.AssociatedComments

class ScalaFunction(method: Def, commit: Commit, sourceFilePath: String, sourceFileContent: String)
    extends AbstractFunction[Def](
      method: Def,
      commit: Commit,
      sourceFilePath: String,
      sourceFileContent: String
    )
    with Yfunction {

  override protected def getInitialName(rawMethod: Def): String = rawMethod.name.value

  override protected def getInitialType(rawMethod: Def): String =
    rawMethod.decltpe
      .map(_.toString())
      .getOrElse("")

  override protected def getInitialModifiers(rawMethod: Def): Ymodifiers = new Ymodifiers(
    rawMethod.mods.filterNot(_.is[Annot]).map(_.toString()).asJava
  )

  override protected def getInitialExceptions(rawMethod: Def): Yexceptions = Yexceptions.NONE

  override protected def getInitialParameters(rawMethod: Def): util.List[Yparameter] =
    rawMethod.paramClauses
      .flatMap(
        _.values.map { p =>
          val mods     = p.mods.filterNot(_.is[Annot]).map(_.toString()).mkString("-")
          val annots   = p.mods.collect { case a: Annot => a }.map(_.toString()).mkString("-")
          val metadata = Map("modifiers" -> mods, "annotations" -> annots).filter { case (_, v) => v.nonEmpty }
          val yp       = new Yparameter(p.name.value, p.decltpe.map(_.toString()).getOrElse(""))
          yp.setMetadata(metadata.asJava)
          yp
        }
      )
      .asJava

  override protected def getInitialBody(rawMethod: Def): String = rawMethod.body.toString()

  override protected def getInitialBeginLine(rawMethod: Def): Int = rawMethod.pos.startLine + 1

  override protected def getInitialEndLine(rawMethod: Def): Int = rawMethod.pos.endLine + 1

  override protected def getInitialParentName(rawMethod: Def): String = rawMethod.parent.collect { case o: Term =>
    o.name.value
  }.orNull

  override protected def getInitialFunctionPath(rawMethod: Def): String = null

  override protected def getInitialAnnotation(rawMethod: Def): String =
    rawMethod.mods.collect { case a: Annot => a }.map(_.toString()).mkString(",")

  override protected def getInitialDoc(rawMethod: Def): String = rawMethod.parent
    .map { p =>
      AssociatedComments(p).leading(rawMethod).map(_.value).mkString("")
    }
    .getOrElse("")

  override protected def getInitialUnformattedBody(rawMethod: Def): String =
    rawMethod.body.toString().filterNot(_.isWhitespace)

  override protected def getInitialId(rawMethod: Def): String = {
    var name = getName
    val isNestedMethod = rawMethod.parent
      .flatMap(_.parent.collect { case _: Term =>
        true
      })
      .getOrElse(false)
    if (isNestedMethod) name = "$" + name
    val idParameterString = getIdParameterString
    if (idParameterString.nonEmpty) name = name + "___" + idParameterString
    name
  }
}
