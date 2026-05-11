package upch.mluque.final_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val language: String = "Español",
    val businessName: String = "",
    val businessCategory: String = "",
    val isOnboardingCompleted: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val voiceSpeed: Float = 1.0f
)
