package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.annotations.SerializedName
import moe.democyann.pixivformuzeiplus.db.*
import java.util.*


/**
 * 抽象的Pixiv抓图服务，实现自定义的抓图方式需要继承此类
 */
abstract class PixivService {

    companion object {
        val URL_PIXIV_HOME = "https://www.pixiv.net"
        val URL_LOGIN_PAGE = "https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index"
        val URL_LOGIN_API = "https://accounts.pixiv.net/api/login?lang=zh"
        val URL_RECOMMENDER = "https://www.pixiv.net/rpc/recommender.php?type=illust&sample_illusts=auto&num_recommendations=500&page=discovery&tt="
        val RECOMM_URL_ANDROID = "https://app-api.pixiv.net/v1/illust/recommended?filter=for_android"
        val ILLUST_URL = "https://www.pixiv.net/rpc/illust_list.php?verbosity=&exclude_muted_illusts=1&illust_ids="
        val DETA_URL = "https://app-api.pixiv.net/v1/illust/detail?illust_id="
        val BOOK_URL = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=public&user_id="
        val BOOK_URL_WWW = "https://www.pixiv.net/bookmark.php?id=%s&p=%s"
        val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " + "Chrome/42.0.2311.152 Safari/537.36"
        val USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Mobile Safari/537.36"
    }

    private val TAG = "PixivService";


    private val context: Context
    protected val db: PixivDatabase
    protected val httpClient: OkHttpClient
    protected val configManger: ConfigManger

    constructor(context: Context) {
        this.context = context
        this.db = PixivDatabase.instance(context)
        httpClient = OkHttpClientWraper.getClient(context)
        configManger = ConfigManger(context)
    }

    /**
     * 获取图片
     */
    abstract fun getImages(): List<PixivImage>

    /**
     * 登录
     * @param force 忽略缓存强制登录
     */
    protected fun Login(force: Boolean = false) {
        // 检查是否需要重新登陆
        val lastupdateStr = db.infoDao().getStringByKey("lastupdate_login")
        Log.d(TAG, "lastupdateStr: ${if (lastupdateStr.isNullOrBlank()) lastupdateStr else Date(lastupdateStr.toLong() + Constants.Time.HOUR).toString()}")
        val token1 = db.infoDao().token
        Log.d(TAG, "token1: $token1")
        val user_id1 = db.infoDao().user_id
        Log.d(TAG, "user_id1: $user_id1")
        if (!token1.isNullOrEmpty()
                && !user_id1.isNullOrEmpty()
                && !lastupdateStr.isNullOrEmpty()
                && lastupdateStr.toLong() + Constants.Time.HOUR > System.currentTimeMillis()
                && !force
        ) {
            Log.d(TAG, "不需要重新登录")
            return
        }

        if (configManger.username.isNullOrEmpty() || configManger.password.isNullOrEmpty()) {
            throw PixivException("登录失败：未设置用户名或密码")
        }

        val request = Request.Builder()
                .url(URL_LOGIN_PAGE)
                .get()
                .build()
        val response = httpClient.newCall(request).execute()
        // 如果不需要重新登陆 会自动重定向到首页
        if (!response.isRedirect) {
            if (!response.isSuccessful) throw PixivException("登录失败：无法访问登录页面")
            val html = response.body()!!.string()
            val matchResult = Regex("name=\"post_key\"\\svalue=\"([a-z0-9]{32})\"").find(html)
//            Log.d(TAG, "html:$html")
            val (post_key) = matchResult!!.destructured
            Log.d(TAG, "post_key: $post_key")
            if (post_key.isNullOrEmpty()) throw PixivException("登录失败：未找到post_key")

            val formBody = FormBody.Builder()
                    .add("pixiv_id", configManger.username)
                    .add("password", configManger.password)
                    .add("captcha", "g_recaptcha_response")
                    .add("post_key", post_key)
                    .add("source", "pc")
                    .add("ref", "wwwtop_accounts_index")
                    .add("return_to", "http://www.pixiv.net/")
                    .build()
            val loginRequest = Request.Builder()
                    .url(URL_LOGIN_API)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Referer", URL_LOGIN_PAGE)
                    .post(formBody)
                    .build()
            val response1 = httpClient.newCall(loginRequest).execute()
            if (!response1.isSuccessful) throw PixivException("登录失败：登录接口请求失败")
            val json = response1.body()!!.string()
            val (_, _, body) = Gson().fromJson(json, LoginResponse::class.java)
            if (body?.success == null) throw PixivException("登录失败：$json")
        }

        val response2 = httpClient.newCall(Request.Builder().url(URL_PIXIV_HOME).build()).execute()
        val html = response2.body()!!.string()

        val (token) = Regex("pixiv.context.token\\s=\\s\"([a-z0-9]{32})\"").find(html)!!.destructured
        Log.d(TAG, "token: $token")
        if (token.isNullOrEmpty()) throw PixivException("登录失败：获取token失败")

        val (user_id) = Regex("pixiv.user.id\\s=\\s\"(\\d+)\"").find(html)!!.destructured
        Log.d(TAG, "user_id: $user_id")
        if (user_id.isNullOrEmpty()) throw PixivException("登录失败：获取userid失败")

        db.infoDao().token = token
        db.infoDao().user_id = user_id

        val upsert = db.infoDao().upsert("lastupdate_login", System.currentTimeMillis().toString())
        Log.d(TAG, "upsert: $upsert")
    }


    data class LoginResponse(
            @SerializedName("error")
            val error: Boolean,
            @SerializedName("message")
            val message: String,
            @SerializedName("body")
            val body: Body?
    ) {
        data class Body(
                @SerializedName("success")
                val success: Success?
        ) {
            data class Success(
                    @SerializedName("return_to")
                    val returnTo: String
            )
        }
    }



}

