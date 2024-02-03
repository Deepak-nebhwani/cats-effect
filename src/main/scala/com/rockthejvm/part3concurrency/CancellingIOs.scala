package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}
import scala.concurrent.duration._

object CancellingIOs extends IOApp.Simple {

  import com.rockthejvm.utils._

  /** 1. Cancelling IOs
    * there are different ways to cancel IO computation
    *
    * 1.1 - fib.cancel ---> `start` the IO in a different Fiber (fb <- `someIO.start`) and then send cancel signal using it's reference, `fib.cancel`
    * this fib ref. is also effect so wrapped in a IO[Fiber] so it can be chained with other IO computation and can send cancel signal.
    * We need a fiber of and IO computation to send cancel signal
    *
    * 1.2 - IO.race & other APIs, IO.race will send cancel signal to slower fiber
    *
    * 1.3 - manual cancellation (`IO.canceled`) ---> we can cancel a composite computation if we have composed `IO.canceled`,
    * as soon as this step is reached further chained computation will bot be executed.
    */
  val chainOfIOs: IO[Int] = IO("waiting").debug >> IO.canceled >> IO(42).debug

  // uncancelable
  // example: online store, payment processor
  // payment process must NOT be canceled
  val specialPaymentSystem = (
    IO("Payment running, don't cancel me...").debug >>
      IO.sleep(1.second) >>
      IO("Payment completed.").debug
  ).onCancel(IO("MEGA CANCEL OF DOOM!").debug.void)

  val cancellationOfDoom = for {
    fib <- specialPaymentSystem.start
    _ <- IO.sleep(500.millis) >> fib.cancel
    _ <- fib.join
  } yield ()

  /** 2. Mask a IO Computation Un-cancelable
    *
    * `IO.uncancelable(_ => ioComputationToMakeUncancelable)`
    *
    * or `IO.uncancelable(poll => ioComputationToMakeUncancelable)`
    *
    * if you still want to cancel any part of computation just wrap that IO in poll(ioToBeCancelled)
    *
    * Note: in an uncancelable composite computation, if one computation is unmasked by poll,
    * now if composite uncancelable computation means IO receive cancel signal
    * - if unmask by poll IO is not finish yet so it will cancel and all further IOs will not get execute
    * - if unmask by poll IO is not finished successfully so all further IOs can not be cancelled complete execution of composite IO will happen
    */

  val atomicPayment = IO.uncancelable(_ => specialPaymentSystem) // "masking"
  val atomicPayment_v2 = specialPaymentSystem.uncancelable // same

  val noCancellationOfDoom = for {
    fib <- atomicPayment.start
    _ <- IO.sleep(500.millis) >> IO("attempting cancellation...").debug >> fib.cancel
    _ <- fib.join
  } yield ()

  /*
    The uncancelable API is more complex and more general.
    It takes a function from Poll[IO] to IO. In the example above, we aren't using that Poll instance.
    The Poll object can be used to mark sections within the returned effect which CAN BE CANCELED.
   */

  /*
    Example: authentication service. Has two parts:
    - input password, can be cancelled, because otherwise we might block indefinitely on user input
    - verify password, CANNOT be cancelled once it's started
   */
  val inputPassword =
    IO("Input password:").debug >> IO("(typing password)").debug >> IO.sleep(2.seconds) >> IO(
      "RockTheJVM1!")
  val verifyPassword =
    (pw: String) => IO("verifying...").debug >> IO.sleep(2.seconds) >> IO(pw == "RockTheJVM1!")

  val authFlow: IO[Unit] = IO.uncancelable { poll =>
    for {
      pw <-
        poll(inputPassword).onCancel(IO("Authentication timed out. Try again later.").debug.void) // this is cancelable
      verified <- verifyPassword(pw) // this is NOT cancelable
      _ <-
        if (verified) IO("Authentication successful.").debug // this is NOT cancelable
        else IO("Authentication failed.").debug
    } yield ()
  }

  val authProgram = for {
    authFib <- authFlow.start
    _ <- IO.sleep(1.seconds) >> IO(
      "Authentication timeout, attempting cancel...").debug >> authFib.cancel
    _ <- authFib.join
  } yield ()

  /** val uncancelableCompositeComputation = IO.uncancelable { poll =>
    * for {
    * _ <- poll(cancelableIO)
    * _ <- uncancelalbeIO
    * - <- IO("after uncancelable")
    * } yield ()
    * }
    *
    * val sendCancelSignalIO = for {
    * fib <- uncancelableCompositeComputation.start
    * _ <- fib.cancel
    * } yield ()
    *
    * * Note: in an uncancelable composite computation, if one computation is unmasked by poll,
    * now if composite uncancelable computation means IO receive cancel signal
    * - if unmask by poll IO is not finish yet so it will cancel and all further IOs will not get execute
    * - if unmask by poll IO is not finished successfully so all further IOs can not be cancelled complete execution of composite IO will happen
    *
    * Uncancelable calls are MASKS which suppress cancellation.
    * Poll calls are "gaps opened" in the uncancelable region.
    */

  /** Exercises: what do you think the following effects will do?
    * 1. Anticipate
    * 2. Run to see if you're correct
    * 3. Prove your theory
    */
  // 1
  val cancelBeforeMol = IO.canceled >> IO(42).debug
  val uncancelableMol = IO.uncancelable(_ => IO.canceled >> IO(42).debug)
  // uncancelable will eliminate ALL cancel points

  // 2
  val invincibleAuthProgram = for {
    authFib <- IO.uncancelable(_ => authFlow).start
    _ <- IO.sleep(1.seconds) >> IO(
      "Authentication timeout, attempting cancel...").debug >> authFib.cancel
    _ <- authFib.join
  } yield ()

  /** If any IO computation is wrapped in IO.uncancelable (_ => io)
    * if it has internal manual cancel signal  `IO.cancel` in composed IO
    * or any external cancel signal using fiber instance fib.cancel
    *  Neither internal nor external cancel signal has impact, it will not cancel the computation
    *  only one way to cancel it, use poll to unmask any IO computation and if that cancel it will cancel the further computations,
    *  if computation inside poll get succeeded before receiving any cancel signal the whole computation can not be canceled
    *
    * Note: if we double wrap the computation in uncancelable, it will eliminate the impact of poll as well, means now it can not be cancel any how
    * just like above authFlow already an uncancelable computation, but having `poll()` inside it so it can be cancel,
    * but if wrap it in another IO.uncancelable (_ => authFlow) the poll() impact will be eliminated, now it can be cancelled.
    * but we can add the other cancellation gap means poll in second uncancelable block to make it cancelable, that is also possible like reversing the impact of second uncancelable
    */

  /*
    Lesson: Uncancelable calls are masks which suppress all existing cancelable gaps (including from a previous uncancelable).
   */

  // 3
  def threeStepProgram(): IO[Unit] = {
    val sequence = IO.uncancelable { poll =>
      poll(IO("cancelable").debug >> IO.sleep(1.second) >> IO("cancelable end").debug) >>
      IO("uncancelable").debug >> IO.sleep(1.second) >> IO("uncancelable end").debug >>
      poll(IO("second cancelable").debug >> IO.sleep(1.second) >> IO("second cancelable end").debug)
    }

    for {
      fib <- sequence.start
      _ <- IO.sleep(1500.millis) >> IO("CANCELING").debug >> fib.cancel
      _ <- fib.join
    } yield ()
  }

  /** Output of above:
    *
    * [io-compute-7] cancelable    from computation 1 since it has 1 second sleep in between and not received any cancellation signal yet
    * [io-compute-1] cancelable end  from computation 1, not received any cancel signal yet so executed
    * [io-compute-1] uncancelable from computation 2, it has started and gone for 1 sec sleep, but after 0.5 sec it receive cancel signal, since it is in uncancelable block so cancel signal doesn't impact.
    * [io-compute-5] CANCELING from the other computation which sends canceling signal after 1500 mili second
    * [io-compute-5] uncancelable end from computation 2, cancel signal doesn't impact so completed
    *
    * Note: Third computation in poll() could not start, since it has received cancel signal
    */

  /*
    Lesson: Uncancelable regions ignore cancellation signals, but that doesn't mean the next CANCELABLE region won't take them.
   */

  override def run = threeStepProgram()
}
