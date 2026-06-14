package upch.mluque.final_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String = "",
    val nationality: String,
    val nationalityFlag: String,
    val selectedProducts: List<SelectedProduct> = emptyList(),
    val subtotal: Double = 0.0,
    val discountValue: Double = 0.0,
    val discountType: DiscountType = DiscountType.FIXED,
    val totalAmount: Double = 0.0,
    val registrationDate: Long = System.currentTimeMillis(),
    val isSent: Boolean = false,
    val sentDate: Long? = null
)
