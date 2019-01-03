package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Request
import com.google.gson.annotations.SerializedName
import moe.democyann.pixivformuzeiplus.db.Info
import java.lang.Exception
import java.util.*


class PixivRankMode(val context: Context) : PixivService(context) {
    private val TAG = "PixivRankMode"

    val URL_RANKING = "https://www.pixiv.net/ranking.php?mode=%s&format=json"

    override fun getImages(): List<PixivImage> {
        return try {
            doGetImages()
        } catch (e: LoginExpiredException) {
            Log.w(TAG, "登陆过期，重新登录")
            Login(true)
            doGetImages()
        }
    }

    private fun doGetImages(): List<PixivImage> {
        val body = GetRankingListJson(configManger.ranking_mode)
        try {
            val rankingResult = Gson().fromJson(body, RankingResult::class.java)
            if (!rankingResult.contents.isNullOrEmpty()) {
                val list = mutableListOf<PixivImage>()
                val contents = rankingResult.contents.take(configManger.ranking_mode_count).shuffled().take(5)
                for (content in contents) {
                    Log.d(TAG, "prepare for ${content.illustId}")
                    val image = PixivImageInfoResolver(context).Resolve(content.illustId.toString())
                    if (image != null)
                        list.add(image)
                }
                return list
            }

        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "解析Json出错")
            LogHelper.writeLongLog(TAG, body!!)
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun GetRankingListJson(mode: String): String? {
        Log.d(TAG, "ranking mode: $mode")
        val lastupdate = db.infoDao().getStringByKey("lastupdate_rankinglistjson_$mode")
        var rankinglistjson = db.infoDao().getStringByKey("rankinglistjson_$mode")
        if (lastupdate.isNullOrEmpty() // 上次更新时间未设置
                || lastupdate.toLong() + Constants.Time.HOUR < System.currentTimeMillis() // 距离上次更新时间超过阈值
                || rankinglistjson.isNullOrEmpty() // 缓存在数据库中的JSON没有值
        ) {
            Log.d(TAG, "从网络获取 rankinglistjson_$mode")
            Log.d(TAG, URL_RANKING.format(mode))
            val request = Request.Builder()
                    .get()
                    .url(URL_RANKING.format(mode))
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .build()
            val call = httpClient.newCall(request)

            call.execute().use {
                if (!it.isSuccessful) {
                    if (it.code() == 403) {
                        throw LoginExpiredException()
                    }
                    return null
                }
                rankinglistjson = it.body()!!.string()
                db.infoDao().upsert(Info("rankinglistjson_$mode", rankinglistjson))
                db.infoDao().upsert(Info("lastupdate_rankinglistjson_$mode", System.currentTimeMillis().toString()))
            }

        } else {
            var lastupdateStr = "未知"
            if (!lastupdate.isNullOrEmpty()) {
                lastupdateStr = Date(lastupdate.toLong()).toString()
            }
            Log.d(TAG, "从缓存获取 rankinglistjson_$mode， 上次更新时间：$lastupdateStr")
        }
        return rankinglistjson
    }

}

data class RankingResult(
        @SerializedName("contents")
        val contents: List<Content>,
        @SerializedName("mode")
        val mode: String,
        @SerializedName("content")
        val content: String,
        @SerializedName("page")
        val page: Int,
        @SerializedName("prev")
        val prev: Boolean,
        @SerializedName("next")
        val next: Int,
        @SerializedName("date")
        val date: String,
        @SerializedName("prev_date")
        val prevDate: String,
        @SerializedName("next_date")
        val nextDate: Boolean,
        @SerializedName("rank_total")
        val rankTotal: Int
) {
    data class Content(
            @SerializedName("title")
            val title: String,
            @SerializedName("date")
            val date: String,
            @SerializedName("tags")
            val tags: List<String>,
            @SerializedName("url")
            val url: String,
            @SerializedName("illust_type")
            val illustType: String,
            @SerializedName("illust_book_style")
            val illustBookStyle: String,
            @SerializedName("illust_page_count")
            val illustPageCount: String,
            @SerializedName("user_name")
            val userName: String,
            @SerializedName("profile_img")
            val profileImg: String,
            @SerializedName("illust_content_type")
            val illustContentType: IllustContentType,
            @SerializedName("illust_id")
            val illustId: Int,
            @SerializedName("width")
            val width: Int,
            @SerializedName("height")
            val height: Int,
            @SerializedName("user_id")
            val userId: Int,
            @SerializedName("rank")
            val rank: Int,
            @SerializedName("yes_rank")
            val yesRank: Int,
            @SerializedName("rating_count")
            val ratingCount: Int,
            @SerializedName("view_count")
            val viewCount: Int,
            @SerializedName("illust_upload_timestamp")
            val illustUploadTimestamp: Int,
            @SerializedName("attr")
            val attr: String,
            @SerializedName("is_bookmarked")
            val isBookmarked: Boolean,
            @SerializedName("bookmarkable")
            val bookmarkable: Boolean
    ) {
        data class IllustContentType(
                @SerializedName("sexual")
                val sexual: Int,
                @SerializedName("lo")
                val lo: Boolean,
                @SerializedName("grotesque")
                val grotesque: Boolean,
                @SerializedName("violent")
                val violent: Boolean,
                @SerializedName("homosexual")
                val homosexual: Boolean,
                @SerializedName("drug")
                val drug: Boolean,
                @SerializedName("thoughts")
                val thoughts: Boolean,
                @SerializedName("antisocial")
                val antisocial: Boolean,
                @SerializedName("religion")
                val religion: Boolean,
                @SerializedName("original")
                val original: Boolean,
                @SerializedName("furry")
                val furry: Boolean,
                @SerializedName("bl")
                val bl: Boolean,
                @SerializedName("yuri")
                val yuri: Boolean
        )
    }
}