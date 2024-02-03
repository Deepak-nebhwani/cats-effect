package com.rockthejvm.practice

import cats.effect.{IO, IOApp, Resource}
import cats.effect.unsafe.IORuntime

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Try
import com.rockthejvm.utils.*
object AsyncPractics extends IOApp.Simple {

  val executors = Executors.newFixedThreadPool(5)
//  val ec =
//    Resource.make(IO(ExecutionContext.fromExecutorService(executors)))(ec => IO(ec.shutdown()))
val ec = ExecutionContext.fromExecutor(executors)

  val mol = 42
//  def asyncFutureIO[A](computation: () => A) = IO.async_[A] { cb =>
//    ec.use(ec =>
//      IO.fromFuture(IO(Future {
//        val result = Try(computation()).toEither
//        cb(result)
//      } { ec }).debug)) >> IO.unit
//  }

  def asyncFutureIO[A](computation: () => A) = IO.async_[A] { cb =>
 
      Future {
        val result = Try(computation()).toEither
        cb(result)
      } { ec }
  }

  override def run: IO[Unit] = asyncFutureIO(() => mol).debug >> IO.println("Done")
}
