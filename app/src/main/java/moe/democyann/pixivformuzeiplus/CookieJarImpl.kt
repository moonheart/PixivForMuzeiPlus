package moe.democyann.pixivformuzeiplus

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJarImpl(val cookieStore: CookieStore) : CookieJar {
    /**
     * Load cookies from the jar for an HTTP request to `url`. This method returns a possibly
     * empty list of cookies for the network request.
     *
     *
     * Simple implementations will return the accepted cookies that have not yet expired and that
     * [match][Cookie.matches] `url`.
     */
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        return cookieStore.get(url)
    }

    /**
     * Saves `cookies` from an HTTP response to this store according to this jar's policy.
     *
     *
     * Note that this method may be called a second time for a single HTTP response if the response
     * includes a trailer. For this obscure HTTP feature, `cookies` contains only the trailer's
     * cookies.
     */
    override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
        cookieStore.set(url, cookies)
    }
}