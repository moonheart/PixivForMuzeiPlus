package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import moe.democyann.pixivformuzeiplus.db.*
import okhttp3.Request
import java.lang.Exception
import java.util.*
import com.google.gson.annotations.SerializedName
import okhttp3.Response


class PixivRecommendMode(val context: Context) : PixivService(context) {

    private val TAG = "PixivRecommendMode"

    /**
     * 获取图片
     */
    override fun getImages(): List<PixivImage> {
        return try {
            Login()
            doGetImages()
        } catch (e: LoginExpiredException) {
            Log.w(TAG, "登陆过期，重新登录")
            Login(true)
            doGetImages()
        }
    }

    private fun doGetImages(): List<PixivImage> {
        val body = getRecommenderJson()
        val recommenderResponse = Gson().fromJson(body, RecommenderResponse::class.java)
        if (recommenderResponse?.recommendations != null && recommenderResponse.recommendations.isNotEmpty()) {
            val list = mutableListOf<PixivImage>()
            val recommenderlist = recommenderResponse.recommendations.shuffled().take(5)
            for (item in recommenderlist) {
                val image = PixivImageInfoResolver(context).Resolve(item)
                if (image != null)
                    list.add(image)
            }
            return list
        }
        return emptyList()
    }

    private fun getRecommenderJson(): String? {
        val lastupdate = db.infoDao().getStringByKey("lastupdate_recommenderjson")
        var recommenderjson = db.infoDao().getStringByKey("recommenderjson")
        if (lastupdate.isNullOrEmpty() // 上次更新时间未设置
                || lastupdate.toLong() + Constants.Time.HOUR < System.currentTimeMillis() // 距离上次更新时间超过阈值
                || recommenderjson.isNullOrEmpty() // 缓存在数据库中的JSON没有值
        ) {
            Log.d(TAG, "从网络获取 recommenderjson")
            val request = Request.Builder()
                    .url(URL_RECOMMENDER + db.infoDao().token)
                    .header("Referer", "https://www.pixiv.net/discovery")
                    .header("Accept", "*/*")
                    .header("User-Agent", USER_AGENT)
                    .build()
            httpClient.newCall(request).execute().use {
                if (!it.isSuccessful) {
                    if (it.code() == 403) {
                        throw LoginExpiredException()
                    }
                    return null
                }
                recommenderjson = it.body()!!.string()
                Log.d(TAG, "recommenderjson: $recommenderjson")
                db.infoDao().upsert(Info("recommenderjson", recommenderjson))
                db.infoDao().upsert(Info("lastupdate_recommenderjson", System.currentTimeMillis().toString()))
            }
        } else {
            var lastupdateStr = "未知"
            if (!lastupdate.isNullOrEmpty()) {
                lastupdateStr = Date(lastupdate.toLong()).toString()
            }
            Log.d(TAG, "从缓存获取 recommenderjson， 上次更新时间：$lastupdateStr")
        }
        Log.d(TAG, "recommenderjson: $recommenderjson")
        return recommenderjson
    }

}

data class RecommenderResponse(
        @SerializedName("recommendations")
        val recommendations: List<String>
)