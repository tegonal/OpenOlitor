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
package ch.openolitor.buchhaltung

import ch.openolitor.stammdaten.models.AboId
import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.core.repositories.ParameterBinderMapping
import ch.openolitor.buchhaltung.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.stammdaten.StammdatenDBMappings

//DB Model bindig
trait BuchhaltungDBMappings extends DBMappings with StammdatenDBMappings {
  import TypeBinder._

  // DB type binders for read operations
  implicit val rechnungIdBinder: TypeBinder[RechnungId] = baseIdTypeBinder(RechnungId.apply _)
  implicit val rechnungsPositionIdBinder: TypeBinder[RechnungsPositionId] = baseIdTypeBinder(RechnungsPositionId.apply _)
  implicit val zahlungsImportIdBinder: TypeBinder[ZahlungsImportId] = baseIdTypeBinder(ZahlungsImportId.apply _)
  implicit val zahlungsEingangIdBinder: TypeBinder[ZahlungsEingangId] = baseIdTypeBinder(ZahlungsEingangId.apply _)

  implicit val rechnungStatusTypeBinder: TypeBinder[RechnungStatus] = string.map(RechnungStatus.apply)
  implicit val rechnungsPositionStatusTypeBinder: TypeBinder[RechnungsPositionStatus.RechnungsPositionStatus] = string.map(RechnungsPositionStatus.apply)
  implicit val rechnungsPositionTypTypeBinder: TypeBinder[RechnungsPositionTyp.RechnungsPositionTyp] = string.map(RechnungsPositionTyp.apply)
  implicit val optionRechnungIdBinder: TypeBinder[Option[RechnungId]] = optionBaseIdTypeBinder(RechnungId.apply _)
  implicit val optionAboIdBinder: TypeBinder[Option[AboId]] = optionBaseIdTypeBinder(AboId.apply _)

  implicit val zahlungsEingangStatusTypeBinder: TypeBinder[ZahlungsEingangStatus] = string.map(ZahlungsEingangStatus.apply)

  //DB parameter binders for write and query operationsit
  implicit val rechnungStatusBinder = toStringSqlBinder[RechnungStatus]
  implicit val rechnungsPositionStatusBinder = toStringSqlBinder[RechnungsPositionStatus.RechnungsPositionStatus]
  implicit val rechnungsPositionTypBinder = toStringSqlBinder[RechnungsPositionTyp.RechnungsPositionTyp]
  implicit val zahlungsEingangStatusBinder = toStringSqlBinder[ZahlungsEingangStatus]

  implicit val rechnungIdSqlBinder = baseIdSqlBinder[RechnungId]
  implicit val optionRechnungIdSqlBinder = optionSqlBinder[RechnungId]
  implicit val rechnungsPositionIdSqlBinder = baseIdSqlBinder[RechnungsPositionId]
  implicit val optionRechnungsPositionIdSqlBinder = optionSqlBinder[RechnungsPositionId]
  implicit val optionAboIdSqlBinder = optionSqlBinder[AboId]
  implicit val zahlungsEingangIdSqlBinder = baseIdSqlBinder[ZahlungsEingangId]
  implicit val zahlungsImportIdSqlBinder = baseIdSqlBinder[ZahlungsImportId]

  implicit val rechnungMapping = new BaseEntitySQLSyntaxSupport[Rechnung] {
    override val tableName = "Rechnung"

    override lazy val columns = autoColumns[Rechnung]()

    def apply(rn: ResultName[Rechnung])(rs: WrappedResultSet): Rechnung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Rechnung): Seq[Any] =
      parameters(Rechnung.unapply(entity).get)

    override def updateParameters(entity: Rechnung) = {
      super.updateParameters(entity) ++ Seq(
        column.kundeId -> parameter(entity.kundeId),
        column.titel -> parameter(entity.titel),
        column.betrag -> parameter(entity.betrag),
        column.einbezahlterBetrag -> parameter(entity.einbezahlterBetrag),
        column.waehrung -> parameter(entity.waehrung),
        column.rechnungsDatum -> parameter(entity.rechnungsDatum),
        column.faelligkeitsDatum -> parameter(entity.faelligkeitsDatum),
        column.eingangsDatum -> parameter(entity.eingangsDatum),
        column.status -> parameter(entity.status),
        column.referenzNummer -> parameter(entity.referenzNummer),
        column.esrNummer -> parameter(entity.esrNummer),
        column.fileStoreId -> parameter(entity.fileStoreId),
        column.anzahlMahnungen -> parameter(entity.anzahlMahnungen),
        column.mahnungFileStoreIds -> parameter(entity.mahnungFileStoreIds),
        column.strasse -> parameter(entity.strasse),
        column.hausNummer -> parameter(entity.hausNummer),
        column.adressZusatz -> parameter(entity.adressZusatz),
        column.plz -> parameter(entity.plz),
        column.ort -> parameter(entity.ort)
      )
    }
  }

  implicit val rechnungsPositionMapping = new BaseEntitySQLSyntaxSupport[RechnungsPosition] {
    override val tableName = "RechnungsPosition"

    override lazy val columns = autoColumns[RechnungsPosition]()

    def apply(rn: ResultName[RechnungsPosition])(rs: WrappedResultSet): RechnungsPosition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: RechnungsPosition): Seq[Any] =
      parameters(RechnungsPosition.unapply(entity).get)

    override def updateParameters(entity: RechnungsPosition) = {
      super.updateParameters(entity) ++ Seq(
        column.rechnungId -> parameter(entity.rechnungId),
        column.parentRechnungsPositionId -> parameter(entity.parentRechnungsPositionId),
        column.aboId -> parameter(entity.aboId),
        column.kundeId -> parameter(entity.kundeId),
        column.betrag -> parameter(entity.betrag),
        column.waehrung -> parameter(entity.waehrung),
        column.anzahlLieferungen -> parameter(entity.anzahlLieferungen),
        column.beschrieb -> parameter(entity.beschrieb),
        column.status -> parameter(entity.status),
        column.typ -> parameter(entity.typ),
        column.sort -> parameter(entity.sort)
      )
    }
  }

  implicit val zahlungsImportMapping = new BaseEntitySQLSyntaxSupport[ZahlungsImport] {
    override val tableName = "ZahlungsImport"

    override lazy val columns = autoColumns[ZahlungsImport]()

    def apply(rn: ResultName[ZahlungsImport])(rs: WrappedResultSet): ZahlungsImport =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsImport): Seq[Any] =
      parameters(ZahlungsImport.unapply(entity).get)

    override def updateParameters(entity: ZahlungsImport) = {
      super.updateParameters(entity) ++ Seq(
        column.file -> parameter(entity.file),
        column.anzahlZahlungsEingaenge -> parameter(entity.anzahlZahlungsEingaenge),
        column.anzahlZahlungsEingaengeErledigt -> parameter(entity.anzahlZahlungsEingaengeErledigt)
      )
    }
  }

  implicit val zahlungsEingangMapping = new BaseEntitySQLSyntaxSupport[ZahlungsEingang] {
    override val tableName = "ZahlungsEingang"

    override lazy val columns = autoColumns[ZahlungsEingang]()

    def apply(rn: ResultName[ZahlungsEingang])(rs: WrappedResultSet): ZahlungsEingang =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsEingang): Seq[Any] =
      parameters(ZahlungsEingang.unapply(entity).get)

    override def updateParameters(entity: ZahlungsEingang) = {
      super.updateParameters(entity) ++ Seq(
        column.erledigt -> parameter(entity.erledigt),
        column.bemerkung -> parameter(entity.bemerkung)
      )
    }
  }
}
