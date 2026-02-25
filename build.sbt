// Default main class for sbt run
Compile / mainClass := Some("com.consultant.api.Server")
import sbt._
import sbt.Keys._

ThisBuild / organization := "com.consultant"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.4.2"

lazy val catsVersion       = "2.10.0"
lazy val catsEffectVersion = "3.5.4"
lazy val http4sVersion     = "0.23.25"
lazy val circeVersion      = "0.14.6"
lazy val doobieVersion     = "1.0.0-RC5"
lazy val tapirVersion      = "1.9.8"
lazy val logbackVersion    = "1.4.14"
lazy val awsVersion        = "2.20.26"
lazy val fs2AwsVersion     = "6.1.0"
lazy val cirisVersion      = "3.5.0"
lazy val redis4catsVersion = "1.5.2"
lazy val jwtVersion        = "10.0.0"
lazy val bcryptVersion     = "1.78.1"
lazy val jbcryptVersion    = "0.4"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ykind-projector"
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core"   % catsVersion,
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "org.scalatest" %% "scalatest"   % "3.2.17" % Test,
    "org.scalamock" %% "scalamock"   % "7.5.4"  % Test
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "consultant-backend"
  )
  .aggregate(core, data, infrastructure, api)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      // jBCrypt for password hashing
      "org.mindrot" % "jbcrypt" % jbcryptVersion
    )
  )

lazy val data = (project in file("data"))
  .settings(commonSettings)
  .settings(
    name := "data",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
      "io.circe"     %% "circe-core"       % circeVersion,
      "io.circe"     %% "circe-generic"    % circeVersion,
      "io.circe"     %% "circe-parser"     % circeVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
      "org.flywaydb"  % "flyway-core"      % "9.22.3"
    ),
    flywayUrl       := sys.env.getOrElse("DB_URL", "jdbc:postgresql://localhost:5432/consultant"),
    flywayUser      := sys.env.getOrElse("DB_USER", "consultant_user"),
    flywayPassword  := sys.env.getOrElse("DB_PASSWORD", "consultant_pass"),
    flywayLocations := Seq("filesystem:data/src/main/resources/db/migration")
  )
  .dependsOn(core)
  .enablePlugins(FlywayPlugin)

lazy val infrastructure = (project in file("infrastructure"))
  .settings(commonSettings)
  .settings(
    name := "infrastructure",
    libraryDependencies ++= Seq(
      // AWS SDK v2
      "software.amazon.awssdk" % "s3"             % awsVersion,
      "software.amazon.awssdk" % "sns"            % awsVersion,
      "software.amazon.awssdk" % "sqs"            % awsVersion,
      "software.amazon.awssdk" % "ses"            % awsVersion,
      "software.amazon.awssdk" % "secretsmanager" % awsVersion,
      // FS2 AWS for reactive streaming
      "io.laserdisc" %% "fs2-aws-s3"  % fs2AwsVersion,
      "io.laserdisc" %% "fs2-aws-sqs" % fs2AwsVersion,
      // Configuration management
      "is.cir" %% "ciris" % cirisVersion,
      // Circe for JSON
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      // Redis for caching
      "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
      "dev.profunktor" %% "redis4cats-streams" % redis4catsVersion,
      // JWT for authentication
      "com.github.jwt-scala" %% "jwt-circe" % jwtVersion,
      // OIDC/JWKS verification
      "com.nimbusds" % "nimbus-jose-jwt" % "9.37.3",
      // BCrypt for password hashing (through BouncyCastle)
      "org.bouncycastle" % "bcprov-jdk18on" % bcryptVersion,
      // jBCrypt for password checking
      "org.mindrot" % "jbcrypt" % jbcryptVersion
    )
  )
  .dependsOn(core)

lazy val api = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name                 := "api",
    Compile / run / fork := true,
    Compile / run / envVars := Map(
      "DB_URL"      -> "jdbc:postgresql://localhost:5432/consultant",
      "DB_USER"     -> "consultant_user",
      "DB_PASSWORD" -> "consultant_pass"
    ),
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-ember-server"     % http4sVersion,
      "org.http4s"                  %% "http4s-ember-client"     % http4sVersion,
      "org.http4s"                  %% "http4s-circe"            % http4sVersion,
      "org.http4s"                  %% "http4s-dsl"              % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "io.circe"                    %% "circe-core"              % circeVersion,
      "io.circe"                    %% "circe-generic"           % circeVersion,
      "io.circe"                    %% "circe-parser"            % circeVersion,
      "ch.qos.logback"               % "logback-classic"         % logbackVersion,
      // Configuration management
      "is.cir" %% "ciris" % cirisVersion,
      // PostgreSQL driver for Flyway runtime
      "org.postgresql" % "postgresql" % "42.7.3"
    ),
    // Assembly settings for Docker
    assembly / assemblyJarName := "consultant-api.jar",
    assembly / mainClass       := Some("com.consultant.api.Server"),
    assembly / assemblyMergeStrategy := {
      // Keep Swagger UI webjars resources - critical for SwaggerUI to work!
      case PathList("META-INF", "resources", "webjars", _*) => MergeStrategy.first
      case PathList("META-INF", "resources", _*)            => MergeStrategy.first
      case PathList("META-INF", "services", _*)             => MergeStrategy.concat // Service loaders (JDBC, etc.)
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "MANIFEST.MF")                        => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        xs.map(_.toLowerCase) match {
          case "index.list" :: Nil | "dependencies" :: Nil => MergeStrategy.discard
          // Discard all signature files that conflict when merging signed JARs
          case s if s.endsWith(".sf") || s.endsWith(".rsa") || s.endsWith(".dsa") => MergeStrategy.discard
          case _                                                                  => MergeStrategy.discard
        }
      case "module-info.class"       => MergeStrategy.discard
      case "application.conf"        => MergeStrategy.concat
      case x if x.endsWith(".proto") => MergeStrategy.first
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .dependsOn(core, data, infrastructure)
