package ch.openolitor.core.repositories

import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.eventsourcing.PersistenceDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.core.models.PersistenceMessage

trait CoreRepositoryQueries extends LazyLogging with CoreDBMappings with PersistenceDBMappings {
  lazy val persistenceJournal = persistenceJournalMapping.syntax("persistence")
  lazy val persistenceMeta = persistenceMetadataMapping.syntax("persistenceMeta")

  protected def queryPersistenceJournalQuery(limit: Int, filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(persistenceJournalMapping as persistenceJournal)
        .innerJoin(persistenceMetadataMapping as persistenceMeta).on(persistenceJournal.persistenceKey, persistenceMeta.persistenceKey)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, persistenceJournal))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, persistenceMeta, Seq("sequence_nr")))
        .orderBy(persistenceJournal.sequenceNr.desc)
        .limit(limit)
    }.map(persistenceJournalMapping(persistenceJournal)).list
  }

  protected def queryLatestPersistenceMessageByPersistenceIdQuery = {
    sql"""SELECT l.persistence_id, l.persistence_key, l.sequence_nr, j.message FROM
      persistence_journal j INNER JOIN (
        SELECT j.persistence_key, m.persistence_id, max(j.sequence_nr) sequence_nr
          FROM persistence_journal j JOIN persistence_metadata m ON j.persistence_key=m.persistence_key group by j.persistence_key, m.persistence_id) l
        ON j.persistence_key=l.persistence_key AND j.sequence_nr=l.sequence_nr
          """.map { rs =>
      val persistenceId = rs.string("persistence_id")
      val persistenceKey = rs.long("persistence_key")
      val seqNr = rs.long("sequence_nr")
      val message = persistentEventBinder.apply(rs.underlying, "message")
      logger.debug(s"Get latest message per persistenceId:$persistenceId, sequenceNr: $seqNr, message:$message")
      PersistenceMessage(persistenceId, seqNr, message)
    }.list
  }
}