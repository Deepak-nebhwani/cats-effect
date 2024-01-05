package com.rockthejvm.practice

object EffectsPractice extends App {

  // instantiation of MyIO, it will not start anything, opposite to future where execution start as soon as we instantiate
  val myIO = MyIO(() => println(("Hello Example")))



  // once we will call it will start execution
  myIO.unsafeRun()

  // unsafeRun: () => A; this is known as Zero Lambda function.
  /*case class MyIO[A](unsafeRun: () => A) {

    def map[B](f: A => B): MyIO[B] = {
      MyIO[B](() => f(unsafeRun()))
    }

    def flatMap[B](f: A => MyIO[B]): MyIO[B] = {
      MyIO[B](() => {
        val a = f(unsafeRun())
        // if we remove case from class below will not work because "a" will become ref instead of object of MyIO[B]
        a.unsafeRun()
      })
    }

  }*/

  case class MyIO[A](unsafeRun: () => A) {
    def map[B](f: A => B): MyIO[B] =
      MyIO(() => f(unsafeRun()))

    def flatMap[B](f: A => MyIO[B]): MyIO[B] =
      MyIO(() => f(unsafeRun()).unsafeRun())
  }

  //Exercises
  //1. An IO which returns the current time of the system
  case class CurrentTimeIo(currentTime: () => Long = () => System.currentTimeMillis())


  // 2. An IO which measures the duration of a computation (hint: use ex 1)

  val time: MyIO[Long] = MyIO[Long](() => System.currentTimeMillis())

//  println(time.unsafeRun())

// Note Down, below function is a data structure and
// calling this function will not start execution,
// execution will start after calling unsafeRun
  def measure[A](computation: MyIO[A]): MyIO[Long] = for {
    start <- time
    k <- computation
    end <- time
  } yield {
    end - start
  }

  def measure2[A](computation: MyIO[A]): MyIO[Long] =
    time.flatMap(start => computation.flatMap(_ => time.map(end => end - start)))

//Usage
 println( measure2(MyIO(() => {
//    Thread.sleep(5)
    println("compute Finish")
  })).unsafeRun())

}



