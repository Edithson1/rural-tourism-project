package yupay.turismo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AppSettings::class, Visit::class, Product::class, PendingOp::class, TtsPreference::class],
    version = 12
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun visitDao(): VisitDao
    abstract fun productDao(): ProductDao
    abstract fun pendingOpDao(): PendingOpDao
    // tts_preferences: modelo de voz activo por idioma (v12). Con fallbackToDestructiveMigration
    // no hace falta migración manual (las tablas se recrean si cambia el esquema).
    abstract fun ttsPreferenceDao(): TtsPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
