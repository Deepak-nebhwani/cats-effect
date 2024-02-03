package com.rockthejvm.practice

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, IOApp, Resource}

import scala.language.postfixOps
import java.io.{File, FileReader}
import java.util.Scanner
import scala.concurrent.duration.*
import com.rockthejvm.utils.*
object RacePractice extends IOApp.Simple {

  /** Exercises:
    * 1 - implement a timeout pattern with race
    * 2 - a method to return a LOSING effect from a race (hint: use racePair)
    * 3 - implement race in terms of racePair
    */

  def timeOut[A](ioa: IO[A], duration: FiniteDuration): IO[Either[A, Unit]] = {
    ioa.onCancel(IO.raiseError(new RuntimeException(s"IO has been timeout after $duration"))).race(
      IO.sleep(duration))
  }

  def testTimeOut = timeOut(IO.sleep(489 millis) >> IO.println("Hello"), 500.millis)

  def lateIO[A, B](io1: IO[A], io2: IO[B]) = {
    io1.racePair(io2) flatMap {
      case Left(value)  => value._2.join
      case Right(value) => value._1.join
    } flatMap {
      case Succeeded(fa) => fa
      case Errored(e)    => IO.raiseError(e)
      case Canceled()    => IO.raiseError(new RuntimeException("cancelled"))
    }
  }

  /*def lateIO_v2[A, B](io1: IO[A], io2: IO[B]) = {
    for {
      racing <- io1.racePair(io2)
      lateIO = racing match {
        case Left(value) => value._2.join
        case Right(value) => value._1.join
      }
      outCome <- lateIO
      result = outCome match {
        case Succeeded(fa) => fa
        case Errored(e:Throwable) => IO.raiseError(e)
        case Canceled() => IO.raiseError(new RuntimeException("cancelled"))
      }
      resultvalue <- result
    } yield resultvalue



  }*/
  def testLateIO =
    lateIO(IO.sleep(800 millis) >> IO("IO1"), IO.sleep(700 millis) >> IO("IO2")).debug

  def race_v2[A, B](io1: IO[A], io2: IO[B]) = {
    io1.racePair(io2) flatMap {
      case Left(value)  => value._2.cancel >> IO(value._1)
      case Right(value) => value._1.cancel >> IO(value._2)
    } flatMap {
      case Succeeded(fa) => fa
      case Errored(e)    => IO.raiseError(e)
      case Canceled()    => IO.raiseError(new RuntimeException("cancelled"))
    }
  }

  def testRace_v2(duration: FiniteDuration) = race_v2(
    (IO.sleep(459 millis) >> IO.println("Hello")).onCancel(IO.raiseError(
      new RuntimeException(s"IO has been timeout after $duration"))),
    IO.sleep(duration))

//  override def run = .void
  override def run = testRace_v2(100.millis).void

}
