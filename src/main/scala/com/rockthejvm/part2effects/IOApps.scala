package com.rockthejvm.part2effects

import cats.effect.{ExitCode, IO, IOApp}

import scala.io.StdIn

object IOApps {
  val program = for {
    line <- IO(StdIn.readLine())
    _ <- IO(println(s"You've just written: $line"))
  } yield ()
}

object TestApp {
  import IOApps._

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global
    program.unsafeRunSync()
  }
}

/**
 * IOApp is trait which provide run method to be implemented and will be a starting point of an application
 */

/**
 * A simple App which override run method that takes List of runtime arguments and return IO[ExitCode]
 * there are two ExitCode, one is success and second is error
 */
object FirstCEApp extends IOApp {
  import IOApps._

  override def run(args: List[String]) =
    program.as(ExitCode.Success)

}

/**
 * A simple App which override run method that doesn't take any argument and return IO[Unit]
 * our program also returning IO[Unit]
 * just use .void function on any IO it will return the IO[Unit]
 * 
 */
object MySimpleApp extends IOApp.Simple {
  import IOApps._

  override def run = program
}

