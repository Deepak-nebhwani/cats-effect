# Cats Effect Resource Methods


Below response is from chat gpt so please read official doc for complete idea

https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html



## 1. Creating Resources

### a. `Resource.apply`  
//it is not working 

Creates a `Resource` from two `IO` actions - one for resource acquisition and another for resource release.

```scala
val myResource: Resource[IO, String] = Resource.apply(IO("Acquiring resource"))(_ => IO(println("Releasing resource")))
```
### b. `Resource.liftK`
Lifts an IO action into a Resource. The resource has a no-op release.

```scala
val simpleResource: Resource[IO, String] = Resource.liftK(IO("My resource"))
```

### c. `Resource.pure`
Creates a Resource with a constant value.

```scala
val constantResource: Resource[IO, Int] = Resource.pure(42)
```

### d. `Resource.eval`
Creates a Resource from an IO action. Lifts an applicative into a resource. The resource has a no-op release. Preserves interruptibility of fa

```scala
val dynamicResource: Resource[IO, String] = Resource.eval(IO(s"Dynamic resource: ${System.currentTimeMillis()}"))
```
## 2. Using Resources
  ### a. `Resource.use`
   Safely uses the acquired resource and ensures proper cleanup.

```scala
val result: IO[Unit] = myResource.use { acquiredResource =>
IO(println(s"Using resource: $acquiredResource"))
}
```

`
## 3. Composing Resources
 ###  a. `Resource.product`
   Combines two resources into a single resource that produces a tuple.

```scala
val composedResource: Resource[IO, (String, Int)] =
myResource.product(Resource.liftF(IO(42)))
```
### b. `Resource.map`
Transforms the result of a resource using a pure function.

```scala
val mappedResource: Resource[IO, String] =
myResource.map(_.toUpperCase)
```
### c. `Resource.flatMap`
Sequences two resources, using the result of the first to determine the second.

```scala
val flatMappedResource: Resource[IO, Int] =
myResource.flatMap(resource => Resource.liftF(IO(resource.length)))
```
## 4. Handling Resource Lifecycle

### b. `Resource.fromAutoCloseable`
Creates a Resource from an AutoCloseable resource (e.g., java.io.Closeable).

```scala
import java.io.FileWriter

val fileResource: Resource[IO, FileWriter] =
Resource.fromAutoCloseable(IO(new FileWriter("example.txt")))
```
### c. `Resource.fromAutoCloseableWeak`
Similar to fromAutoCloseable, but allows for resource release failures.

```scala
val fileResourceWeak: Resource[IO, FileWriter] =
Resource.fromAutoCloseableWeak(IO(new FileWriter("example.txt")))
```
## 5. Error Handling
### a. `Resource.attempt`
   Attempts to acquire a resource and returns an Either[Throwable, A].

```scala
val resultEither: IO[Either[Throwable, String]] =
myResource.attempt.use { result =>
IO(println(s"Result: $result"))
}
```
### b. `Resource.handleErrorWith`
Handles errors during resource acquisition by providing a fallback resource.

```scala
val fallbackResource: Resource[IO, String] =
myResource.handleErrorWith { error =>
Resource.pure("Fallback resource")
}
```
## 6. Custom Resource Management
### a. `Resource.makeCase`
   Similar to bracket, but provides information on how the resource was acquired or interrupted.

```scala
val resourceWithCase: Resource[IO, String] =
Resource.makeCase(acquire) { (resource, exitCase) =>
exitCase match {
case ExitCase.Completed => IO(println(s"Resource used successfully: $resource"))
case ExitCase.Error(e)  => IO(println(s"Error using resource: $e"))
case ExitCase.Canceled  => IO(println("Resource usage canceled"))
}
}
```