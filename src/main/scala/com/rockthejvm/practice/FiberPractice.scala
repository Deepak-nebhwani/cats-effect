package com.rockthejvm.practice

import cats.effect.kernel.Outcome
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{FiberIO, IO, IOApp}
import cats.implicits.catsSyntaxFlatMapOps
import com.rockthejvm.part3concurrency.Resources.Connection
import com.rockthejvm.utils.*

import scala.concurrent.duration.*
import scala.language.postfixOps
object FiberPractice extends IOApp.Simple {

  val io1 = IO("first IO").debug
  val mol = IO(42).debug

  val fib = IO.raiseError(new RuntimeException("failure"))
  val execute = for {
    result <- io1.start <* IO.sleep(1 second).debug
    erredFib <- fib.start.debug
    failedJoinedResult <- erredFib.join
    _ <- IO("after error").debug
//    _ <- IO.raiseError(new RuntimeException("ex"))
  } yield failedJoinedResult

  val transformedFiberResult = execute.flatMap {
    case Succeeded(result)  => result
    case Outcome.Errored(e) => IO(e)
    case Outcome.Canceled() => IO("canceled")
  }
  def runIoOnAnotherThread[A](io: IO[A]) = {
    io.start
  }

  def runTwoIoFibers[A, B](io1: IO[A], io2: IO[B]): IO[(A, B)] = {
    val result = for {
      fb <- io1.start
      fb2 <- io2.start
      r1 <- fb.join
      r2 <- fb2.join
    } yield (r1, r2)
    result.flatMap {
      case (Succeeded(v1), Succeeded(v2))    => v1.flatMap(v => v2.map((v, _)))
      case (Errored(e1), _)                  => IO.raiseError(e1)
      case (_, Errored(e2))                  => IO.raiseError(e2)
      case (Canceled(), _) | (_, Canceled()) => IO.raiseError(new RuntimeException("Cancelled"))
    }
  }

  def bracketTestWithFailure = IO(new Connection("test/url/string")).bracket(conn =>
    for {
      con <- conn.open()
      result <- IO.raiseError[String](new RuntimeException("a proper fail"))
      after <- IO("after exception")
    } yield con)(conn => conn.close().void)
    .handleErrorWith {
      case x: RuntimeException => IO("I am failed")
    }

  override def run =
//   bracketTest.flatMap(IO.println)
    bracketTestWithFailure.void
}
