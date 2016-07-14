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

object HeadReportResultCollector {
  def props(reportSystem: ActorRef): Props = Props(classOf[HeadReportResultCollector], reportSystem)
}

/**
 * after sending report request to reportsystem wait for only for first reportresult and send that back to the sender
 */
class HeadReportResultCollector(reportSystem: ActorRef) extends Actor with ActorLogging {

  var origSender: Option[ActorRef] = None

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case result: SingleReportResult =>
      origSender map (_ ! result)
      self ! PoisonPill
  }

}