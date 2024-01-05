package com.rockthejvm.practice

import cats.effect.IO

import scala.None.toRight
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ErrorHandling {

  // 1 - construct potentially failed IOs from standard data types (Option, Try, Either)
  def option2IO[A](option: Option[A])(ifEmpty: Throwable): IO[A] = {
    option match {
      case Some(value) => IO(value)
      case None => IO.raiseError(ifEmpty)
    }
  }

  def try2IO[A](aTry: Try[A]): IO[A] = {
    aTry match {
      case Success(value) => IO(value)
      case Failure(exception) => IO.raiseError(exception)
    }
  }

  def either2IO[A](anEither: Either[Throwable, A]): IO[A] = {
    anEither match
      case Left(value) => IO.raiseError(value)
      case Right(value) => IO(value)
  }
  import cats.syntax.apply._
  import cats.implicits._
  import scala.concurrent.ExecutionContext.Implicits.global
  def main(args: Array[String]): Unit = {

    val function = (a: Int, b: String, c: Double) => s"$b $a + $c"
    val optionMapNTest = (Option(1), Option("Hello"), Option(4.5)).mapN(function)
    val EitherMapNTest = (Either.right(1), Either.right("Hello"), Either.right(4.5)).mapN(function)
    val FutureMapNTest = (Future(1), Future("Hello"), Future(4.5)).mapN(function)
    val ListMapNTest = (List(1), List("Hello"), List(4.5)).mapN(function)
    println(optionMapNTest)
    println(EitherMapNTest)
    println(FutureMapNTest)
    println(ListMapNTest)
  }
}
