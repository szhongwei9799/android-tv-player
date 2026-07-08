package com.multimediaplayer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.multimediaplayer.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Media::class,
        Tag::class,
        MediaTagCrossRef::class,
        Playlist::class,
        PlaylistItem::class,
        PlaylistTag::class,
        ScheduledTask::class,
        DisplaySettings::class,
        MediaAudioOverlay::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun tagDao(): TagDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_player_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        initializeDefaultData(database)
                    }
                }
            }

            suspend fun initializeDefaultData(database: AppDatabase) {
                // 创建默认"未分类"标签
                val tagDao = database.tagDao()
                tagDao.insertTag(
                    Tag(
                        name = "未分类",
                        color = "#999999"
                    )
                )

                // 创建默认显示设置
                database.taskDao().insertDisplaySettings(
                    DisplaySettings()
                )
            }
        }
    }
}
