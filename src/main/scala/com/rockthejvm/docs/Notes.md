## 2.1-effects <br>

## 2.2-effects-exercises

1. Pure functional Programming and referential transparency (substitution of expression by its value)
2. What is an effect and effect properties
3. Validate effect properties for Option, Future, MyIO
4. MyIO data type Monad
5. Exercises
    *
        1. An IO which returns the current time of the system
    *
        2. An IO which measures the duration of a computation (hint: use ex 1)
    *
        3. An IO which prints something to the console
    *
        4. An IO which reads a line (a string) from the std input

## 2.3-io <br>

* ``IO.pure(42)``it is eager and evaluate at the time of instantiation. en-wrap computation should not produce side
  effect.
* IO should delay the side effect.
* unsafe global is a thread pool to run the IO computations.
* A Program or Method returning IO is just a description of problem and will get executed when we call unsafeRun methods
  with global thread pool

1. What is IO , `IO.pure`, `IO.delay`, `IO.apply`, `IO`
2. IO map and flatMap
3. Composition of multiple IO computations using for-yield composition
4. `mapN` from apply type class instances - combine IO effects as tuples, just like reduce `import cats.syntax.apply._`
5. go to `IO.scala` class most common methods having good description.

## 2.4-io-exercises

1. How Lazy evaluation of IO prevent stackOverflow failure (in `flatMap` and in `>>`)
2. eager evaluation of IO `*>` (andThen operator)
3. call By Name arguments make it lazy and will be executed when we use `unsafeRunSync`, while eager arguments get
   executed instantly.
4. When you create a chaining of IO flatMap implementation, it internally create a long chain of LinkedList operation
   which uses tail recursion and prevent stack over flow.
5. IO.defer is used when we want to suspend the effects ( by wrapping a recursive call in IO will suspend the execution
   and gives stack safety )
6. ` IO.defer(IO(println))` is same as `IO(IO(println)).flatten` or `IO.delay(IO(println)).flatten`
   or `IO(IO(println)).flatMap(x => x)`.
7. ` IO.defer(IO(println))` Suspends a synchronous side effect which produces an IO in IO. This is useful for
   trampolining (i.e. when the side effect is conceptually the allocation of a stack frame).

## `2.5-io-error-handling`

see comments in class

1. create failed effects --> raise exception from a computation `IO.raiseError(new RuntimeException("a proper fail"))`
2. handle exceptions

`aFailure.handleErrorWith{
case
}`

## `2.6-io-apps`

## `2.7-io-parallelism`

## `2.8-io-traversal`

## 3.1-fibers

https://blog.rockthejvm.com/cats-effect-fibers/

Migration from v2 to v3
https://typelevel.org/cats-effect/docs/migration-guide
