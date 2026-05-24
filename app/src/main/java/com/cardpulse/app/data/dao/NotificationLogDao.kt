package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.NotificationLog

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_log ORDER BY sentAt DESC LIMIT 50")
    suspend fun getRecentNotifications(): List<NotificationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NotificationLog)

    @Query("UPDATE notification_log SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM notification_log")
    suspend fun deleteAllNotifications()
}
