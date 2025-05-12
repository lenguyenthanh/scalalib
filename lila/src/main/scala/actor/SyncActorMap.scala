package scalalib
package actor

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.Executor

final class SyncActorMap[Id, T <: SyncActor](
    mkActor: Id => T,
    accessTimeout: FiniteDuration
)(using Executor):

  def getOrMake(id: Id): T = actors.get(id)

  def touchOrMake(id: Id): Unit = getOrMake(id)

  def getIfPresent(id: Id): Option[T] = actors.getIfPresent(id)

  def tell(id: Id, msg: Matchable): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: Id, msg: => Matchable): Unit = getIfPresent(id).foreach(_ ! msg)

  def ask[A](id: Id)(makeMsg: Promise[A] => Matchable): Future[A] = getOrMake(id).ask(makeMsg)

  private val actors: LoadingCache[Id, T] =
    cache.scaffeine
      .expireAfterAccess(accessTimeout)
      .removalListener((_: Id, actor: T, _: RemovalCause) => actor.stop())
      .build[Id, T](mkActor)
