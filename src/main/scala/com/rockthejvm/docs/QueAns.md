##### Que: What is thunk?
##### Ans:
'thunk' is a function that takes no arguments (zero argument lambda functions) and, when invoked, performs a potentially expensive// operation (computing a square root, in this example) and/or causes some side-effect to occur
thunks are primarily used to delay a calculation until its result is needed,

call by name functions

`(thunk: => Future[A])`

##### Que: what is an Effect?
##### Ans:
An effect is basically something which changes the world. In programming, it's mainly expressed as side effects.
A very good blog to understand effect and effect-full computation, must read
https://alvinalexander.com/scala/what-effects-effectful-mean-in-functional-programming/

##### Que: What are some common sources of side effects in functional programming?
##### Ans:
1. Input and output
2. Mutable data structures
3. Global variables and state
4. Randomness and time
5. Exceptions and errors
* https://www.linkedin.com/advice/0/what-some-common-sources-side-effects-functional


##### Que: is IO an effect?
##### Ans: 
No IO is not an effect. An IO is a data structure that represents just a description of a side effectful computation. A data type for encoding side effects as pure values, capable of expressing both synchronous and asynchronous computations.
it is a monadic wrapper. it can be consider an Effect Type 



##### Que: Any other data structure to handle side effect
Use monads
Monads are a powerful abstraction that can help you handle side effects in a functional way. A monad is a type of data structure that wraps a value and provides a way to chain operations on that value. Monads can also encapsulate the context or the effects of those operations, such as errors, state, or IO.

Monad give us the ability to chain computation, it gives flatMap function where we can pass the computation as an argument returning the same monadic type
if we have defined map and flatMap function in a custom monad wrapper we can use for - yield comprehension for chaining the computation in imperative style.

* Option is a monad that models the effect of optionality
* Future is a monad that models latency as an effect
* Try is a monad that models the effect of failures (manages exceptions as effects)
* Reader is a monad that models the effect of composing operations that depend on some input
* Writer is a monad that models logging as an effect
* State is a monad that models the effect of state (composing a series of computations that maintain state)
* IO is a monad that models the effect of any computation that might produce side effects


##### Que: How do you manage side effects in your functional software design?

1. Use monads
2. Use effects systems
3. Use functional reactive programming
4. Use immutable data structures
5. Use dependency injection
* https://www.linkedin.com/advice/0/how-do-you-manage-side-effects-your-functional#:~:text=to%20help%20you.-,1%20Use%20monads,errors%2C%20state%2C%20or%20IO

##### Que: what is use of `mapN` function ?
##### Ans:
`mapN` is generally a extension function which can be applied on tuple of monadic computations like
* `(Option(a), Option(b), Option(c)).mapN((a,b,c) => T)` the result will be Option[T]
* `(IO(a), IO(b), IO(c)).mapN((a,b,c) => T)` the result will be IO[T]
* `(Future(a), Future(b), Future(c)).mapN((a,b,c) => T)` the result will be Future[T]
* `(parIO1:IO.Par[Int], parIO2:IO.Par[String]).mapN((num, string) => T)`

It will lift the values from monadic tuple, and expect a function from values, if it is a Tuple2 then we can pass function which expect 2 args,
if it a Tuple5 then we can pass function with 5 args (matching datatypes)

Imports:
* import cats.syntax.apply._
* import cats.effect.implicits._
* import cats.syntax.parallel._

  ```
  import cats.syntax.apply._ // for apply applicative mapN is comming from here
  import cats.implicits._ //for either class right function
  import scala.concurrent.ExecutionContext.Implicits.global // for future
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
  
  //Output: 
  //Some(Hello 1 + 4.5)
  //Right(Hello 1 + 4.5)
  //Future(Success(Hello 1 + 4.5))
  //List(Hello 1 + 4.5)
  ```
if any one computation is failing in tuple the overall output will be failure one, like None, Left(), Future(Failure())


#### Qua: what are the available Finalizers just like finally block 
* someIO.onCancel <--- it will only called when the fiber containing this IO will receive cancel signal
* someIO.bracket(useFunction)(releaseFunction) <---- releaseFunction will always get call (after success, failure or cancel)
* 


#### Qua: What is callback function
#### Ans: [click to go on this page AsyncIO.md](AsyncIO.md)

#### Que: What is caller vs callee functions 


#### Caller vs. Callee Functions

In computer programming, caller and callee functions designate the roles functions play in the execution process:

#### Caller Function
* Initiates the execution of another function (callee).
* Prepares arguments, pushes them onto the stack (if applicable), and jumps to the callee's entry point.
* Resumes execution after the callee returns.
#### Responsibilities:
* Argument preparation
* Stack management (potentially)
* Error handling
#### Callee Function
* Function being called by another function (caller).
* Receives arguments, performs a task, and returns a result (or void).
#### Responsibilities:
* Argument processing
* Task execution
* Return value (or void)

#### Analogy:

Here's an analogy to understand the concept:

* Think of the caller as a restaurant manager taking your order (call) and delivering it to the chef (callee).
* The manager (caller) gathers the ingredients (arguments), sends them to the chef, and waits for the meal (return value).
* The chef (callee) receives the ingredients, prepares the dish (executes the task), and sends it back to the manager.e as chef: Receives ingredients, prepares the dish (executes the task), and sends it back to the manager.