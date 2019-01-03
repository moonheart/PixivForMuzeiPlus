package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import moe.democyann.pixivformuzeiplus.db.Image
import moe.democyann.pixivformuzeiplus.db.PixivDatabase
import okhttp3.OkHttpClient
import okhttp3.Request

class PixivImageInfoResolver {

    private val TAG = "PixivImageInfoResolver"
    protected val httpClient: OkHttpClient
    protected val db: PixivDatabase

    constructor(context: Context) {
        this.db = PixivDatabase.instance(context)
        httpClient = OkHttpClientWraper.getClient(context)
    }

    fun Resolve(id: String): PixivImage? {
        return getInfoFromMobilePage(id)
    }

    /**
     * 获取图片详细信息
     */
    private fun getInfoFromMobilePage(id: String): PixivImage? {
        val dbImage = db.imageDao().getById(id)
        var json = ""
        if (dbImage == null || dbImage.Info.isNullOrEmpty() || dbImage.lastupdate + Constants.Time.HOUR < System.currentTimeMillis()) {
            val url = "https://www.pixiv.net/touch/ajax/illust/details?illust_id=$id"
            val request = Request.Builder()
                    .url(url)
                    .header("Referer", "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=$id")
//                    .header("Accept-Encoding", "gzip")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("User-Agent", PixivService.USER_AGENT_MOBILE)
                    .build()
            val response = httpClient.newCall(request).execute()
//            Log.d(TAG, "headers: ${response.headers().toString()}")
            json = response.body()!!.string()
            db.imageDao().upsert(Image(id, json, System.currentTimeMillis()))
        } else {
            json = dbImage.Info
        }
        LogHelper.writeLongLog(TAG, "json: $json")
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
        var description = illust.comment ?: ""
        val indexOf = description.indexOf("\\r\\n")
        if (indexOf > 0) {
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

    private fun getArtworkToken(url: String): String {
        val split = url.split("/")
        return split[split.size - 1].replace(".jpg", ".png")
    }


    private fun getDetailUrl(illustId: String): String {
        return "http://www.pixiv.net/member_illust.php?mode=medium&illust_id=$illustId"
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
}
