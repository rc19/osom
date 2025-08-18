package studio.atopthehill.osom.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfo(
        @PrimaryKey
        @ColumnInfo(name = "package_name")
        val packageName: String, // The unique package name of the application
        @ColumnInfo(name = "label") val label: String, // The display name of the application
        @ColumnInfo(name = "icon", typeAffinity = ColumnInfo.BLOB)
        val icon: ByteArray?, // The application icon as a byte array, nullable as it might not be
        // used initially or available
        @ColumnInfo(name = "is_installed")
        val isInstalled: Boolean = true, // Flag to indicate if the app is currently installed
        @ColumnInfo(name = "last_updated")
        val lastUpdated: Long =
                System.currentTimeMillis(), // Timestamp of when this app info was last updated or
// fetched
        @ColumnInfo(name = "is_whitelisted")
        val isWhitelisted: Boolean = false // Flag to indicate if the app is whitelisted for monitoring
) {
    // Overriding equals and hashCode to ensure proper comparison, especially if used in sets or as
    // map keys
    override fun equals(other: Any?): Boolean {
        if (this === other) return true // Check for same instance
        if (javaClass != other?.javaClass) return false // Check for different class

        other as AppInfo // Cast to AppInfo

        if (packageName != other.packageName) return false // Compare package name
        if (label != other.label) return false // Compare label
        if (icon != null) { // Compare icon if not null
            if (other.icon == null) return false // If other icon is null, not equal
            if (!icon.contentEquals(other.icon)) return false // Compare byte array content
        } else if (other.icon != null)
                return false // If this icon is null and other is not, not equal
        if (isInstalled != other.isInstalled) return false // Compare installation status
        if (isWhitelisted != other.isWhitelisted) return false // Compare whitelist status
        // lastUpdated can differ, so it's often excluded from equality checks unless specifically
        // needed for versioning of the entity itself

        return true // All comparable fields are equal
    }

    // Overriding hashCode to be consistent with equals
    override fun hashCode(): Int {
        var result = packageName.hashCode() // Start with package name hash code
        result = 31 * result + label.hashCode() // Combine with label hash code
        result =
                31 * result +
                        (icon?.contentHashCode() ?: 0) // Combine with icon hash code (or 0 if null)
        result = 31 * result + isInstalled.hashCode() // Combine with installation status hash code
        result = 31 * result + isWhitelisted.hashCode() // Combine with whitelist status hash code
        // lastUpdated is often excluded here as well
        return result // Return the final hash code
    }
}
