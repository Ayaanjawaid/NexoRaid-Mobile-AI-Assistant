package pw.mng.nexoraid.data

import android.content.Context
import androidx.room.*

@Database(entities = [ChatSession::class, Message::class], version = 2, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                .fallbackToDestructiveMigration() // Simplified for this task
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
