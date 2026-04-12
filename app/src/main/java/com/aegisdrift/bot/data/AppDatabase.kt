package com.aegisdrift.bot.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val side: String,
    val entryTime: String,
    val entryPrice: Double,
    val stopPrice: Double,
    val exitTime: String?,
    val exitPrice: Double?,
    val exitReason: String?,
    val qty: Double,
    val netPnlUsd: Double?,
    val barsHeld: Int?,
    val status: String   // "OPEN" or "CLOSED"
)

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY id DESC")
    suspend fun getAllTrades(): List<TradeEntity>

    @Query("SELECT * FROM trades WHERE status = 'OPEN' LIMIT 1")
    suspend fun getOpenTrade(): TradeEntity?

    @Insert
    suspend fun insertTrade(trade: TradeEntity): Long

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Query("DELETE FROM trades")
    suspend fun deleteAll()
}

@Database(entities = [TradeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aegis_bot.db"
                ).build().also { INSTANCE = it }
            }
    }
}
