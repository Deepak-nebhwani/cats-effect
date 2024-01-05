package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, IOApp, Resource}

import java.io.{File, FileReader}
import java.util.Scanner
import scala.concurrent.duration._


object Resources extends IOApp.Simple {

  import com.rockthejvm.utils._

  /**
   *  1. use-case: manage a connection lifecycle
    */

  class Connection(url: String) {
    def open(): IO[String] = IO(s"opening connection to $url").debug
    def close(): IO[String] = IO(s"closing connection to $url").debug
  }

  val asyncFetchUrl = for {
    fib <- (new Connection("rockthejvm.com").open() *> IO.sleep((Int.MaxValue).seconds)).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
   * 1.2 problem in above: leaking resources
   * whenever there is a cancellation or error, resource will not close.
   */


  /**
   * 1.3 Custom Solution for the above problem
   * chained `onCancel` finalizer with IO composed operation and in this onCancel method close the connection.
   * Note: we have not used any join to block the thread (Fiber) in below
   */
  val correctAsyncFetchUrl = for {
    conn <- IO(new Connection("rockthejvm.com"))
    fib <- (conn.open() *> IO.sleep((Int.MaxValue).seconds)).onCancel(conn.close().void).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
   * 2. bracket pattern: someIO.bracket(useResourceCb)(releaseResourceCb)
   * bracket is equivalent to try-catches (pure FP)
   * Note: Please read the source code comments of bracket method by cmd + click
   *
   * someIO <---- in this IO instantiation of Resource (Reader, Writer, API endpoint etc.) happen and it will return the instance
   * bracket(useResourceCb)(releaseResourceCb), it's a curring function
   * *. first argument is a function `(use: A => IO[B])` to use the created instance `[A]` to open connection with resource
   * and interacting with resource, it can be assumed as `try` block
   * *. second argument is a function `(release: A => IO[Unit])` to use the created instance `[A]` to close connection
   * and perform any finalizing operation, it can be assumed as `finally` block
   *
   * if we want `catch` kind of implementation as well, we should chain it will
   * handleErrorWith {
   * case x: RuntimeException => IO("I am failed")
   * }
   *
   * NOTE on error handling: in case both the release function and the use function throws, the error raised by release gets signaled.
   *
   * If we want to perform release by checking the outcome of operation we can use `bracketCase[B]`
   * here release function uses two args, one is instance and second is outcomes of use function `(use: A => IO[B])(release: (A, OutcomeIO[B]) => IO[Unit])`
   * In comparison with the simpler bracket version, this one allows the caller to differentiate between normal termination, termination in error and cancellation via an `cats.effect.Outcome` parameter.
   *
   */
  val bracketFetchUrl = IO(new Connection("rockthejvm.com"))
    .bracket(conn => conn.open() *> IO.sleep(Int.MaxValue.seconds))(conn => conn.close().void)


  val bracketProgram = for {
    fib <- bracketFetchUrl.start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  /**
   * 2.2 Que: What is the result after using bracket function, in case use function throw and error
   * Ans: Use function will be failed with the Exception and prior to exception all things will be called and release function will always be called
   */
  def bracketTestWithFailure = IO(new Connection("test/url/string")).bracket(
    conn =>
      for {
        con <- conn.open()
        result <- IO.raiseError[String](new RuntimeException("a proper fail"))
        after <- IO("after exception")
      } yield con
  )(conn =>
    //this release function will always be called.
    conn.close().void)

  /**
   * 2.3 Output of above function,
   *
   * [io-compute-1] opening connection to test/url/string
   * [io-compute-1] closing connection to test/url/string
   * Exception in thread "main" java.lang.RuntimeException: a proper fail
   *
   * no "after exception" is printed and the return type is an exception, which can fail the calling method.
   *
   */

  def bracketTestWithExceptionHandled = IO(new Connection("test/url/string")).bracket(
    conn =>
      for {
        con <- conn.open()
        result <- IO.raiseError[String](new RuntimeException("a proper fail"))
        after <- IO("after exception")
      } yield con
  )(conn => conn.close().void)
    .handleErrorWith {
      case x: RuntimeException => IO("I am failed")
    }

  /**
   * 3.1 Exercise: read the file with the bracket pattern
   *  - open a scanner
   *  Ref: https://www.codingninjas.com/studio/library/scanner-class-in-java
   *  - read the file line by line, every 100 millis
   *  - close the scanner
   *  - if cancelled/throws error, close the scanner
   */
  def openFileScanner(path: String): IO[Scanner] =
    IO(new Scanner(new FileReader(new File(path))))

  /**
   * Note: in the below function we need iteration and instead of `for` or `while` loop,
   * recursion is used because recursion is stack safe with `flatMap` in IO as well as with `>>` operator, `*>` operator is eager and not stack safe
   * remember `>>` operator, whenever any IO computation chaining or composition is needed we frequently use this, instead of for-yield or flatMap
   */

  def readLineByLine(scanner: Scanner): IO[Unit] =
    if (scanner.hasNextLine) IO(scanner.nextLine()).debug >> IO.sleep(100.millis) >> readLineByLine(scanner)
    else IO.unit

  /**
   * in above function, output will be printed due to debug function
   * due to sleep blocking operation, it will use diff fibers due to de-scheduling and re-scheduling of fibers like asynchronous call
   * if sleep is not used all lines will be in single fiber thread.
   */

  /**
   *
   * In functional programming, there's a tendency to avoid the usage of the typical loops that are well-known in imperative languages.
   * The problem with loops is their conditions are usually based on mutable variables.
   * In Scala, it's possible to use while loop just like in imperative languages, e.g. Java.
   */
  def bracketReadFile(path: String): IO[Unit] =
    IO(s"opening file at $path") >>
      openFileScanner(path).bracket { scanner =>
        readLineByLine(scanner)
      } { scanner =>
        IO(s"closing file at $path").debug >> IO(scanner.close())
      }



  /**
   * 4. Problem with bracket pattern (nested resources)
   *
   */
  def connFromConfig(path: String): IO[Unit] =
    openFileScanner(path)
      .bracket { scanner =>
        // acquire a connection based on the file
        IO(new Connection(scanner.nextLine())).bracket { conn =>
          conn.open() >> IO.never
        }(conn => conn.close().void)
      }(scanner => IO("closing file").debug >> IO(scanner.close()))
  // nesting resources are tedious


  /**
   * Resources
   */

  /**
   * 5. Solution of nesting resources is `Resource` data structure
   * which can create a composite `Resource` by chaining the Resources using `flatMap` and `for-yield `
   * Once Resource instance is available we can call `.use` function
   *
   */
  val connectionResource = Resource.make(IO(new Connection("rockthejvm.com")))(conn => conn.close().void)
  // ... at a later part of your code

  val resourceFetchUrl = for {
    fib <- connectionResource.use(conn => conn.open() >> IO.never).start
    _ <- IO.sleep(1.second) >> fib.cancel
  } yield ()

  /**
   * 6. bracket function vs Resource Data structure
   * resources are equivalent to brackets
   *
   * `bracket` is curring function of IO, which can be chained with IO which is having resource instance.
   * `Resource` is data structure which has different methods which take resource IO as an argument to create resources like `make()()`
   *
   */

  val simpleResource = IO("some resource instance") // Instantiate your resource inside the IO, like `new Connection` or `new Scanner`, `new File`, `new Reader`
  val usingResource: String => IO[String] = string => IO(s"using the string: $string").debug
  val releaseResource: String => IO[Unit] = string => IO(s"finalizing the string: $string").debug.void

  val usingResourceWithBracket = simpleResource
                                  .bracket(usingResource)(releaseResource)

  val usingResourceWithResource = Resource
                                  .make(simpleResource)(releaseResource)
                                  .use(usingResource)

  /**
   *  7. Exercise: read a text file with one line every 100 millis, using Resource
   *  (refactor the bracket exercise to use Resource)
   */
  def getResourceFromFile(path: String) = Resource.make(openFileScanner(path)) { scanner =>
    IO(s"closing file at $path").debug >> IO(scanner.close())
  }

  def resourceReadFile(path: String) =
    IO(s"opening file at $path") >>
      getResourceFromFile(path).use { scanner =>
        readLineByLine(scanner)
      }

  def cancelReadFile(path: String) = for {
    fib <- resourceReadFile(path).start
    _ <- IO.sleep(2.seconds) >> fib.cancel
  } yield ()

  // nested resources
  def connFromConfResource(path: String) =
    Resource.make(IO("opening file").debug >> openFileScanner(path))(scanner => IO("closing file").debug >> IO(scanner.close()))
      .flatMap(scanner => Resource.make(IO(new Connection(scanner.nextLine())))(conn => conn.close().void))

  // equivalent
  def connFromConfResourceClean(path: String) = for {
    scanner <- Resource.make(IO("opening file").debug >> openFileScanner(path))(scanner => IO("closing file").debug >> IO(scanner.close()))
    conn <- Resource.make(IO(new Connection(scanner.nextLine())))(conn => conn.close().void)
  } yield conn

  def connFromConfResourceMoreClean_v2(path: String): Resource[IO, Connection] = for {
    scanner <- {
      val fileResourceIO = IO("opening file").debug >> openFileScanner(path)
      val fileCloseIOFunction = (scanner: Scanner) => IO("closing file").debug >> IO(scanner.close())
      Resource.make(fileResourceIO)(fileCloseIOFunction)
    }
    conn <- {
      val connResourceIO = IO(new Connection(scanner.nextLine()))
      val connCloseIOFunction = (conn: Connection) => conn.close().void
      Resource.make(connResourceIO)(connCloseIOFunction)
    }
  } yield conn

  val openConnection = connFromConfResourceClean("cats-effect/src/main/resources/connection.txt").use(conn => conn.open() >> IO.never)
  val canceledConnection = for {
    fib <- openConnection.start
    _ <- IO.sleep(1.second) >> IO("cancelling!").debug >> fib.cancel
  } yield ()

  // connection + file will close automatically

  /**
   * 8. finalizers to regular IOs
   * 
   * someIO.guarantee(finalizerIO)
   * always Executes the given finalizerIO when the source is finished, either in success or in error, or if canceled. just like finally block
    */
  
  val ioWithFinalizer = IO("some resource").debug.guarantee(IO("freeing resource").debug.void)

  /**
   * 9. `someIO.bracket(useIOFunction)(releaseIOFunction)` vs `someIO.guarantee(finalizerIO)`
   * since guarantee function doesn't get instance of resource as an argument, it just execute the passed IO like finally block 
   * while in bracket curring function second argument is a function which gets instance of resource so we can clean up the resource here.
   * 
   * This equivalence always holds:
   * io.guarantee(f) <-> IO.unit.bracket(_ => io)(_ => f) -----> bracket which is not using resource instance in use function and release function
   */

  /**
   * 10. `someIO.guaranteeCase(finalizerIOFunction)`
   * 
   * it accept a function in argument, and this function get the outcomes of IO 
   * so we can execute the finalizing action by investigating the outcome type of IO result.
   * 
   */

  val ioWithFinalizer_v2 = IO("some resource").debug.guaranteeCase {
    case Succeeded(fa) => fa.flatMap(result => IO(s"releasing resource: $result").debug).void
    case Errored(e) => IO("nothing to release").debug.void
    case Canceled() => IO("resource got canceled, releasing what's left").debug.void
  }

  /**
   * 
   * io.guaranteeCase(f) <-> IO.unit.bracketCase(_ => io)((_, e) => f(e))
   */

  override def run = ioWithFinalizer.void
}
