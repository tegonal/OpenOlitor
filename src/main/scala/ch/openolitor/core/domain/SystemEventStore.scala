/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.domain

import org.joda.time.DateTime
import ch.openolitor.core.models.PersonId
import akka.persistence._
import ch.openolitor.core.SystemConfig
import akka.actor._
import ch.openolitor.core.AkkaEventStream

object SystemEventStore {
  val VERSION = 1
  val persistenceId = "event-store"

  case class SystemEventStoreState(startTime: DateTime, seqNr: Long) extends State

  def props()(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultSystemEventStore], sysConfig)
}

/**
 * PersistentActor welcher SystemEvents speichert
 */
trait SystemEventStore extends AggregateRoot with AkkaEventStream {
  import SystemEvents._
  import SystemEventStore._
  import AggregateRoot._

  override def persistenceId: String = SystemEventStore.persistenceId
  type S = SystemEventStoreState
  override var state: SystemEventStoreState = SystemEventStoreState(DateTime.now, 0L)
  override val system = context.system

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: PersistentEvent): Unit = {
    evt match {
      case PersistentSystemEvent(meta, event) =>
        //publish event to eventstream
        publish(event)
      case _ =>
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    state match {
      case Removed => context become removed
      case Created => context become created
    }
  }

  val removed: Receive = {
    case _ =>
  }

  override val receiveCommand = created

  /**
   * Eventlog initialized, handle entity events
   */
  val created: Receive = {
    case KillAggregate =>
      log.debug(s"created => KillAggregate")
      context.stop(self)
    case GetState =>
      log.debug(s"created => GetState")
      sender ! state
    case systemEvent: SystemEvent =>
      persist(PersistentSystemEvent(metadata, systemEvent))(afterEventPersisted)
  }

  def metadata = {
    EventMetadata(SystemPersonId, VERSION, DateTime.now, state.seqNr, persistenceId)
  }

  def incState = {
    state.copy(seqNr = state.seqNr + 1)
  }
}

class DefaultSystemEventStore(sysConfig: SystemConfig) extends SystemEventStore {
}
