
### _The below content is a comment from `cats.effect.kernel.GenSpawn` `trait`_

https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/GenSpawn.html



A typeclass that characterizes monads which support spawning and racing of
 fibers. `[[GenSpawn]]` extends the capabilities of `[[MonadCancel]]`, so an
 instance of this typeclass must also provide a lawful instance for
 `[[MonadCancel]]`.

 This documentation builds upon concepts introduced in the `[[MonadCancel]] `
 documentation.

 ## Concurrency

 `[[GenSpawn]]` introduces a notion of concurrency that enables fibers to
 safely interact with each other via three special functions.

 `[[GenSpawn!.start start]]` spawns a fiber that executes concurrently with the
 spawning fiber.` [[Fiber!.join join]]` semantically blocks the joining fiber
 until the joinee fiber terminates, after which the `[[Outcome]]` of the joinee is
 returned. `[[Fiber!.cancel cancel]] `requests a fiber to abnormally terminate,
 and semantically blocks the canceller until the cancellee has completed
 finalization.

 Just like threads, fibers can execute concurrently with respect to
 each other. This means that the effects of independent fibers may be
 interleaved nondeterministically. This mode of concurrency reaps benefits
 for modular program design; fibers that are described separately can execute
 simultaneously without requiring programmers to explicitly yield back to the
 runtime system.

 The interleaving of effects is illustrated in the following program:

 ```{{{

   for {
     fa <- (println("A1") *> println("A2")).start
     fb <- (println("B1") *> println("B2")).start
   } yield ()

 }}}
 ```

 In this program, two fibers A and B are spawned concurrently. There are six
 possible executions, each of which exhibits a different ordering of effects.
 The observed output of each execution is shown below:
```
   1. A1, A2, B1, B2
   1. A1, B1, A2, B2
   1. A1, B1, B2, A2
   1. B1, B2, A1, A2
   1. B1, A1, B2, A2
   1. B1, A1, A2, B3
```
 Notice how every execution preserves sequential consistency of the effects
 within each fiber: `A1` always prints before `A2`, and `B1` always prints
 before `B2`. However, there are no guarantees around how the effects of
 both fibers will be ordered with respect to each other; it is entirely
 nondeterministic.

 ## Cancelation

 `[[MonadCancel]]` introduces a simple means of cancelation, particularly
 self-cancelation, where a fiber can request the abnormal termination of its
 own execution. This is achieved by calling
 `[[MonadCancel!.canceled canceled]]`.

 `[[GenSpawn]]` expands on the cancelation model described by `[[MonadCancel]]`
 by introducing a means of external cancelation. With external cancelation,
 a fiber can request the abnormal termination of another fiber by calling
 `[[Fiber!.cancel]]`.

 The cancelation model dictates that external cancelation behaves
 identically to self-cancelation. To guarantee consistent behavior between
 the two, the following semantics are shared:

   1. Masking: if a fiber is canceled while it is masked, cancelation is
      suppressed until it reaches a completely unmasked state. See
      `[[MonadCancel]]` documentation for more details.
   1. Backpressure: `[[Fiber!.cancel cancel]]` semantically blocks all callers
      until finalization is complete.
   1. Idempotency: once a fiber's cancelation has been requested, subsequent
      cancelations have no effect on cancelation status.
   1. Terminal: Cancelation of a fiber that has terminated immediately
      returns.

 External cancelation contrasts with self-cancelation in one aspect: the
 former may require synchronization between multiple threads to communicate
 a cancelation request. As a result, cancelation may not be immediately
 observed by a fiber. Implementations are free to decide how and when this
 synchronization takes place.

 ## Cancelation safety

 A function or effect is considered to be cancelation-safe if it can be run
 in the absence of masking without violating effectful lifecycles or leaking
 resources. These functions require extra attention and care from users to
 ensure safe usage.

 `[[start]]` and `[[racePair]]` are both considered to be cancelation-unsafe
 effects because they return a `[[Fiber]]`, which is a resource that has a
 lifecycle.
```
 {{{

   // Start a fiber that continuously prints "A".
   // After 10 seconds, cancel the fiber.
   F.start(F.delay(println("A")).foreverM).flatMap { fiber =>
     F.sleep(10.seconds) *> fiber.cancel
   }

 }}}
```
 In the above example, imagine the spawning fiber is canceled after it
 starts the printing fiber, but before the latter is canceled. In this
 situation, the printing fiber is not canceled and will continue executing
 forever, contending with other fibers for system resources. Fiber leaks like
 this typically happen because some fiber that holds a reference to a child
 fiber is canceled before the child terminates; like threads, fibers will
 not automatically be cleaned up.

 Resource leaks like this are unfavorable when writing applications. In
 the case of `[[start]]` and `[[racePair]]`, it is recommended not to use
 these methods; instead, use `[[background]]` and `[[race]]` respectively.

 The following example depicts a safer version of the `[[start]]` example
 above:

 ```
 {{{

   // Starts a fiber that continously prints "A".
   // After 10 seconds, the resource scope exits so the fiber is canceled.
   F.background(F.delay(println("A")).foreverM).use { _ =>
     F.sleep(10.seconds)
   }

 }}}
 ```

 ## Scheduling

 Fibers are commonly referred to as ''lightweight threads'' or
 ''green threads''. This alludes to the nature by which fibers are scheduled
 by runtime systems: many fibers are multiplexed onto one or more native
 threads.

 For applications running on the JVM, the scheduler typically manages a thread
 pool onto which fibers are scheduled. These fibers are executed
 simultaneously by the threads in the pool, achieving both concurrency and
 parallelism. For applications running on JavaScript platforms, all compute
 is restricted to a single worker thread, so multiple fibers must share that
 worker thread (dictated by fairness properties), achieving concurrency
 without parallelism.

 `[[cede]]` is a special function that interacts directly with the underlying
 scheduler. It is a means of cooperative multitasking by which a fiber
 signals to the runtime system that it intends to pause execution and
 resume at some later time at the discretion of the scheduler. This is in
 contrast to preemptive multitasking, in which threads of control are forcibly
 yielded after a well-defined time slice.

 Preemptive and cooperative multitasking are both features of runtime systems
 that influence the fairness and throughput properties of an application.
 These modes of scheduling are not necessarily mutually exclusive: a runtime
 system may incorporate a blend of the two, where fibers can explicitly yield
 back to the scheduler, but the runtime preempts a fiber if it has not
 yielded for some time.

 For more details on schedulers, see the following resources:

   1. https://gist.github.com/djspiewak/3ac3f3f55a780e8ab6fa2ca87160ca40
   1. https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c
      