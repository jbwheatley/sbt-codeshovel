# sbt-codeshovel

![Tag](https://img.shields.io/github/v/tag/jbwheatley/sbt-codeshovel?sort=semver)

"Take this shovel to dig in source code history for changes to specific methods and functions."

* [Getting Started](#getting-started)
* [Usage](#usage)
* [Use with other build tools (Mill, Bazel)](#use-with-other-build-tools-mill-bazel)
* [Background](#background)

## Getting Started

`sbt-codeshovel` is available through Maven Central. 

Add the sbt plugin to `plugins.sbt`:

```scala
addSbtPlugin("io.github.jbwheatley" %% "sbt-codeshovel" % xxx)
```

and enable in `build.sbt`: 

```scala
enablePlugins(CodeShovelPlugin)
```

## Usage

Start an sbt shell in the root of your repo, and run the following: 

```bash 
shovel ${path/to/the/file.scala} ${functionName} ${lineNumber} ${commit}
```

For example, to run against [this function](https://github.com/jbwheatley/pact4s/blob/10b907e625f4057b1202567096f95079f0999895/shared/src/main/scala/pact4s/StateChanger.scala#L68) in pact4s starting from the HEAD commit: 

```bash
shovel shared/src/main/scala/pact4s/StateChanger.scala handle 68 HEAD
```

`sbt-codeshovel` also works for `java`, `js` and `ts` files. 

A `.html` file will then be produced and placed in the `/target` directory (e.g. `/target/shovel-handle-68-HEAD.html`). Opening this in your 
browser will allow you to browse the git history for the supplied method. [Check the example here.](./doc/example.html)

## Use with other build tools (Mill, Bazel)

The core functionality of codeshovel is released as its own standalone library to be used with other build tools: 

```scala
"io.github.jbwheatley" %% "codeshovel" % xxx
```

The method `codeshovel.Execution.run` can be used to produce the html document: 

```scala
run(
  repositoryName = "pact4s",
  baseDir = "/Users/jbwheatley/pact4s",
  filePath = "shared/src/main/scala/pact4s/StateChanger.scala",
  functionName = "handle",
  startLine = 68,
  startCommitName = "HEAD"
)
```

## Background

`sbt-codeshovel` is a fork of [`ataraxie/codeshovel`](https://github.com/ataraxie/codeshovel) which is based on research by the [Software Practices Lab](https://spl.cs.ubc.ca) at UBC in Vancouver, Canada.
This plugin takes their excellent original work and extends it to work for scala functions, and makes it more convenient for everyday development needs. 