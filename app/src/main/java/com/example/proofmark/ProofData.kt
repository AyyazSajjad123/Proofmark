package com.example.proofmark.core.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class ProofStatus {
    QUEUED, RUNNING, SYNCED, FAILED, DELETED
}

@Entity(tableName = "proofs")
data class ProofEntity(
    @PrimaryKey val id: String,                     // proof:<timestamp> or file stem
    val inputPath: String,
    val outputPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ProofStatus = ProofStatus.QUEUED,
    // telemetry
    val overlayMs: Long? = null,
    val compressMs: Long? = null,
    val hashMs: Long? = null,
    val chainTotalMs: Long? = null,
    val sha256: String? = null,
)

@Dao
interface ProofDao {
    @Query("SELECT * FROM proofs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProofEntity>>

    @Query("SELECT * FROM proofs WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun observeByStatuses(statuses: List<ProofStatus>): Flow<List<ProofEntity>>

    @Query("SELECT * FROM proofs WHERE id = :id")
    suspend fun get(id: String): ProofEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ProofEntity)

    @Query("UPDATE proofs SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: ProofStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE proofs SET outputPath = :out, updatedAt = :now WHERE id = :id")
    suspend fun setOutput(id: String, out: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE proofs SET overlayMs=:overlay, compressMs=:compress, hashMs=:hash, chainTotalMs=:total, sha256=:sha, updatedAt=:now WHERE id = :id")
    suspend fun setTelemetry(
        id: String,
        overlay: Long?, compress: Long?, hash: Long?, total: Long?, sha: String?,
        now: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM proofs WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ProofEntity::class], version = 1, exportSchema = false)
abstract class ProofDb : RoomDatabase() {
    abstract fun dao(): ProofDao
}

object ProofDbProvider {
    @Volatile private var inst: ProofDb? = null
    fun get(context: Context): ProofDb =
        inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(
                context.applicationContext,
                ProofDb::class.java,
                "proofmark.db"
            ).fallbackToDestructiveMigration().build().also { inst = it }
        }
}
