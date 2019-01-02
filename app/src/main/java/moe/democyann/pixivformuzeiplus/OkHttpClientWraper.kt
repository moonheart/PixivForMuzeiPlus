package moe.democyann.pixivformuzeiplus

import android.content.Context
import moe.democyann.pixivformuzeiplus.db.PixivDatabase
import okhttp3.OkHttpClient

class OkHttpClientWraper {
    companion object {
        private var httpClient: OkHttpClient? = null
        fun getClient(context: Context): OkHttpClient {
            if (httpClient == null) {
                httpClient = OkHttpClient.Builder()
                        .cookieJar(CookieJarImpl(CookieStore(PixivDatabase.instance(context))))
                        .followRedirects(false)
                        .build()
            }
            return httpClient!!
        }
    }
}