package upch.mluque.final_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nationality: String,
    val nationalityFlag: String,
    val priceType: String,
    val priceValue: String,
    val priceCurrency: String,
    val services: String, // Comma separated list of services
    val registrationDate: Long = System.currentTimeMillis(),
    val isSent: Boolean = false,
    val sentDate: Long? = null
) {
    fun getFormattedPrice(): String {
        return when (priceType) {
            "Rango" -> "$priceCurrency $priceValue"
            "Fijo" -> "$priceCurrency $priceValue"
            "Personalizado" -> "$priceCurrency $priceValue"
            else -> "$priceCurrency $priceValue"
        }
    }
}
