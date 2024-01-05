package com.rockthejvm.part2effects

import cats.Parallel
import cats.effect.{IO, IOApp}

object IOParallelism extends IOApp.Simple {
  /**
   *
   * 1. IOs are usually sequential
   * 2. if we chain IO computations it will be executing in a same Thread which can be seen by Thread.currentThread().getName
   *
   */
  val aniIO = IO(s"[${Thread.currentThread().getName}] Ani")
  val kamranIO = IO(s"[${Thread.currentThread().getName}] Kamran")

  val composedIO = for {
    ani <- aniIO
    kamran <- kamranIO
  } yield s"$ani and $kamran love Rock the JVM"

  /**
   * 3. debug extension method which will be printing the IO's execution Thread using Thread.currentThread().getName
   */

  import com.rockthejvm.utils._

  /**
   * 4. mapN extension method is from the below import
   * import cats.syntax.apply._
   *
   */

  import cats.syntax.apply._
  val meaningOfLife: IO[Int] = IO.delay(42)
  val favLang: IO[String] = IO.delay("Scala")
  val goalInLife = (meaningOfLife.debug, favLang.debug).mapN((num, string) => s"my goal in life is $num and $string")

  /**
   * 5. parallelism on IOs
   * convert a sequential IO to parallel IO using `Parallel[IO].parallel(ioToBeParallelized)`
   * now ioToBeParallelized computation will be executed in separate Thread
   * It is from
   * import cats.Parallel
   */

  //
  val parIO1: IO.Par[Int] = Parallel[IO].parallel(meaningOfLife.debug)
  val parIO2: IO.Par[String] = Parallel[IO].parallel(favLang.debug)
  import cats.effect.implicits._
  val goalInLifeParallel: IO.Par[String] = (parIO1:IO.Par[Int], parIO2:IO.Par[String]).mapN((num, string) => s"my goal in life is $num and $string")

  /**
   * 6. convert  Parallel IO back in to sequential IO
   * `Parallel[IO].sequential(parallelIOToBeSequential)`
   *
   */

  val goalInLife_v2: IO[String] = Parallel[IO].sequential(goalInLifeParallel)

  // shorthand:
  /**
   * 7. the below is the most common way to run IO computation in different threads
   * if we want to execute IO's computations in parallel, just wrap those in a tuple and call parMapN() on tuple, 
   * it takes a function which takes results of those IOs as argument, and we can perform the transformation over the result of those.
   * parMapN----> it will take a transformFunction: (Int, String) => String
   * output will be wrapped in IO[String] not IO.Par[String] but computation will be in separate Threads
   * IO operations will be executed in separate threads.
   *
   */

  import cats.syntax.parallel._
  def transformFunction(num: Int, string: String): String = s"my goal in life is $num and $string"
  val goalInLife_v3: IO[String] = (meaningOfLife.debug, favLang.debug).parMapN((num, string) => s"my goal in life is $num and $string")
  val goalInLife_v4: IO[String] = (meaningOfLife.debug, favLang.debug).parMapN(transformFunction)

  // 
  /**
   * regarding failure in parallel IO:
   * whatever computations are running in parallel, 
   * if any one is getting failed it will stop execution of all pending computation,
   * it will show the results of all completed executions before any failed computation. 
   * and whosoever is failing first in multiple failed computations will be giving the final failed result.
   * 
   */
  val aFailure: IO[String] = IO.raiseError(new RuntimeException("I can't do this!"))
  // compose success + failure
  val parallelWithFailure = (IO(Thread.sleep(1000)) >> meaningOfLife.debug, aFailure.debug).parMapN((num, string) => s"$num $string")
  val parallelWithFailure_2 = (aFailure.debug, IO(Thread.sleep(1000)) >> meaningOfLife.debug).parMapN((string, num) => s"$string $num ")

  // compose failure + failure
  val anotherFailure: IO[String] = IO.raiseError(new RuntimeException("Second failure"))
  val twoFailures: IO[String] = (aFailure.debug, anotherFailure.debug).parMapN(_ + _)
  // the first effect to fail gives the failure of the result
  val twoFailuresDelayed: IO[String] = (IO(Thread.sleep(1000)) >> aFailure.debug, anotherFailure.debug).parMapN(_ + _)


  override def run: IO[Unit] =
    twoFailuresDelayed.debug.void
}
