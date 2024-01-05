package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Fiber, IO, IOApp}

import scala.concurrent.duration.{FiniteDuration, *}

object RacingIOs extends IOApp.Simple {

  import com.rockthejvm.utils.*

  def runWithSleep[A](value: A, duration: FiniteDuration): IO[A] =
    (
      IO(s"starting computation: $value").debug >>
      IO.sleep(duration) >>
      IO(s"computation for $value: done") >>
      IO(value)
    ).onCancel(IO(s"computation CANCELED for $value").debug.void)

  def testRace() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val first: IO[Either[Int, String]] = IO.race(meaningOfLife, favLang)
    /**
     * 1. `IO.race(io1, io2)` or `io1.race(io2)`
     *
      - both IOs run on separate fibers
      - the first one to finish will complete the result
      - the loser will get cancel signal, it will be canceled.
      - return type will be `Either[io1ResultType, io2ResultType]`
     */

    first.flatMap {
      case Left(mol) => IO(s"Meaning of life won: $mol")
      case Right(lang) => IO(s"Fav language won: $lang")
    }
  }

  /**
   * 2. `IO.racePair(io1, io2)` or `io1.racePair(io2)`
   * - both IOs run on separate fibers
   * Return Type contain Result type `Either[(io1Outcomes, io2Fiber),(io1Fiber, io2Outcomes)]`
   * It will not cancel the loser fiber, so we have more control over the loser fiber,
   * so we can `join` and block the slower fiber to wait until it finish with result outcomes,
   * even if we want we can send the cancel signal to the slower fiber to cancel it's operation.
   * faster fiber will results first in form of outcomes, so we can check whether result of faster IO is succeeded, errored or cancelled
   *
   *
   */
  def testRacePair() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val raceResult: IO[Either[
      (Outcome[IO, Throwable, Int], Fiber[IO, Throwable, String]), // (winner result, loser fiber)
      (Fiber[IO, Throwable, Int], Outcome[IO, Throwable, String])  // (loser fiber, winner result)
    ]] = IO.racePair(meaningOfLife, favLang)

    raceResult.flatMap {
      case Left((outMol, fibLang)) => fibLang.cancel >> IO("MOL won").debug >> IO(outMol).debug
      case Right((fibMol, outLang)) => fibMol.cancel >> IO("Language won").debug >> IO(outLang).debug
    }
  }

  /**
   * Exercises:
   * 1 - implement a timeout pattern with race
   * 2 - a method to return a LOSING effect from a race (hint: use racePair)
   * 3 - implement race in terms of racePair
   */
  // 1
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] = {
    val timeoutEffect = IO.sleep(duration)
    val result = IO.race(io, timeoutEffect)

    result.flatMap {
      case Left(v) => IO(v)
      case Right(_) => IO.raiseError(new RuntimeException("Computation timed out."))
    }
  }

  def timeOut_v2[A](ioa: IO[A], duration: FiniteDuration): IO[Either[A, Unit]] = {
    ioa
      .onCancel(IO.raiseError(new RuntimeException(s"IO has been timeout after $duration")))
      .race(IO.sleep(duration))
  }


  val importantTask = IO.sleep(2.seconds) >> IO(42).debug
  val testTimeout = timeOut_v2(importantTask, 1.seconds)
  val testTimeout_v2 = importantTask.timeout(1.seconds)

  // 2
  def unrace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((_, fibB)) => fibB.join.flatMap {
        case Succeeded(resultEffect) => resultEffect.map(result => Right(result))
        case Errored(e) => IO.raiseError(e)
        case Canceled() => IO.raiseError(new RuntimeException("Loser canceled."))
      }
      case Right((fibA, _)) => fibA.join.flatMap {
        case Succeeded(resultEffect) => resultEffect.map(result => Left(result))
        case Errored(e) => IO.raiseError(e)
        case Canceled() => IO.raiseError(new RuntimeException("Loser canceled."))
      }
    }

  // 3
  def simpleRace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((outA, fibB)) => outA match {
        case Succeeded(effectA) => fibB.cancel >> effectA.map(a => Left(a))
        case Errored(e) => fibB.cancel >> IO.raiseError(e)
        case Canceled() => fibB.join.flatMap {
          case Succeeded(effectB) => effectB.map(b => Right(b))
          case Errored(e) => IO.raiseError(e)
          case Canceled() => IO.raiseError(new RuntimeException("Both computations canceled."))
        }
      }
      case Right((fibA, outB)) => outB match {
        case Succeeded(effectB) => fibA.cancel >> effectB.map(b => Right(b))
        case Errored(e) => fibA.cancel >> IO.raiseError(e)
        case Canceled() => fibA.join.flatMap {
          case Succeeded(effectA) => effectA.map(a => Left(a))
          case Errored(e) => IO.raiseError(e)
          case Canceled() => IO.raiseError(new RuntimeException("Both computations canceled."))
        }
      }
    }

  override def run = testTimeout.debug.void
}
