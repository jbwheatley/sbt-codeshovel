import sbt.Keys.scalaVersion

val scala212         = "2.12.20"
val scala213         = "2.13.16"
val scala3           = "3.3.5"
val allScalaVersions = Seq(scala212, scala213, scala3)

sonatypeCredentialHost := Sonatype.sonatype01

inThisBuild(
  List(
    organization := "io.github.jbwheatley",
    homepage     := Some(url("https://github.com/jbwheatley/sbt-codeshovel")),
    developers := List(
      Developer(
        "jbwheatley",
        "jbwheatley",
        "jbwheatley@proton.me",
        url("https://github.com/jbwheatley")
      )
    ),
    startYear := Some(2025),
    licenses := List(
      "MIT"        -> url("https://mit-license.org/"),
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    scalaVersion := scala212
  )
)

lazy val root = (project in file("codeshovel"))
  .settings(
    name               := "codeshovel",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      "org.eclipse.jgit"       % "org.eclipse.jgit"              % "7.1.0.202411261347-r",
      "com.google.code.gson"   % "gson"                          % "2.12.1",
      "commons-io"             % "commons-io"                    % "2.18.0",
      "org.apache.commons"     % "commons-text"                  % "1.13.0",
      "org.slf4j"              % "slf4j-simple"                  % "2.0.16",
      "com.github.javaparser"  % "javaparser-symbol-solver-core" % "3.26.3",
      "com.carrotsearch"       % "java-sizeof"                   % "0.0.5",
      "commons-cli"            % "commons-cli"                   % "1.9.0",
      "org.antlr"              % "antlr4-runtime"                % "4.13.2",
      "com.eclipsesource.j2v8" % "j2v8_macosx_x86_64"            % "4.6.0",
      "org.scalameta"         %% "scalameta"                     % "4.12.7",
      // https://github.com/scalameta/scalameta/issues/4145
      ("com.lihaoyi"           %% "scalatags"               % "0.13.1").withCrossVersion(CrossVersion.for3Use2_13),
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.13.0",
      "org.junit.jupiter"       % "junit-jupiter-engine"    % "5.11.4" % Test
    )
  )

lazy val sbtPlugin =
  (project in file("sbt-codeshovel"))
    .enablePlugins(SbtPlugin)
    .settings(name := "sbt-codeshovel", crossScalaVersions := Nil)
    .dependsOn(root)
