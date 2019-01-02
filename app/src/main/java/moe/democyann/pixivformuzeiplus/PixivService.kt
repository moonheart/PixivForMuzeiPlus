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

    protected val SENCOND = 1000;
    protected val MINUTE = SENCOND * 60;
    protected val HOUR = MINUTE * 60;
    protected val DAY = HOUR * 24;

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


    protected fun getArtworkToken(url: String): String {
        val split = url.split("/")
        return split[split.size - 1].replace(".jpg", ".png")
    }


    protected fun getDetailUrl(illustId: String): String {
        return "http://www.pixiv.net/member_illust.php?mode=medium&illust_id=$illustId"
    }

    /**
     * 登录
     * @param force 忽略缓存强制登录
     */
    protected fun Login(force: Boolean = false) {
        // 检查是否需要重新登陆
        val lastupdateStr = db.infoDao().getStringByKey("lastupdate_login")
        Log.d(TAG, "lastupdateStr: ${if (lastupdateStr.isNullOrBlank()) lastupdateStr else Date(lastupdateStr.toLong() + HOUR).toString()}")
        val token1 = db.infoDao().token
        Log.d(TAG, "token1: $token1")
        val user_id1 = db.infoDao().user_id
        Log.d(TAG, "user_id1: $user_id1")
        if (!token1.isNullOrEmpty()
                && !user_id1.isNullOrEmpty()
                && !lastupdateStr.isNullOrEmpty()
                && lastupdateStr.toLong() + HOUR > System.currentTimeMillis()
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


    /**
     * 获取图片详细信息
     */
    protected fun getInfoFromMobilePage(id: String): PixivImage? {
        val dbImage = db.imageDao().getById(id)
        var json = ""
        if (dbImage == null || dbImage.Info.isNullOrEmpty() || dbImage.lastupdate + HOUR < System.currentTimeMillis()) {
            val url = "https://www.pixiv.net/touch/ajax/illust/details?illust_id=$id"
            val request = Request.Builder()
                    .url(url)
                    .header("Referer", "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=$id")
//                    .header("Accept-Encoding", "gzip")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("User-Agent", USER_AGENT_MOBILE)
                    .build()
            val response = httpClient.newCall(request).execute()
//            Log.d(TAG, "headers: ${response.headers().toString()}")
            json = response.body()!!.string()
            db.imageDao().upsert(Image(id, json, System.currentTimeMillis()))
        } else {
            json = dbImage.Info
        }
        writeLongLog("json: $json")
        val detailResponse = Gson().fromJson(json, ImageDetailResponse::class.java)
        if (detailResponse == null || !detailResponse.isSucceed) {
            Log.w(TAG, "获取图片详情失败：json: $json")
            return null
        }
        val illust = detailResponse.illustDetails

        val image = PixivImage()
        image.illustId = illust.id
        image.title = illust.title
        image.author = illust.authorDetails.userName
        var description = illust.comment?:""
        val indexOf = description.indexOf("\\r\\n")
        if(indexOf>0){
            description = description.substring(0, indexOf)
        }
        image.description = description
        image.tags = illust.tags
        image.isR18 = illust.xRestrict == "1"
        image.token = getArtworkToken(illust.urlBig)
        image.url_original = illust.urlBig
        image.height = illust.height.toInt()
        image.width = illust.width.toInt()
        image.bookmark_user_total = illust.bookmarkUserTotal
        image.rating_count = illust.ratingCount.toInt()
        image.rating_view = illust.ratingView.toInt()
        image.authorId = illust.userId
        image.pixivUrl = getDetailUrl(illust.id)
        return image
    }

    protected fun writeLongLog(s: String) {
        return
        if (s.length > 4000) {
            Log.v(TAG, "sb.length = " + s.length)
            val chunkCount = s.length / 4000     // integer division
            for (i in 0..chunkCount) {
                val max = 4000 * (i + 1)
                if (max >= s.length) {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + s.substring(4000 * i))
                } else {
                    Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + s.substring(4000 * i, max))
                }
            }
        } else {
            Log.v(TAG, s.toString())
        }
    }
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

data class ImageDetailResponse(
        @SerializedName("isSucceed")
        val isSucceed: Boolean,
        @SerializedName("illust_details")
        val illustDetails: IllustDetails,
        @SerializedName("author_details")
        val authorDetails: AuthorDetails
) {
    data class IllustDetails(
            @SerializedName("url")
            val url: String,
            @SerializedName("tags")
            val tags: List<String>,
            @SerializedName("illust_images")
            val illustImages: List<IllustImage>,
            @SerializedName("reupload_timestamp")
            val reuploadTimestamp: Boolean,
            @SerializedName("tags_editable")
            val tagsEditable: Boolean,
            @SerializedName("bookmark_user_total")
            val bookmarkUserTotal: Int,
            @SerializedName("url_s")
            val urlS: String,
            @SerializedName("url_ss")
            val urlSs: String,
            @SerializedName("url_big")
            val urlBig: String,
            @SerializedName("url_placeholder")
            val urlPlaceholder: String,
            @SerializedName("ugoira_meta")
            val ugoiraMeta: Any,
            @SerializedName("share_text")
            val shareText: String,
            @SerializedName("is_mypixiv")
            val isMypixiv: Boolean,
            @SerializedName("is_howto")
            val isHowto: Boolean,
            @SerializedName("is_original")
            val isOriginal: Boolean,
            @SerializedName("factoryGoods")
            val factoryGoods: FactoryGoods,
            @SerializedName("is_rated")
            val isRated: Boolean,
            @SerializedName("response_get")
            val responseGet: List<Any>,
            @SerializedName("response_send")
            val responseSend: List<Any>,
            @SerializedName("storable_tags")
            val storableTags: List<String>,
            @SerializedName("upload_timestamp")
            val uploadTimestamp: Int,
            @SerializedName("id")
            val id: String,
            @SerializedName("user_id")
            val userId: String,
            @SerializedName("title")
            val title: String,
            @SerializedName("width")
            val width: String,
            @SerializedName("height")
            val height: String,
            @SerializedName("restrict")
            val restrict: String,
            @SerializedName("x_restrict")
            val xRestrict: String,
            @SerializedName("type")
            val type: String,
            @SerializedName("sl")
            val sl: Int,
            @SerializedName("page_count")
            val pageCount: String,
            @SerializedName("comment")
            val comment: String?,
            @SerializedName("rating_count")
            val ratingCount: String,
            @SerializedName("rating_view")
            val ratingView: String,
            @SerializedName("comment_html")
            val commentHtml: String,
            @SerializedName("author_details")
            val authorDetails: AuthorDetails
    ) {

        data class FactoryGoods(
                @SerializedName("integratable")
                val integratable: Boolean,
                @SerializedName("integrated")
                val integrated: Boolean
        )

        data class IllustImage(
                @SerializedName("illust_image_width")
                val illustImageWidth: String,
                @SerializedName("illust_image_height")
                val illustImageHeight: String
        )

        data class AuthorDetails(
                @SerializedName("user_id")
                val userId: String,
                @SerializedName("user_name")
                val userName: String,
                @SerializedName("user_account")
                val userAccount: String
        )
    }

    data class AuthorDetails(
            @SerializedName("user_id")
            val userId: String,
            @SerializedName("user_status")
            val userStatus: String,
            @SerializedName("user_account")
            val userAccount: String,
            @SerializedName("user_name")
            val userName: String,
            @SerializedName("user_premium")
            val userPremium: String,
            @SerializedName("profile_img")
            val profileImg: ProfileImg,
            @SerializedName("is_followed")
            val isFollowed: Boolean
    ) {
        data class ProfileImg(
                @SerializedName("main")
                val main: String
        )
    }

}