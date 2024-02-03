### whats is callback?

A callback is a mechanism that allows a piece of code (a function or method) to be passed as an argument to another
piece of code, with the expectation that it will be executed at a certain point in the future or upon the occurrence of
a particular event. Callbacks are commonly used in asynchronous programming, event handling, and scenarios where you
want to define behavior that should happen in response to certain conditions or events or after a certain operation
completes.

### Java

In Java, callbacks are commonly implemented using interfaces or functional interfaces (introduced in Java 8) that define
a single abstract method. Objects implementing these interfaces can be passed around and invoked to achieve a callback
mechanism.
A functional interface is an interface with a single abstract method, and it can be implemented using a lambda
expression or a method reference.

Here's a simple example using Java's Runnable interface:

```java
// Java example 
// functional interface can be used in lambda expression 
public interface Callback {
    void onComplete();
}
```

```java
public class AsynchronousOperation {
    public void performAsyncOperation(Callback callback) {
        // Simulating an asynchronous operation
        new Thread(() -> {
            // ... some operation
            callback.onComplete();
        }).start();
    }
}
```

```java
// Usage
class Test {
    public static void main(String[] arg) {
        AsynchronousOperation operation = new AsynchronousOperation();
        //using the callback by passing lambda callback expression 
        operation.performAsyncOperation(() -> System.out.println("Operation completed!"));

    }
}
```

Java 8 introduced the `java.util.function` package, which includes various functional interfaces, such
as `Runnable`, `Consumer`, `Supplier`, etc. These interfaces are often used for callbacks.

```
// Runnable Example
Runnable onComplete = () -> System.out.println("Operation completed!");

// Usage
AsynchronousOperation operation = new AsynchronousOperation();
operation.performAsyncOperation(onComplete);
```

### Scala:

In Scala, you can use function literals (lambdas) directly as callbacks, providing a concise syntax.

Here's a similar example in Scala:

```scala
object CallbackExample {

  trait Callback {
    def onComplete(): Unit
  }

  class AsynchronousOperation {
    def performAsyncOperation(callback: Callback): Unit = {
      // Simulating an asynchronous operation
      new Thread(new Runnable {
        def run(): Unit = {
          // ... some operation
          callback.onComplete()
        }
      }).start()
    }
  }

  def main(args: Array[String]): Unit = {
    val operation = new AsynchronousOperation

    // Using a callback by passing lambda expression
    operation.performAsyncOperation(() => {
      println("Operation completed!")
    })
  }
}

```

Callbacks can directly represented as functions, making them more concise and expressive.

```scala
def doAsyncTask(callback: () => Unit) {
  // ... (perform asynchronous task)
  callback()
}

doAsyncTask(() => println("Task completed!"))
```

In both examples, the `AsynchronousOperation` class takes a `Callback` and performs some asynchronous operation,
invoking the `onComplete` method when done. The `main` method demonstrates how to use callbacks by passing functions or
function literals.

#### Ques: Is all higher order functions passed as an argument can be says a callback

Not all higher-order functions (HOFs) passed as arguments can be strictly considered callbacks. While they share the
concept of passing functions as arguments, callbacks have a more specific purpose and usage pattern.

Here's a breakdown of the key distinctions:

### 1. Callback:

#### 1.1. Purpose:

* Designed for delayed or asynchronous execution, often in response to events or triggers.
* Control Flow: The **callee** decides when to invoke the callback, often based on external factors or events.
* Common Use Cases: Asynchronous programming, event handling, asynchronous APIs, GUI interactions.

#### 1.2 Examples:

* Handling a button click in a GUI.
* Processing a network response asynchronously.
* Executing code after a timeout.

### 2. Higher-Order Function (HOF):

#### 2.1. Purpose:

* More general abstraction for working with functions as values, enabling operations like mapping, filtering, reducing,
  etc.
* Control Flow: The **caller** typically controls when and how the HOF invokes the passed function.
* Common Use Cases: Array/collection transformations, functional programming patterns, code clarity and conciseness.
*

#### 2.2 Examples:

* Mapping a function over an array to transform its elements.
* Filtering elements of a collection based on a predicate.
* Reducing a collection to a single value using a combining function.