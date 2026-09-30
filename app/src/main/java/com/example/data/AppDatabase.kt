package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val matricule: String = "EMP-0001",
    val fullName: String,
    val passwordHash: String,
    val jobTitle: String = "Collaborateur",
    val department: String = "Direction & Administration",
    val phone: String = "",
    val hireDate: String = "01 Janvier 2024",
    val officeLocation: String = "Bureau Central",
    val avatarUrl: String = "",
    val paidLeaveAllowance: Int = 25,
    val paidLeaveUsed: Int = 0,
    val rttAllowance: Int = 10,
    val rttUsed: Int = 0
)

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey val id: String,
    val employeeEmail: String,
    val employeeName: String,
    val department: String,
    val leaveType: String,
    val category: String = "STANDARD",
    val startDate: String,
    val endDate: String,
    val startDay: Int,
    val endDay: Int,
    val month: Int, // 0-indexed Calendar.MONTH (ex: 9 for Oct)
    val year: Int,
    val daysCount: Int,
    val reason: String,
    val attachmentName: String? = null,
    val attachmentUri: String? = null,
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val decisionAt: Long? = null,
    val adminComment: String? = null,
    val alertDismissedByEmployee: Boolean = false
)

@Entity(tableName = "app_alerts")
data class AppAlert(
    @PrimaryKey val id: String,
    val targetUserEmail: String,
    val title: String,
    val message: String,
    val type: String, // "SUBMITTED", "APPROVED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isPopupShown: Boolean = false
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) AND TRIM(passwordHash) = TRIM(:password) LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): User?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(matricule)) = LOWER(TRIM(:matricule)) LIMIT 1")
    suspend fun getUserByMatricule(matricule: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("DELETE FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email))")
    suspend fun deleteUserByEmail(email: String)
}

@Dao
interface LeaveRequestDao {
    @Query("SELECT * FROM leave_requests ORDER BY createdAt DESC")
    fun getAllRequestsFlow(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests ORDER BY createdAt DESC")
    suspend fun getAllRequests(): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests WHERE LOWER(TRIM(employeeEmail)) = LOWER(TRIM(:email)) ORDER BY createdAt DESC")
    fun getRequestsByEmployeeFlow(email: String): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE LOWER(TRIM(employeeEmail)) = LOWER(TRIM(:email)) ORDER BY createdAt DESC")
    suspend fun getRequestsByEmployee(email: String): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequestsFlow(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingRequests(): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED' AND month = :month AND year = :year ORDER BY startDay ASC")
    suspend fun getApprovedRequestsByMonth(month: Int, year: Int): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED' ORDER BY year DESC, month DESC, startDay ASC")
    fun getAllApprovedRequestsFlow(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): LeaveRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: LeaveRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<LeaveRequestEntity>)

    @Update
    suspend fun updateRequest(request: LeaveRequestEntity)

    @Query("UPDATE leave_requests SET status = :status, decisionAt = :decisionAt, adminComment = :comment WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, decisionAt: Long, comment: String?)

    @Query("UPDATE leave_requests SET alertDismissedByEmployee = 1 WHERE id = :id")
    suspend fun dismissAlert(id: String)

    @Delete
    suspend fun deleteRequest(request: LeaveRequestEntity)

    @Query("DELETE FROM leave_requests WHERE id LIKE 'seed-%' OR id LIKE 'sample_%' OR id LIKE 'REQ-DEMO-%'")
    suspend fun deleteDemoRequests()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM app_alerts ORDER BY timestamp DESC")
    suspend fun getAllAlerts(): List<AppAlert>

    @Query("SELECT * FROM app_alerts WHERE LOWER(TRIM(targetUserEmail)) = LOWER(TRIM(:email)) ORDER BY timestamp DESC")
    fun getAlertsForUserFlow(email: String): Flow<List<AppAlert>>

    @Query("SELECT * FROM app_alerts WHERE LOWER(TRIM(targetUserEmail)) = LOWER(TRIM(:email)) AND isPopupShown = 0 ORDER BY timestamp DESC")
    suspend fun getPendingPopupsForUser(email: String): List<AppAlert>

    @Query("SELECT COUNT(*) FROM app_alerts WHERE LOWER(TRIM(targetUserEmail)) = LOWER(TRIM(:email)) AND isRead = 0")
    fun getUnreadCountFlow(email: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AppAlert)

    @Query("UPDATE app_alerts SET isPopupShown = 1 WHERE id = :id")
    suspend fun markAlertPopupShown(id: String)

    @Query("UPDATE app_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAlertRead(id: String)

    @Query("UPDATE app_alerts SET isRead = 1 WHERE LOWER(TRIM(targetUserEmail)) = LOWER(TRIM(:email))")
    suspend fun markAllAlertsRead(email: String)

    @Query("DELETE FROM app_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: String)

    @Query("DELETE FROM app_alerts WHERE LOWER(TRIM(targetUserEmail)) = LOWER(TRIM(:email))")
    suspend fun deleteAllAlertsForUser(email: String)

    @Query("DELETE FROM app_alerts WHERE id LIKE 'seed-%' OR id LIKE 'demo-%' OR id LIKE 'ALERT-DEMO-%'")
    suspend fun deleteDemoAlerts()
}

@Database(
    entities = [User::class, LeaveRequestEntity::class, AppAlert::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timeoff_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
