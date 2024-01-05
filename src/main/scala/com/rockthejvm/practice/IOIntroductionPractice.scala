package com.rockthejvm.practice

import cats.effect.IO

object IOIntroductionPractice {
  // 1 - sequence two IOs and take the result of the LAST one
  // hint: use flatMap
  def sequenceTakeLast[A, B](ioa: IO[A], iob: IO[B]): IO[B] = {
    for {_ <- ioa
      result <- iob
    } yield result
  }

  def sequenceTakeLast2[A, B](ioa: IO[A], iob: IO[B]): IO[B] = {
    ioa.flatMap(_ => iob)
  }

  // 2 - sequence two IOs and take the result of the FIRST one
  // hint: use flatMap
  def sequenceTakeFirst[A, B](ioa: IO[A], iob: IO[B]): IO[A] =
    ioa.flatMap(result => iob.map(_ => result))

  // 3 - repeat an IO effect forever
  // hint: use flatMap + recursion
//  def forever[A](io: IO[A]): IO[A] = io.flatMap( _ => forever(io))

  def forever[A](io: IO[A]): IO[A] =
    io.flatMap(_ => forever(io))

//  def foreverTest() = forever(IO("testing"))

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global

    forever(IO("testing!")).unsafeRunSync()
  }

}
