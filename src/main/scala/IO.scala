package ornicar.scalalib

import scalaz.effects.{ IO ⇒ SIO }
import scalaz.Zero

trait IO extends scalaz.Zeros {

  implicit def IOZero[A: Zero]: Zero[SIO[A]] = new Zero[SIO[A]] {
    val zero = SIO.ioPure pure ∅[A]
  }

  implicit def ornicarRichIOA[A](ioa: SIO[A]) = new {

    def >>[B](iob: SIO[B]): SIO[B] = ioa flatMap (_ ⇒ iob)

    def void: SIO[Unit] = ioa map (_ ⇒ Unit)

    def inject[A](a: A): SIO[A] = ioa map (_ ⇒ a)
  }

  implicit def ornicarRichIOZero[A : Zero](iou: SIO[A]) = new {

    def doIf(cond: Boolean): SIO[A] = if (cond) iou else SIO.ioPure pure ∅[A]

    def doUnless(cond: Boolean): SIO[A] = if (cond) SIO.ioPure pure ∅[A] else iou
  }

  val void: SIO[Unit] = SIO.ioPure pure Unit
}
