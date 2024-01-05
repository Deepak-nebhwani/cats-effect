package com.rockthejvm.practice

import cats.effect.{IO, IOApp, Resource}

import scala.language.postfixOps
import java.io.{File, FileReader}
import java.util.Scanner
import scala.concurrent.duration.*
import com.rockthejvm.utils.*
object ResourcePractice extends IOApp.Simple {

  /**
   * 3.1 Exercise: read the file with the bracket pattern
   *  - open a scanner
   *    https://www.codingninjas.com/studio/library/scanner-class-in-java
   *  - read the file line by line, every 100 millis
   *  - close the scanner
   *  - if cancelled/throws error, close the scanner
   */


  def getScanner(path: String): IO[Scanner] = {
    IO(new Scanner(new FileReader(path)))
  }
  def readFileLineByLine(scanner: Scanner): IO[Unit] = {
    if (scanner.hasNextLine) IO(scanner.nextLine()).debug >> IO.sleep(100.millis) >> readFileLineByLine(scanner)
    else IO.unit
  }

  def readFile(path: String) = {
    getScanner(path)
      .bracket(scanner =>
        readFileLineByLine(scanner))(scanner => IO(scanner.close()).void)
  }

  def readFile_v2(path: String) = {
    Resource
      .make(getScanner(path))
      (scanner => IO(scanner.close()).void)
        .use(scanner =>readFileLineByLine(scanner))
  }
  val dynamicResource: Resource[IO, String] = Resource.eval(IO(s"Dynamic resource: ${System.currentTimeMillis()}"))
  val resultEither =
    dynamicResource.attempt.use { result =>
      IO(s"Result: $result")
    }
  override def run: IO[Unit] = readFile_v2("/Users/deepak.nebhwani/gitRepo/cats-effect/src/main/scala/com/rockthejvm/practice/ResourcePractice.scala")
}
