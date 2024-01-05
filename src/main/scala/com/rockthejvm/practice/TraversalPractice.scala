package com.rockthejvm.practice

import cats.effect.{IO, IOApp}
import cats.syntax._
import cats.Traverse
import cats.instances.list._
import com.rockthejvm.utils._
object TraversalPractice extends IOApp.Simple {

  def sequence[A](listOfIOs: List[IO[A]]): IO[List[A]] = {
    val listTraverse = Traverse[List]
    listTraverse.traverse(listOfIOs)(identity)
  }
  val listOfIos = List(IO("first String"), IO("Second String"), IO("Third String"))
  override def run = sequence(listOfIos).debug.void

}
