package studio.atopthehill.osom.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "app_usage",
        foreignKeys =
                [
                        ForeignKey(
                                entity = AppInfo::class,
                                parentColumns = ["package_name"],
                                childColumns = ["package_name"],
                                onDelete =
                                        ForeignKey.CASCADE // If an AppInfo record is deleted, its
                                // associated AppUsage records are also
                                // deleted.
                                )],
        indices = [Index(value = ["package_name"]) // Index on package_name for faster queries
                ]
)
data class AppUsage(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0, // Auto-generated primary key for the usage entry
        @ColumnInfo(name = "package_name")
        val packageName: String, // Foreign key referencing AppInfo.package_name
        @ColumnInfo(name = "timestamp")
        val timestamp: Long = System.currentTimeMillis(), // Timestamp of when the app was opened
        @ColumnInfo(name = "reason")
        val reason: String // Reason provided by the user for opening the app
)
