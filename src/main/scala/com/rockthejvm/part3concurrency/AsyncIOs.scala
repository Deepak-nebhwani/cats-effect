package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import com.rockthejvm.utils._

object AsyncIOs extends IOApp.Simple {

  /**
   * Async Ref: https://typelevel.org/cats-effect/docs/typeclasses/async
   */
  // IOs can run asynchronously on fibers, without having to manually manage the fiber lifecycle
  /** Async-  usually means, you start some computations and receive some callbacks which evaluate later when that computation succeed.
    * and this computation can run on diff thread pool
    */

  /** callback is just a function, which will be passed as an argument and will be called after a computation is complete,
    * in scala we can create a type allies for a lambda function.
    */
  val threadPool = Executors.newFixedThreadPool(8)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)
  // Type Allies
  type Callback[A] = Either[Throwable, A] => Unit

  /**
   *
    */

  def computeMeaningOfLife(): Int = {
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] computing the meaning of life on some other thread...")
    42
  }

  def computeMeaningOfLifeEither(): Either[Throwable, Int] = Try {
    computeMeaningOfLife()
  }.toEither

  /** below `execute` method is from Java concurrent executors, which take `Runnable` instance,
    * Runnable is functional interface and we can create zero argument lambda function for Runnable run method which takes nothing and return unit
    *
    * we are running `computeMeaningOfLife` in java thread (java thread pool).
    */
  def computeMolOnThreadPool(): Unit =
    threadPool.execute(() => computeMeaningOfLife())

  /** Problem:
    * the problem with above function is, it is returning unit, and it is eager as well, as soon as we call computeMolOnThreadPool() it will schedule the thread for execution
    * means doing some kind of side effects and we are not getting any return results of the computation.
    */

  /** Solution:
    * `IO.async_[A](k: (Either[Throwable, A] => Unit) => Unit): IO[A]`
    *
    * similar to `IO.async_[A](k: Callback[Int] => Unit): IO[A]`
    *
    * where callback is
    * `type Callback[A] = Either[Throwable, A] => Unit`
    *
    * Suspends an asynchronous side effect in `IO`.
    */

  // lift computation to an IO
  // async is a FFI
  val asyncMolIO: IO[Int] = IO.async_ { (cb: Callback[Int]) => // CE thread blocks (semantically) until this cb is invoked (by some other thread)
    threadPool.execute { () => // computation not managed by CE, it is java thread pool and execute is from java, and it is Runnable lambda execution.
      val result = computeMeaningOfLifeEither()
      cb(result) // CE thread is notified with the result, here in java thread we are calling callback function, it will get execute in CE fiber threads
    }
  }

  /** Exercise: lift an async computation on ec to an IO.
    */
  def asyncToIO[A](computation: () => A)(ec: ExecutionContext): IO[A] =
    IO.async_[A] { (cb: Callback[A]) =>
      ec.execute { () =>
        val result = Try(computation()).toEither
        cb(result)
      }
    }

  val asyncMolIO_v2: IO[Int] = asyncToIO(computeMeaningOfLife)(ec)

  /**
   *
   * Future is similar to ec in above it will run the computation in different pool and pass the result in callback
   * which will be called by CE pool
   *
   * One catch here, we have called cb(result) at the last, means after completion of complete work by future thread
   * we can call in between as well instead of last line  means Future is running and we call cb(result) which will notify CE thread to start execution
   * and now both threads future and CE are working in parallel
   */
  def asyncToIO_withFuture[A](computation: () => A)(ec2: ExecutionContext): IO[A] = {
    IO.async_[A] { (cb: Callback[A]) =>
      Future {
        println("execution started")
        val result = Try(computation()).toEither
        cb(result)
      } (ec2)
    }
  }

    val asyncMolIO_withFuture: IO[Int] = asyncToIO_withFuture(computeMeaningOfLife)(ec)

  /** Exercise: lift an async computation as a Future, to an IO.
   * here once future is completed after that we are calling cb(result)
    */
  def convertFutureToIO[A](future: => Future[A]): IO[A] =
    IO.async_ { (cb: Callback[A]) =>
      future.onComplete { tryResult =>
        val result = tryResult.toEither
        cb(result)
      }
    }

  lazy val molFuture: Future[Int] = Future { computeMeaningOfLife() }
  val asyncMolIO_v3: IO[Int] = convertFutureToIO(molFuture)
  val asyncMolIO_v4: IO[Int] = IO.fromFuture(IO(molFuture))

  /**  Exercise: a never-ending IO?
    */
  val neverEndingIO: IO[Int] = IO.async_[Int](_ => ()) // no callback, no finish
  val neverEndingIO_v2: IO[Int] = IO.never

  import scala.concurrent.duration._

  /*
    FULL ASYNC Call
   */
  def demoAsyncCancellation() = {
    val asyncMeaningOfLifeIO_v2: IO[Int] = IO.async { (cb: Callback[Int]) =>
      /*
        finalizer in case computation gets cancelled.
        finalizers are of type IO[Unit]
        not specifying finalizer => Option[IO[Unit]]
        creating option is an effect => IO[Option[IO[Unit]]]
       */
      // return IO[Option[IO[Unit]]]
      IO {
        threadPool.execute { () =>
          val result = computeMeaningOfLifeEither()
          cb(result)
        }
      }.as(Some(IO("Cancelled!").debug.void))
    }

    for {
      fib <- asyncMeaningOfLifeIO_v2.start
      _ <- IO.sleep(500.millis) >> IO("cancelling...").debug >> fib.cancel
      _ <- fib.join
    } yield ()
  }

  override def run =
//    asyncMolIO_withFuture.debug >> IO(threadPool.shutdown())
    demoAsyncCancellation().debug >> IO(threadPool.shutdown())
}
