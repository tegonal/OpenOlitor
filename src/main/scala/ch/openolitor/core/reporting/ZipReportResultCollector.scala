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
package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.core.reporting.ReportSystem._
import java.io.ByteArrayOutputStream
import scala.util._
import ch.openolitor.util.ZipBuilder

object ZipReportResultCollector {
  def props(reportSystem: ActorRef): Props = Props(classOf[ZipReportResultCollector], reportSystem)
}

/**
 * Collect all results into a zip file. Send back the zip result when all reports got generated
 */
class ZipReportResultCollector(reportSystem: ActorRef) extends Actor with ActorLogging {

  var origSender: Option[ActorRef] = None
  val zipBuilder: ZipBuilder = new ZipBuilder
  var errors: Seq[ReportError] = Seq()

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, _, Left(error)) =>
      errors = errors :+ error
    case SingleReportResult(id, _, Right(result: ReportResultWithDocument)) =>
      log.debug(s"Add Zip Entry:${result.name}")
      zipBuilder.addZipEntry(result.name, result.document) match {
        case Success(r) =>
        case Failure(error) =>
          log.warning(s"Coulnd't att document to  zip file:$error")
          errors = errors :+ ReportError(Some(id), s"Dokument konnte nicht zum Zip hinzugefügt werde:$error")
      }
    case result: GenerateReportsStats =>
      //finished, send back zip result
      val zip = zipBuilder.close()
      origSender map (_ ! ZipReportResult(result, errors, zip))
      self ! PoisonPill
  }
}