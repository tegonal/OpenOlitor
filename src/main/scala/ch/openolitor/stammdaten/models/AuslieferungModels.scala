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
package ch.openolitor.stammdaten.models

import ch.openolitor.core.models._
import org.joda.time.DateTime

case class AuslieferungId(id: Long) extends BaseId

sealed trait AuslieferungStatus

case object Erfasst extends AuslieferungStatus
case object Ausgeliefert extends AuslieferungStatus

object AuslieferungStatus {
  def apply(value: String): AuslieferungStatus = {
    Vector(Erfasst, Ausgeliefert) find (_.toString == value) getOrElse (Erfasst)
  }
}

/**
 * Die Auslieferung repräsentiert eine Sammlung von Körben zu einem Bestimmten Lieferzeitpunkt mit einem Ziel.
 */
trait Auslieferung extends BaseEntity[AuslieferungId] {
  val lieferungId: LieferungId
  val status: AuslieferungStatus
  val datum: DateTime
  val anzahlKoerbe: Int
}

/**
 * Auslieferung pro Depot
 */
case class DepotAuslieferung(
  id: AuslieferungId,
  lieferungId: LieferungId,
  status: AuslieferungStatus,
  depotName: String,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung

/**
 * Auslieferung pro Tour
 */
case class TourAuslieferung(
  id: AuslieferungId,
  lieferungId: LieferungId,
  status: AuslieferungStatus,
  tourName: String,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung

/**
 * Auslieferung zur Post
 */
case class PostAuslieferung(
  id: AuslieferungId,
  lieferungId: LieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung