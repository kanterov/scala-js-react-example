import sbt._
import Keys._
import play.PlayScala
import play.Play._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.packager.universal.UniversalKeys
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys

object ApplicationBuild extends Build with UniversalKeys {

  val SharedSrcDir = "scala"

  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  override def rootProject = Some(scalajvm)

  lazy val commonSettings = Seq(
    resolvers += "scalajs-react bintray" at "http://dl.bintray.com/kanterov/maven"
  )

  lazy val scalajvm = Project(
    id = "scalajvm",
    base = file("scalajvm")
  ) enablePlugins (PlayScala) settings (scalajvmSettings: _*) settings (commonSettings: _*) aggregate (scalajs)

  lazy val scalajs = Project(
    id = "scalajs",
    base = file("scalajs")
  ) enablePlugins(ScalaJSPlugin) settings (scalajsSettings: _*) settings (commonSettings: _*)

  lazy val sharedScala = Project(
    id = "sharedScala",
    base = file(SharedSrcDir)
  ) settings (sharedScalaSettings: _*) settings (scalajsSettings: _*)

  lazy val scalajvmSettings =
    Seq(
      name := "play-example",
      version := Versions.app,
      scalaVersion := Versions.scala,
      scalajsOutputDir := (classDirectory in Compile).value / "public" / "javascripts",
      compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in (scalajs, Compile)) dependsOn copySourceMapsTask,
      dist <<= dist dependsOn (fullOptJS in (scalajs, Compile)),
      stage <<= stage dependsOn (fullOptJS in (scalajs, Compile)),
      libraryDependencies ++= Dependencies.scalajvm.value,
      EclipseKeys.skipParents in ThisBuild := false
    ) ++ (
      // ask scalajs project to put its outputs in scalajsOutputDir
      Seq(packageJSDependencies, packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
        crossTarget in (scalajs, Compile, packageJSKey) := scalajsOutputDir.value
      }
    ) ++ sharedDirectorySettings

  lazy val scalajsSettings =
    Seq(
      name := "scalajs-example",
      version := Versions.app,
      scalaVersion := Versions.scala,
      persistLauncher := true,
      persistLauncher in Test := false,
      relativeSourceMaps := true,
      libraryDependencies ++= Dependencies.scalajs.value,
      skip in packageJSDependencies := false
    ) ++ sharedDirectorySettings

  lazy val sharedScalaSettings =
    Seq(
      name := "shared-scala-example",
      libraryDependencies ++= Dependencies.shared.value
    )

  lazy val sharedDirectorySettings = Seq(
    unmanagedSourceDirectories in Compile += new File((file(".") / SharedSrcDir / "src" / "main" / "scala").getCanonicalPath),
    unmanagedSourceDirectories in Test += new File((file(".") / SharedSrcDir / "src" / "test" / "scala").getCanonicalPath),
    unmanagedResourceDirectories in Compile += file(".") / SharedSrcDir / "src" / "main" / "resources",
    unmanagedResourceDirectories in Test += file(".") / SharedSrcDir / "src" / "test" / "resources"
  )

  val copySourceMapsTask = Def.task {
    val scalaFiles = (Seq(sharedScala.base, scalajs.base) ** ("*.scala")).get
    for (scalaFile <- scalaFiles) {
      val target = new File((classDirectory in Compile).value, scalaFile.getPath)
      IO.copyFile(scalaFile, target)
    }
  }
}

object Dependencies {
  val shared = Def.setting(Seq())

  val scalajvm = Def.setting(shared.value ++ Seq(
    "com.vmunier" %% "play-scalajs-sourcemaps" % Versions.playScalajsSourcemaps
  ))

  val scalajs = Def.setting(shared.value ++ Seq(
    "com.kanterov.scalajs" %%% "scalajs-react" % Versions.scalajsReact,
    compilerPlugin("org.scalamacros" % "paradise" % Versions.macroParadise cross CrossVersion.full)
  ))
}

object Versions {
  val app = "0.1.0-SNAPSHOT"
  val scala = "2.11.6"
  val scalajsReact = "0.3.4"
  val macroParadise = "2.0.1"
  val playScalajsSourcemaps = "0.1.0"
}
