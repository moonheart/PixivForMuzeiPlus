package moe.democyann.pixivformuzeiplus.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = arrayOf(Image::class, Info::class), version = 3)
abstract class PixivDatabase : RoomDatabase() {
    companion object {
        private var db:PixivDatabase? = null
        fun instance(context: Context):PixivDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                        context,
                        PixivDatabase::class.java,
                        "pixiv")
                        .fallbackToDestructiveMigration()
                        .setJournalMode(JournalMode.TRUNCATE)
                        .build()
            }
            return db as PixivDatabase
        }
    }
    abstract fun imageDao(): ImageDao
    abstract fun infoDao(): InfoDao

}