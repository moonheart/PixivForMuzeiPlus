package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class PixivImageDownloader(context: Context) {

    companion object {
        private var downloader: PixivImageDownloader? = null
        fun instance(context: Context): PixivImageDownloader {
            if (downloader == null) {
                downloader = PixivImageDownloader(context)
            }
            return downloader as PixivImageDownloader
        }
    }


    private val TAG = "PixivImageDownloader"


    private val httpClient = OkHttpClientWraper.getClient(context)

    fun downloadReturnUri(url: String, illustId: String): Uri? {
        return Uri.fromFile(downloadReturnFile(url, illustId))
    }

    fun downloadReturnFile(url: String, illustId: String): File? {
        val imageFile = File(getPixivCacheDir(), getFilename(url))
        if (!imageFile.exists()) {
            val refer = "https://www.pixiv.net/member_illust.php?mode=big&illust_id=${illustId}";
            var response = httpClient.newCall(buildDownloadRequest(url, refer)).execute()
            Log.d(TAG, "download image status code: ${response.code()}")
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    response = httpClient.newCall(buildDownloadRequest(url.replace(".png", ".jpg"), refer)).execute()
                } else if (response.code() == 403) {
                    Log.w(TAG, "download image 403")
                }
            }
            if (!response.isSuccessful) return null

            FileOutputStream(imageFile).use {
                response.body()!!.byteStream().copyTo(it)
            }
            response.close()
        } else {
            Log.d(TAG, "文件已存在：${imageFile.toURI()}")
        }
        return imageFile
    }

    private fun buildDownloadRequest(url: String, referer: String): Request {
        return Request.Builder()
                .url(url)
                //.header("User-agent", USER_AGENT_MOBILE)
                .header("Referer", referer)
                .get()
                .build()
    }

    private fun getFilename(url: String): String {
        val split = getOriginalUrl(url).split("/")
        return split[split.size - 1]
    }


    /**
     * 获取原图URL
     * @param imgurl
     * @return
     */
    private fun getOriginalUrl(imgurl: String): String {
        // 略缩：https://i.pximg.net/c/150x150_90/img-master/img/2016/11/05/21/07/57/59813504_p0_master1200.jpg
        // 高清：https://i.pximg.net/img-master/img/2016/11/05/21/07/57/59813504_p0_master1200.jpg
        // 原图：https://i.pximg.net/img-original/img/2016/11/05/21/07/57/59813504_p0.png
        // 或者：https://i.pximg.net/img-original/img/2010/08/30/00/32/39/12904418_p0.jpg
        //        Log.d(TAG, imgurl);
        var big = Pattern.compile("/c/[0-9]+x[0-9]+/img-master").matcher(imgurl).replaceFirst("/img-original")
        big = Pattern.compile("/c/[0-9]+x[0-9]+_\\d+/img-master").matcher(big).replaceFirst("/img-original")
        big = Pattern.compile("\\_master[0-9]+\\.(jpg|png)", Pattern.CASE_INSENSITIVE).matcher(big).replaceFirst(".png")
        //        Log.d(TAG, String.format("%s -> %s", imgurl, big));
        return big
    }

    private fun getPixivCacheDir(): File {
        val savePath = "/pixiv/"
        // 创建外置缓存文件夹
        val pPath = File(Environment.getExternalStorageDirectory().toString() + savePath)
        if (pPath.exists()) {
            if (pPath.isFile) {
                pPath.delete()
                pPath.mkdir()
            }
        } else {
            pPath.mkdir()
        }
        return pPath
    }


}