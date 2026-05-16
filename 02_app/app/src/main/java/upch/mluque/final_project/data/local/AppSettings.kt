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
    val voiceSpeed: Float = 1.0f,
    val entrepreneurTips: Map<String, String> = emptyMap(),
    val mapSummary: Map<String, String> = emptyMap(),
    val entrepreneurTipsAudio: String = "",
    val mapSummaryAudio: String = "",
    val profilePicture: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppSettings

        if (id != other.id) return false
        if (language != other.language) return false
        if (businessName != other.businessName) return false
        if (businessCategory != other.businessCategory) return false
        if (isOnboardingCompleted != other.isOnboardingCompleted) return false
        if (voiceSpeed != other.voiceSpeed) return false
        if (entrepreneurTips != other.entrepreneurTips) return false
        if (mapSummary != other.mapSummary) return false
        if (entrepreneurTipsAudio != other.entrepreneurTipsAudio) return false
        if (mapSummaryAudio != other.mapSummaryAudio) return false
        if (profilePicture != null) {
            if (other.profilePicture == null) return false
            if (!profilePicture.contentEquals(other.profilePicture)) return false
        } else if (other.profilePicture != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + language.hashCode()
        result = 31 * result + businessName.hashCode()
        result = 31 * result + businessCategory.hashCode()
        result = 31 * result + isOnboardingCompleted.hashCode()
        result = 31 * result + voiceSpeed.hashCode()
        result = 31 * result + entrepreneurTips.hashCode()
        result = 31 * result + mapSummary.hashCode()
        result = 31 * result + entrepreneurTipsAudio.hashCode()
        result = 31 * result + mapSummaryAudio.hashCode()
        result = 31 * result + (profilePicture?.contentHashCode() ?: 0)
        return result
    }
}
