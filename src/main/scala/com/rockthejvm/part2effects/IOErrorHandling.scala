package com.rockthejvm.part2effects

import cats.effect.IO

import scala.util.{Failure, Success, Try}

object IOErrorHandling {

  // IO: pure, delay, defer
  /**
   * In cats-effect exception are treated as values
   * and wrapped in IO to create failed computations,
   * will be executed once we call unsafeRunSync
   */


  /**
   * 1. create failed effects
   * Below both are same but the second one io standard and expected
   */
  val aFailedCompute: IO[Int] = IO.delay(throw new RuntimeException("A FAILURE"))
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper fail"))

  /**
   * 2. handle exceptions
   */
  /**
   * 2.1 chain IO with handleErrorWith, with multiple exception cases and return the handled value after handling exception case
   */
  val dealWithIt = aFailure.handleErrorWith {
    case _: RuntimeException => IO.delay(println("I'm still here"))
    // add more cases
  }

  /**
   *  2.2 turn into an Either using attempt
    */

  val effectAsEither: IO[Either[Throwable, Int]] = aFailure.attempt
  //
  /**
   * 2.3 redeem[A]: transform the failure and the success in one go, it is a short hand for `attempt.map`
   * recover: Throwable to String or any Type A
   * bind: transformer function like map , IO's value to String or any Type A
   */
  val resultAsString_v1 = aFailure.attempt.map {
    case Left(throwable) => s"FAIL: $throwable"
    case Right(value) => s"SUCCESS: $value"
  }

  val resultAsString: IO[String] = aFailure.redeem(ex => s"FAIL: $ex", value => s"SUCCESS: $value")

  /**
   *  2.4 `redeemWith` when we  exception handler computation (function) and value transformation (transformer function) are also some effect-ful operation, means returning IO
   * recover: Throwable to IO[String] or any Type IO[A]
   * bind: transformer function like map , IO's value to IO[String] or any Type IO[A]
   */

  val resultAsEffect: IO[Unit] = aFailure.redeemWith(ex => IO(println(s"FAIL: $ex")), value => IO(println(s"SUCCESS: $value")))

  /**
   * Exercises Note: All below custom methods are already available in IO standard class
   * like fromFuture, fromTry, fromEither etc.
   */
  // 1 - construct potentially failed IOs from standard data types (Option, Try, Either)

  /**
   *
   * in the below all we have used eager IO.pure instead of lazy IO.delay or only IO, 
   * because for standard data-structure like Option, Try, Future; it is eagerly executed and already available as value.
   */

  def option2IO[A](option: Option[A])(ifEmpty: Throwable): IO[A] =
    option match {
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(ifEmpty)
    }

  def try2IO[A](aTry: Try[A]): IO[A] =
    aTry match {
      case Success(value) => IO.pure(value)
      case Failure(ex) => IO.raiseError(ex)
    }

  def either2IO[A](anEither: Either[Throwable, A]): IO[A] =
    anEither match {
      case Left(ex) => IO.raiseError(ex)
      case Right(value) => IO.pure(value)
    }

  // 2 - handleError, handleErrorWith
  def handleIOError[A](io: IO[A])(handler: Throwable => A): IO[A] =
    io.redeem(handler, identity)

  def handleIOErrorWith[A](io: IO[A])(handler: Throwable => IO[A]): IO[A] =
    io.redeemWith(handler, IO.pure)

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global
    resultAsEffect.unsafeRunSync()
  }
}
