package upch.mluque.final_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nationality: String,
    val nationalityFlag: String,
    val priceApprox: String,
    val services: String, // Comma separated list of services
    val registrationDate: Long = System.currentTimeMillis(),
    val isSent: Boolean = false,
    val sentDate: Long? = null
)
