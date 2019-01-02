package moe.democyann.pixivformuzeiplus.db

import androidx.room.*

@Entity(tableName = "image")
class Image(
        @PrimaryKey var Id: String,
        var Info: String,
        var lastupdate: Long
)

@Dao
interface ImageDao {

    @Query("select * from image where Id=:illustId limit 1")
    fun getById(illustId:String):Image?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(image:Image)
}


