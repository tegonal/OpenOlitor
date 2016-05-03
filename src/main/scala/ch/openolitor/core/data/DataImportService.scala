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
package ch.openolitor.core.data

import akka.actor._
import ch.openolitor.stammdaten._
import org.odftoolkit.simple._
import org.odftoolkit.simple.table._
import java.util.Date
import ch.openolitor.stammdaten.models._
import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.core.domain.EventService
import java.io.File
import ch.openolitor.core.db.evolution.scripts.V1Scripts
import scalikejdbc._
import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import ch.openolitor.core.repositories.BaseWriteRepository
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.repositories.SqlBinder
import java.io.InputStream

object DataImportService {
  case class ImportData(clearDatabaseBeforeImport: Boolean, document: InputStream)
  case class ImportResult(error: Option[String], result: Map[String, Int])

  def props(): Props = Props(classOf[DataImportService])
}

trait DataImportServiceComponent {
}

trait DataImportService extends Actor with ActorLogging
    with BaseWriteRepository
    with StammdatenDBMappings
    with BuchhaltungDBMappings {

  import DataImportService._
  import DataImportParser._

  val parser = context.actorOf(DataImportParser.props)
  var clearBeforeImport = false
  var originator: Option[ActorRef] = None

  val receive: Receive = {
    case ImportData(clearBefore, file) =>
      originator = Some(sender)
      clearBeforeImport = clearBefore
      parser ! ParseSpreadsheet(file)
      context become waitForResult
  }

  val waitForResult: Receive = {
    case e: ParseError =>
      originator.map(_ ! e)
    case ParseResult(projekt, kundentypen, kunden, personen, pendenzen, touren, depots, abotypen, vertriebsarten, lieferungen,
      lieferplanungen, lieferpositionen, abos, abwesenheiten, produkte, produktekategorien, produktProduktekategorien,
      produzenten, produktProduzenten, bestellungen, bestellpositionen) =>
      try {
        DB localTx { implicit session =>
          //clear database
          if (clearBeforeImport) {
            log.debug(s"Clear database before importing...")
            V1Scripts.dbInitializationScripts map { script =>
              script.execute
            }
          }

          //import entities
          //TODO: get userid from login
          implicit val userId = Boot.systemUserId
          log.debug(s"Start importing data")
          log.debug(s"Import Projekt...")
          var result = Map[String, Int]()
          insertEntity[Projekt, ProjektId](projekt)
          result = result + ("Projekt" -> 1)

          result = importEntityList[CustomKundentyp, CustomKundentypId]("Kundentypen", kundentypen, result)
          result = importEntityList[Person, PersonId]("Personen", personen, result)
          result = importEntityList[Kunde, KundeId]("Kunden", kunden, result)
          result = importEntityList[Pendenz, PendenzId]("Pendenzen", pendenzen, result)
          result = importEntityList[Tour, TourId]("Touren", touren, result)
          result = importEntityList[Depot, DepotId]("Depots", depots, result)
          result = importEntityList[Abotyp, AbotypId]("Abotypen", abotypen, result)

          log.debug(s"Import ${vertriebsarten.length} Vertriebsarten...")
          vertriebsarten.map {
            case dl: Depotlieferung =>
              insertEntity[Depotlieferung, VertriebsartId](dl)
            case hl: Heimlieferung =>
              insertEntity[Heimlieferung, VertriebsartId](hl)
            case pl: Postlieferung =>
              insertEntity[Postlieferung, VertriebsartId](pl)
          }
          result = result + ("Vertriebsarten" -> vertriebsarten.length)

          result = importEntityList[Lieferung, LieferungId]("Lieferungen", lieferungen, result)
          result = importEntityList[Lieferplanung, LieferplanungId]("Lieferplanungen", lieferplanungen, result)
          result = importEntityList[Lieferposition, LieferpositionId]("Lieferpositionen", lieferpositionen, result)

          log.debug(s"Import ${abos.length} Abos...")
          abos.map {
            case dl: DepotlieferungAbo =>
              insertEntity[DepotlieferungAbo, AboId](dl)
            case hl: HeimlieferungAbo =>
              insertEntity[HeimlieferungAbo, AboId](hl)
            case pl: PostlieferungAbo =>
              insertEntity[PostlieferungAbo, AboId](pl)
          }
          result = result + ("Abos" -> abos.length)

          result = importEntityList[Abwesenheit, AbwesenheitId]("Abwesenheiten", abwesenheiten, result)
          result = importEntityList[Produkt, ProduktId]("Produkte", produkte, result)
          result = importEntityList[Produktekategorie, ProduktekategorieId]("Produktekategorien", produktekategorien, result)
          result = importEntityList[ProduktProduktekategorie, ProduktProduktekategorieId]("ProduktProduktekategorien", produktProduktekategorien, result)
          result = importEntityList[Produzent, ProduzentId]("Produzenten", produzenten, result)
          result = importEntityList[ProduktProduzent, ProduktProduzentId]("ProduktProduzenten", produktProduzenten, result)
          result = importEntityList[Bestellung, BestellungId]("Bestellungen", bestellungen, result)
          result = importEntityList[Bestellposition, BestellpositionId]("Bestellpositionen", bestellpositionen, result)

          originator.map(_ ! ImportResult(None, result))
        }
      } catch {
        case t: Throwable =>
          originator.map(_ ! ImportResult(Option(t.getMessage), Map()))
      }
  }

  def importEntityList[E <: BaseEntity[I], I <: BaseId](name: String, entities: List[E], result: Map[String, Int])(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: UserId) = {
    log.debug(s"Import ${entities.length} $name...")
    entities.map { entity =>
      insertEntity[E, I](entity)
    }
    result + (name -> entities.length)
  }
}