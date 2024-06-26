package scalalib
package json

import play.api.libs.json.{ Json as PlayJson, * }
import java.time.Instant

import scala.util.NotGiven
import scalalib.newtypes.SameRuntime
import scalalib.model.*
import scalalib.time.toMillis

object Json:

  trait NoJsonHandler[A] // don't create default JSON handlers for this type

  given opaqueFormat[A, T](using
      bts: SameRuntime[A, T],
      stb: SameRuntime[T, A],
      format: Format[A]
  )(using NotGiven[NoJsonHandler[T]]): Format[T] =
    format.bimap(bts.apply, stb.apply)

  given [A](using Render[A]): KeyWrites[A] with
    def writeKey(key: A) = key.render

  given [A](using Render[A]): Writes[A] = a => JsString(a.render)

  private val stringFormatBase: Format[String] = Format(Reads.StringReads, Writes.StringWrites)
  private val intFormatBase: Format[Int]       = Format(Reads.IntReads, Writes.IntWrites)

  def stringFormat[A <: String](f: String => A): Format[A] = stringFormatBase.bimap(f, identity)
  def intFormat[A <: Int](f: Int => A): Format[A]          = intFormatBase.bimap(f, identity)

  def writeAs[O, A: Writes](f: O => A) = Writes[O](o => PlayJson.toJson(f(o)))

  def writeWrap[A, B](fieldName: String)(get: A => B)(using writes: Writes[B]): OWrites[A] = OWrites: a =>
    PlayJson.obj(fieldName -> writes.writes(get(a)))

  def stringIsoWriter[O](using iso: Iso[String, O]): Writes[O] = writeAs[O, String](iso.to)
  def intIsoWriter[O](using iso: Iso[Int, O]): Writes[O]       = writeAs[O, Int](iso.to)

  def stringIsoReader[O](using iso: Iso[String, O]): Reads[O] = Reads.of[String].map(iso.from)

  def intIsoFormat[O](using iso: Iso[Int, O]): Format[O] =
    Format[O](
      Reads.of[Int].map(iso.from),
      Writes: o =>
        JsNumber(iso to o)
    )

  def stringIsoFormat[O](using iso: Iso[String, O]): Format[O] =
    Format[O](
      Reads.of[String].map(iso.from),
      Writes: o =>
        JsString(iso to o)
    )

  def stringRead[O](from: String => O): Reads[O] = Reads.of[String].map(from)

  def optRead[O](from: String => Option[O]): Reads[O] = Reads
    .of[String]
    .flatMapResult: str =>
      from(str).fold[JsResult[O]](JsError(s"Invalid value: $str"))(JsSuccess(_))
  def optFormat[O](from: String => Option[O], to: O => String): Format[O] = Format[O](
    optRead(from),
    Writes(o => JsString(to(o)))
  )

  def tryRead[O](from: String => scala.util.Try[O]): Reads[O] = Reads
    .of[String]
    .flatMapResult: code =>
      from(code).fold(err => JsError(err.getMessage), JsSuccess(_))
  def tryFormat[O](from: String => scala.util.Try[O], to: O => String): Format[O] = Format[O](
    tryRead(from),
    Writes[O](o => JsString(to(o)))
  )

  given Writes[Instant] = writeAs(_.toMillis)

  // given Writes[chess.Color] = writeAs(_.name)
  //
  // given Reads[Uci] = Reads
  //   .of[String]
  //   .flatMapResult: str =>
  //     JsResult.fromTry(Uci(str).toTry(s"Invalid UCI: $str"))
  // given Writes[Uci] = writeAs(_.uci)
  //
  // given Reads[LilaOpeningFamily] = Reads[LilaOpeningFamily]: f =>
  //   f.get[String]("key")
  //     .flatMap(LilaOpeningFamily.find)
  //     .fold[JsResult[LilaOpeningFamily]](JsError(Nil))(JsSuccess(_))
  //
  // given NoJsonHandler[chess.Square] with {}
  //
  // given OWrites[Crazyhouse.Pocket] = OWrites: p =>
  //   JsObject:
  //     p.flatMap((role, nb) => Option.when(nb > 0)(role.name -> JsNumber(nb)))
  //
  // given OWrites[chess.variant.Crazyhouse.Data] = OWrites: v =>
  //   PlayJson.obj("pockets" -> v.pockets.all)
  //

  given Writes[MaxPerPage] with
    def writes(m: MaxPerPage) = JsNumber(m.value)

  import scalalib.paginator.Paginator
  given paginatorWrite[A: Writes]: OWrites[Paginator[A]] = OWrites[Paginator[A]]: p =>
    PlayJson.obj(
      "currentPage"        -> p.currentPage,
      "maxPerPage"         -> p.maxPerPage,
      "currentPageResults" -> p.currentPageResults,
      "nbResults"          -> p.nbResults,
      "previousPage"       -> p.previousPage,
      "nextPage"           -> p.nextPage,
      "nbPages"            -> p.nbPages
    )

  // import lila.core.LightUser
  // given lightUserWrites: OWrites[LightUser] = OWrites(lightUser.write)
  // object lightUser:
  //   def write(u: LightUser): JsObject = writeNoId(u) + ("id" -> JsString(u.id.value))
  //   def writeNoId(u: LightUser): JsObject = PlayJson
  //     .obj("name" -> u.name)
  //     .add("title", u.title)
  //     .add("flair", u.flair)
  //     .add("patron", u.isPatron)
