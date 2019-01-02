package moe.democyann.pixivformuzeiplus.db

import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteStatement

@Entity(tableName = "info")
class Info(
        @PrimaryKey val Key: String,
        var Value: String
)

@Dao
interface InfoDao {

    @Query("select * from info where `Key`=:key")
    fun getByKey(key: String): Info

    @Update
    fun update(info: Info): Int

    @Query("update info set Value=:value where `Key`=:key")
    fun update(key: String, value: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(info: Info): Long

    @Query("select `Value` from info where `Key`=:key")
    fun getStringByKey(key: String): String

}


var InfoDao.user_id: String
    get() {
        return getStringByKey("user_id")
    }
    set(value) {
        upsert(Info("user_id", value))
    }

var InfoDao.token: String
    get() {
        return getStringByKey("token")
    }
    set(value) {
        Log.d("XXXXXX", upsert(Info("token", value)).toString())
    }

fun InfoDao.upsert(key: String, value: String): Long {
    return upsert(Info(key, value))
}