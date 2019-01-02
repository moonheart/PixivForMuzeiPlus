package moe.democyann.pixivformuzeiplus

import android.util.Log
import com.google.common.base.Strings
import moe.democyann.pixivformuzeiplus.db.Info
import moe.democyann.pixivformuzeiplus.db.PixivDatabase
import okhttp3.Cookie
import okhttp3.HttpUrl

class CookieStore {
    val db: PixivDatabase
    val cookieMap: HashMap<String, String>
val TAG = "CookieStore"
    constructor(db: PixivDatabase) {
        this.db = db
        cookieMap = HashMap()
        // 从数据库初始化
        val cookieStr = db.infoDao().getStringByKey("cookie")
        if (!Strings.isNullOrEmpty(cookieStr)) {
            for (s in cookieStr.split(";")) {
                val split = s.split("=")
                cookieMap[split[0]] = split[1]
            }
        }
    }

    /**
     * 获取cookie
     */
    fun get(url: HttpUrl): MutableList<Cookie> {
        val list = mutableListOf<Cookie>()
        if (url.host().endsWith("pixiv.net")) {
            for (cookie in cookieMap) {
                list.add(Cookie.Builder()
                        .name(cookie.key)
                        .value(cookie.value)
                        .domain("pixiv.net").build())
            }
        }
        return list
    }

    /**
     * 设置cookie
     */
    fun set(url: HttpUrl, cookies: MutableList<Cookie>) {
        if (url.host().endsWith("pixiv.net")) {
            for (cookie in cookies) {
                cookieMap[cookie.name()] = cookie.value()
            }
            val cookieStr = cookieMap.map { cookie -> "${cookie.key}=${cookie.value}" }.joinToString()
            Log.d(TAG, cookieStr)
            db.infoDao().upsert(Info("cookie", cookieStr))
        }
    }
}