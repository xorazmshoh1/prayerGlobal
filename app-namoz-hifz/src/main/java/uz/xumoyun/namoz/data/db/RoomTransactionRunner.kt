package uz.xumoyun.namoz.data.db

import androidx.room.withTransaction
import uz.xumoyun.feature.prayer.tracker.domain.TransactionRunner
import javax.inject.Inject

/** [TransactionRunner] ning Room amalga oshirilishi (withTransaction). */
class RoomTransactionRunner @Inject constructor(private val db: NamozDatabase) : TransactionRunner {
    override suspend fun <T> atomic(block: suspend () -> T): T = db.withTransaction { block() }
}
