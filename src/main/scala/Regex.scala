package ornicar.scalalib

import scala.util.matching.{ Regex => ScalaRegex }

trait Regex {

  implicit def richRegex(r: ScalaRegex) = new {

    def matches(s: String): Boolean = 
      r.pattern.matcher(s).matches
  }
}
