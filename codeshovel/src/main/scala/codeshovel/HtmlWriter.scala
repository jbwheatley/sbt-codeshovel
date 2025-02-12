package codeshovel

import com.felixgrund.codeshovel.changes._
import com.felixgrund.codeshovel.json.JsonResult
import org.scalameta.collections.XtensionJavaList
import scalatags.Text
import scalatags.Text.all._

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.jdk.CollectionConverters._

object HtmlWriter {
  private def diffString(hash: String, change: Ychange): String =
    change match {
      case filechange: Ycrossfilechange =>
        val oldFile = filechange.getOldFunction.getSourceFilePath
        val newFile = filechange.getNewFunction.getSourceFilePath
        s"""diff --git --- a/$oldFile b/$newFile
           |index $hash
           |@@ -1,1 +1,1 @@
           |- $oldFile
           |+ $newFile
           |""".stripMargin
      case ycomparefunctionchange: Ycomparefunctionchange =>
        val file = ycomparefunctionchange.getNewFunction.getSourceFilePath
        val diff = ycomparefunctionchange.getDiffAsString
        s"""diff --git --- a/$file b/$file
           |index $hash
           |--- $file
           |+++ $file
           |${diff.replace("\\ No newline at end of file", "")}
           |""".stripMargin

      case yintroduced: Yintroduced =>
        val file = yintroduced.newFunction.getSourceFilePath
        val diff = yintroduced.getDiffAsString
        s"""diff --git --- a/$file b/$file
           |index $hash
           |--- $file
           |+++ $file
           |${diff.replace("\\ No newline at end of file", "")}
           |""".stripMargin
      case ymultichange: Ymultichange =>
        val changes = ymultichange.getChanges.toScala
        val maybeMoved = changes.collectFirst { case ycrossfilechange: Ycrossfilechange =>
          ycrossfilechange
        }
        val (currentFile, diff) = changes
          .collectFirst {
            case f: Ycomparefunctionchange => (f.getNewFunction.getSourceFilePath, f.getDiffAsString)
            case i: Yintroduced            => (i.newFunction.getSourceFilePath, i.getDiffAsString)
          }
          .getOrElse("" -> "")
        val oldFile = maybeMoved.map(_.getOldFunction.getSourceFilePath).getOrElse(currentFile)
        s"""diff --git --- a/$oldFile b/$currentFile
           |index $hash
           |--- $oldFile
           |+++ $currentFile
           |${diff.replace("\\ No newline at end of file", "")}
           |""".stripMargin
      case _ => ""
    }

  // outputFormat can be `line-by-line` or `side-by-side`
  private def mkScript(changes: Map[String, Ychange]): String = {
    val addListenerFunction = """function addListener(element, diff) {
      |  document.addEventListener('DOMContentLoaded', function () {
      |    var targetElement = document.getElementById(element);
      |    var configuration = {
      |      drawFileList: false,
      |      fileListToggle: false,
      |      fileListStartVisible: false,
      |      fileContentToggle: false,
      |      matching: 'lines',
      |      outputFormat: 'line-by-line',
      |      synchronisedScroll: true,
      |      highlight: true,
      |      renderNothingWhenEmpty: false,
      |      stickyFileHeaders: false,
      |    };
      |    var diff2htmlUi = new Diff2HtmlUI(targetElement, diff, configuration);
      |    diff2htmlUi.draw();
      |    diff2htmlUi.highlightCode();
      |  });
      |}
      |""".stripMargin
    val apply = changes
      .filter {
        case (_, _: Ynochange) => false
        case (_, _)            => true
      }
      .map { case (hash, change) =>
        s"""
        |const diff$hash = `${diffString(hash, change)}`;
        |addListener('$hash', diff$hash);
        |""".stripMargin
      }
    addListenerFunction + apply.mkString("\n")
  }

  private val style: Text.TypedTag[String] = tag("style")(
    """
      |body {
      |  font-family: -apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Fira Sans,Droid Sans,Helvetica Neue,sans-serif;
      |}
      |
      |table {
      |  text-align: left;
      |  position: relative;
      |}
      |
      |th {
      |  background: white;
      |  position: sticky;
      |  top: 0;
      |  z-index: 999;
      |  text-align: center;
      |}
      |
      |#managerTable {
      |  max-height: 90vh;
      |  overflow: auto;
      |}
      |
      |.datarow {
      |  text-align: center;
      |}
      |
      |.accordion {
      |  background-color: #eee;
      |  color: #444;
      |  cursor: pointer;
      |  padding: 18px;
      |  width: 100%;
      |  text-align: left;
      |  border: none;
      |  outline: none;
      |  transition: 0.4s;
      |}
      |
      |.active, .accordion:hover {
      |  background-color: #ccc;
      |}
      |
      |.panel {
      |  padding: 0 18px;
      |  background-color: white;
      |  visibility: collapse;
      |  overflow: hidden;
      |}
      |
      |.accordion:after {
      |  content: '\02795'; /* Unicode character for "plus" sign (+) */
      |  font-size: 13px;
      |  color: #777;
      |  float: right;
      |  margin-left: 5px;
      |}
      |
      |.active:after {
      |  content: "\2796"; /* Unicode character for "minus" sign (-) */
      |}
      |
      |""".stripMargin
  )

  private def mkHtml(result: JsonResult): Text.TypedTag[String] =
    html(
      head(
        meta(charset := "utf-8"),
        link(
          rel  := "stylesheet",
          href := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/10.7.1/styles/github.min.css"
        ),
        link(
          rel    := "stylesheet",
          `type` := "text/css",
          href   := "https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css"
        ),
        script(
          `type` := "text/javascript",
          src    := "https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js"
        ),
        style
      ),
      script(
        raw(mkScript(result.getChangeHistoryDetails.asScala.toMap.map { case (k, v) => k.take(7) -> v }))
      ),
      body(
        h1(s"${result.getRepositoryName} - ${result.getFunctionName}"),
        changes(result),
        hideCodeScript
      )
    )

  private val hideCodeScript = script(
    raw("""
          |var acc = document.getElementsByClassName("accordion");
          |var i;
          |
          |for (i = 0; i < acc.length; i++) {
          |  acc[i].addEventListener("click", function() {
          |    /* Toggle between adding and removing the "active" class,
          |    to highlight the button that controls the panel */
          |    this.classList.toggle("active");
          |
          |    /* Toggle between hiding and showing the active panel */
          |    var panel = this.parentNode.parentNode.nextElementSibling.cells[0];
          |    if (panel.style.visibility === "visible") {
          |      panel.style.visibility = "collapse";
          |    } else {
          |      panel.style.visibility = "visible";
          |    }
          |  });
          |} """.stripMargin)
  )

  def file(change: Ychange): String = (change match {
    case ycomparefunctionchange: Ycomparefunctionchange => ycomparefunctionchange.getNewFunction.getSourceFilePath
    case yintroduced: Yintroduced                       => yintroduced.newFunction.getSourceFilePath
    case ymultichange: Ymultichange =>
      val changes = ymultichange.getChanges.toScala
      changes.collectFirst { case ycrossfilechange: Ycrossfilechange =>
        ycrossfilechange
      } match {
        case Some(value) => value.getNewFunction.getSourceFilePath
        case None =>
          changes.collectFirst { case f: Ycomparefunctionchange => f } match {
            case Some(value) => value.getNewFunction.getSourceFilePath
            case None        => ""
          }
      }
    case _ => ""
  }).split("/").last

  def changeType(change: Ychange): String = change match {
    case ycomparefunctionchange: Ycomparefunctionchange =>
      ycomparefunctionchange match {
        case _: Ybodychange => "Body"
        case ycrossfilechange: Ycrossfilechange =>
          ycrossfilechange match {
            case _: Yfilerename   => "File Rename"
            case _: Ymovefromfile => "Move From File"
            case _                => ""
          }
        case ysignaturechange: Ysignaturechange =>
          ysignaturechange match {
            case _: Yexceptionschange => "Exceptions"
            case _: Ymodifierchange   => "Modifier"
            case _: Yparameterchange  => "Parameter"
            case _: Yrename           => "Rename"
            case _: Yreturntypechange => "Return"
            case _                    => ""
          }
        case _ => ""
      }
    case _: Yintroduced  => "Introduction"
    case _: Ymultichange => "Multiple"
    case _: Ynochange    => "None"
    case _               => ""
  }

  def diffText(change: Ychange): String = change match {
    case _: Ycrossfilechange       => "Path"
    case _: Ycomparefunctionchange => "Code"
    case _: Yintroduced            => "Code"
    case ymultichange: Ymultichange =>
      if (
        !ymultichange.getChanges.toScala.exists {
          case _: Ycrossfilechange => true
          case _                   => false
        }
      ) "Code"
      else "Path"
    case _ => "Diff"
  }

  def changes(result: JsonResult) = {
    val headers = tr(th("Date"), th("Author"), th("Commit"), th("File"), th("Type"), th("Diff"))
    val rows: List[Text.TypedTag[String]] = result.getChangeHistoryDetails.asScala.toList.flatMap {
      case (commitHash, change) =>
        val commit = change.getCommit
        tr(
          `class` := "datarow",
          td(commit.getCommitDate.toString),
          td(
            link(
              commit.getAuthorName,
              href := s"https://github.com/search?q=${commit.getAuthorName.replace(' ', '+')}&type=Users"
            )
          ),
          td(commitHash.take(7)),
          td(file(change)),
          td(changeType(change)),
          td(button(diffText(change), `class` := "accordion"))
        ) :: tr(td(`class` := "panel", colspan := 6, div(id := commitHash.take(7)))) :: Nil
    }
    div(id := "managerTable")(table((headers :: rows): _*))
  }

  def write(result: JsonResult, file: Path): Unit = {
    Files
      .writeString(
        file,
        mkHtml(result).render,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
    ()
  }
}
