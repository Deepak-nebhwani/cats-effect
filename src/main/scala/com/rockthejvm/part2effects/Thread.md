### `t.join()`
If any executing thread t1 calls join() on t2 i.e; `t2.join()` immediately t1 will enter into waiting state until t2 completes its execution.

Giving a timeout within join(), will make the join() effect to be nullified after the specific timeout.

`t2.join `is similar to `Future.await(future2)` in scala, `t2` will finish it's execution first and the thread which is calling it, will be blocked until `t2` is not finishing its stuff

### `Thread.yield()`
If a thread wants to pass its execution to give chance to remaining threads of the same priority then we should go for yield()

### usage of yield()
As the official documentation suggests it’s rarely necessary to use `Thread.yield()` and hence should be avoided unless very clear with the objectives in the light of its behavior.

Suppose there are three threads t1, t2, and t3. Thread t1 gets the processor and starts its execution and thread t2 and t3 are in Ready/Runnable state. The completion time for thread t1 is 5 hours and the completion time for t2 is 5 minutes. Since t1 will complete its execution after 5 hours, t2 has to wait for 5 hours to just finish 5 minutes job. In such scenarios where one thread is taking too much time to complete its execution, we need a way to prevent the execution of a thread in between if something important is pending. yield() helps us in doing so.

The yield() basically means that the thread is not doing anything particularly important and if any other threads or processes need to be run, they should run. Otherwise, the current thread will continue to run.

### `Thread.yield()` vs `Thread.sleep()`

While yield() can only make a heuristic attempt to suspend the execution of the current thread with no guarantee of when will it be scheduled back (if no other thread present with same priority or higher priority it will not stop execution), sleep() can force the scheduler to suspend the execution of the current thread for at least the mentioned time period as its parameter

### Lifecycle methods of Thread
https://www.baeldung.com/java-thread-lifecycle


