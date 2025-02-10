# sbt-codeshovel

![Tag](https://img.shields.io/github/v/tag/jbwheatley/sbt-codeshovel?sort=semver)

"Take this shovel to dig in source code history for changes to specific methods and functions."

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

For example, to run against [this function](https://github.com/jbwheatley/pact4s/blob/10b907e625f4057b1202567096f95079f0999895/shared/src/main/scala/pact4s/TimeLimiter.scala#L24) in pact4s starting from the HEAD commit: 

```bash
shovel shared/src/main/scala/pact4s/TimeLimiter.scala callWithTimeout 24 HEAD
```

`sbt-codeshovel` also works for `java`, `js` and `ts` files. 

A `.html` file will then be produced and placed in the `/target` directory (e.g. `/target/shovel-callWithTimeout-24-HEAD.html`). Opening this in your 
browser will allow you to browse the git history for the supplied method. 


## Background

`sbt-codeshovel` is a fork of [`ataraxie/codeshovel`](https://github.com/ataraxie/codeshovel) which is based on research by the [Software Practices Lab](https://spl.cs.ubc.ca) at UBC in Vancouver, Canada.
This plugin takes their excellent original work and extends it to work for scala functions, and makes it more convenient for everyday development needs. 