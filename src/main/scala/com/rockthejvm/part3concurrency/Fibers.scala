package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Fiber, IO, IOApp, Outcome}
import scala.concurrent.duration._

object Fibers extends IOApp.Simple {

  /**
   * Read below reference to get full idea
   * Ref: https://blog.rockthejvm.com/cats-effect-fibers/
   */

  val meaningOfLife = IO.pure(42)
  val favLang = IO.pure("Scala")

  import com.rockthejvm.utils._

  def sameThreadIOs() = for {
    _ <- meaningOfLife.debug
    _ <- favLang.debug
  } yield ()

  /**
   * 1.  introducing Fiber: a data structure describing an effect running on some thread
   */

  /**
   * 2. Fiber Signature
   * A Fiber takes 3 type arguments: the “effect type”, itself generic (usually IO),
   * the type of error it might fail with and the type of result it might return if successful.
   *
   * It is almost impossible to create fibers manually.
   */
  def createFiber: Fiber[IO, Throwable, String] = ???

  /**
   * 3. IO start method to create Fiber
   * We generally create Fiber using `start` method of IOs,
   *
   * The start method should spawn a Fiber.
   * But since creating the fiber itself — and running the IO on a separate thread — is an effect,
   * hence the fiber is not actually started and the returned fiber is wrapped in another IO instance `IO[Fiber[IO, Throwable, Int]]`
   */

  val aFiber: IO[Fiber[IO, Throwable, Int]] = meaningOfLife.debug.start

  /**
   * 4. Fiber on different Thread
   * Fiber computations run on a different thread.
   * and in the below for-comprehension it will run both IOs in different thread
   *
   * if we have multiple IO computations chained together and one IO is fiber
   * then only fiber IO will be executed in diff thread, all other will go in the same current thread
   *
   * for example:
   * val execute = for{
   * result <- io1.start <* IO.sleep(1 second).debug
   * _ <-  mol
   * _ <- IO("after error").debug
   * } yield result
   *
   * Output:
   * [io-compute-1] first IO <--- this is from fiber
   * [io-compute-2] () <--- from sleep
   * [io-compute-2] 42 <--- from mol
   * [io-compute-2] after error <---- from the last one string
   */
  def differentThreadIOs() = for {
    _ <- aFiber
    _ <- favLang.debug
  } yield ()

  /**
   * 5. joining a fiber
   * in above for-yield; fiber will also execute but doesn't give the result, if fiber is failing it will not fail the whole computation.
   * if we want to work with the result of fiber we will have to `join` or `cancel` the fiber
   *
   * fiber join Awaits the completion of the fiber and returns its Outcome once it completes.
   * It is similar to `Future.await` blocking operation which will pause the current thread until thread where the future is running not getting completed.
   *
   * other fibers like current thread will wait until fiber instance on which join is called finish its execution and provide the result
   * (next statement of current thread will halt until this joining fiber is not finishing)
   * after completion of execution  current thread where other computation are happening will start execution
   *
   */


  def runOnSomeOtherThread[A](io: IO[A]): IO[Outcome[IO, Throwable, A]] = for {
    fib <- io.start
    result <- fib.join // an effect which waits for the fiber to terminate
  } yield result

  /**
   * 6. Fiber Outcomes (Results (Outcome) of a fiber)
   * the result can be one of three types: `Succeeded`, `Errored`, or `Canceled`.
   * When a fiber result is joined, its result is of type `Outcome[IO, Throwable, A]`.
   * If a fiber completes successfully, the outcome will be of type `Succeeded`.
   * If the fiber execution resulted in a failure, it will have the type `Errored`.
   * On fiber cancelation, the outcome will be of type `Canceled`.
   * Therefore, we can pattern-match on the outcome result to handle different scenarios:
   */


  val someIOOnAnotherThread = runOnSomeOtherThread(meaningOfLife)

  /**
   * 7. Pattern Match on Fiber outcomes after Join
   * possible outcomes:
   * - success with an IO
   * - failure with an exception
   * - cancelled
   */

  val someResultFromAnotherThread = someIOOnAnotherThread.flatMap {
    case Succeeded(effect) => effect
    case Errored(e) => IO(0)
    case Canceled() => IO(0)
  }

  /**
   * [[Fiber!.join join]] semantically blocks the joining fiber until the joinee fiber terminates,
   * after which the [[Outcome]] of the joinee is returned. [[Fiber!.cancel cancel]] requests a fiber to abnormally terminate,
   * and semantically blocks the canceller until the cancellee has completed finalization.
   */

  /**
   * 8. Fiber which raise exception
   * In composed IO computation; If a computation running on diff fiber and getting failed, it will not fail the complete execution.
   * other IO's will be computed normally.
   *
   * We can see the failed result using Outcomes pattern match after joining the fiber.
   *
   * if any IO from current fiber fail will fail the complete execution of composed IO
   *
   * for example:
   *
   * val fib = IO.raiseError(new RuntimeException("failure"))
   * val execute = for {
   * result <- io1.start <* IO.sleep(1 second).debug
   * erredFib <- fib.start.debug
   * failedJoinedResult <- erredFib.join    <--- will not fail complete execution
   * _ <- IO("after error").debug
   * //    _ <- IO.raiseError(new RuntimeException("ex"))    <---- this will fail the complete execution
   * } yield failedJoinedResult
   *
   * // transformation of Failed fiber result
   *
   * val transformedFiberResult = execute.flatMap {
   * case Succeeded(result) => result
   * case Outcome.Errored(e) => IO(e)
   * case Outcome.Canceled() => IO("canceled")
   * }
   *
   * override def run =   transformedFiberResult.flatMap(IO.println)
   *
   * //OutPut:
   * //[io-compute-9] first IO
   * //[io-compute-3] ()
   * //[io-compute-3] false
   * //[io-compute-3] after error
   * //java.lang.RuntimeException: failure
   *
   */
  def throwOnAnotherThread() = for {
    fib <- IO.raiseError[Int](new RuntimeException("no number for you")).start
    result <- fib.join
  } yield result


  /**
   *
   * @return
   */
  def testCancel() = {
    val task = IO("starting").debug >> IO.sleep(1.second) >> IO("done").debug
    // onCancel is a "finalizer", allowing you to free up resources in case you get canceled
    val taskWithCancellationHandler = task.onCancel(IO("I'm being cancelled!").debug.void)

    for {
      fib <- taskWithCancellationHandler.start // on a separate thread
      _ <- IO.sleep(500.millis) >> IO("cancelling").debug // running on the calling thread
      _ <- fib.cancel
      result <- fib.join
    } yield result
  }


  /**
   * Exercises:
   *  1. Write a function that runs an IO on another thread, and, depending on the result of the fiber
   *    - return the result in an IO
   *    - if errored or cancelled, return a failed IO
   *
   *  2. Write a function that takes two IOs, runs them on different fibers and returns an IO with a tuple containing both results.
   *    - if both IOs complete successfully, tuple their results
   *    - if the first IO returns an error, raise that error (ignoring the second IO's result/error)
   *    - if the first IO doesn't error but second IO returns an error, raise that error
   *    - if one (or both) canceled, raise a RuntimeException
   *
   *  3. Write a function that adds a timeout to an IO:
   *    - IO runs on a fiber
   *    - if the timeout duration passes, then the fiber is canceled
   *    - the method returns an IO[A] which contains
   *      - the original value if the computation is successful before the timeout signal
   *      - the exception if the computation is failed before the timeout signal
   *      - a RuntimeException if it times out (i.e. cancelled by the timeout)
   *
   *      HINT: An IO which will sleep for timeout duration
   *      and can send cancel signal to the IO for which we want to add this timeout feature
   */
  // Exercise 1
  def processResultsFromFiber[A](io: IO[A]): IO[A] = {
    val ioResult = for {
      fib <- io.debug.start
      result <- fib.join
    } yield result

    ioResult.flatMap {
      case Succeeded(fa) => fa
      case Errored(e) => IO.raiseError(e)
      case Canceled() => IO.raiseError(new RuntimeException("Computation canceled."))
    }
  }

  def testEx1() = {
    val aComputation = IO("starting").debug >> IO.sleep(1.second).debug >> IO("done!").debug >> IO(42)

    /**
     * Note we can pass any IO composite computation as well and it complete chained composite IO will execute in separate thread .
     *
     * Any Blocking operation like IO.sleep or fiber.join, instantly de-schedule the fiber thread and yield the control of thread to the thread pool,
     * it looks like Thread is blocked for blocking fiber but not the actual thread is blocked or wasted only fiber is de-scheduled
     * and will acquire the required thread from thread pool when it restart it's execution.
     *
     */

    /**
     * Output of this computation
     * only first will go in separate thread always, and all other three goes in same thread.
     * [io-compute-3] starting
     * [io-compute-8] ()
     * [io-compute-8] done!
     * [io-compute-8] 42
     */

    processResultsFromFiber(aComputation).void
  }

  // 2
  def tupleIOs[A, B](ioa: IO[A], iob: IO[B]): IO[(A, B)] = {
    val result = for {
      fiba <- ioa.start
      fibb <- iob.start
      resulta <- fiba.join
      resultb <- fibb.join
    } yield (resulta, resultb)

    result.flatMap {
      case (Succeeded(fa), Succeeded(fb)) => for {
        a <- fa
        b <- fb
      } yield (a, b)
      case (Errored(e), _) => IO.raiseError(e)
      case (_, Errored(e)) => IO.raiseError(e)
      case _ => IO.raiseError(new RuntimeException("Some computation canceled."))
    }
  }

  def testEx2() = {
    val firstIO = IO.sleep(2.seconds).debug >> IO(1).debug
    val secondIO = IO.sleep(3.seconds).debug >> IO(2).debug
    tupleIOs(firstIO, secondIO).debug.void

    /**
     * Output:
     * [io-compute-0] ()
     * [io-compute-0] 1
     * [io-compute-5] ()
     * [io-compute-5] 2
     * [io-compute-5] (1,2)
     */
  }

  // 3
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] = {
    val computation = for {
      fib <- io.start  // it will go in separate IO
      _ <- (IO.sleep(duration) >> fib.cancel).start // careful - fibers can leak
      result <- fib.join
    } yield result

    computation.flatMap {
      case Succeeded(fa) => fa
      case Errored(e) => IO.raiseError(e)
      case Canceled() => IO.raiseError(new RuntimeException("Computation canceled."))
    }
  }

  def testEx3() = {
    val aComputation = IO("starting").debug >> IO.sleep(1.second) >> IO("done!").debug >> IO(42)
    timeout(aComputation, 500.millis).debug.void
  }

  override def run = testEx3()
}
