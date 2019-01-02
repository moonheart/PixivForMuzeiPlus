package moe.democyann.pixivformuzeiplus

import android.net.Uri
import android.util.Log
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import com.google.android.apps.muzei.api.provider.ProviderContract
import com.google.gson.Gson
import java.io.InputStream

class PixivProvider : MuzeiArtProvider() {

    companion object {
        val TAG = "PixivProvider"
    }

    override fun onLoadRequested(initial: Boolean) {
        val modeMap = mapOf(
                Pair(0, PixivRankMode(context)),
                Pair(1, PixivRecommendMode(context))
        )

        val method = ConfigManger(context).method


        val images: List<PixivImage> =
                try {
                    modeMap[method]!!.getImages()
                } catch (e: PixivException) {
                    e.printStackTrace()
                    Log.w(TAG, "fallback to ranking mode")
                    PixivRankMode(context).getImages()
                }

        Log.d(TAG, "获取到${images.size.toString()}")

        images.forEach {
            val artwork = com.google.android.apps.muzei.api.provider.Artwork.Builder()
                    .token(it.token)
                    .title(it.title)
                    .byline(it.author)
                    .attribution(it.description)
                    .webUri(Uri.parse(it.pixivUrl))
//                    .persistentUri(PixivImageDownloader.instance(context).download(it.url_original, it.illustId))
                    .metadata(Gson().toJson(it))
                    .build()
            ProviderContract.getProviderClient(context, PixivProvider::class.java).addArtwork(artwork)
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        Log.d(TAG, "open file for ${artwork.title} ${artwork.token}")
//        if(artwork.data!=null){
//            return artwork.data.inputStream()
//        }
        val image = Gson().fromJson(artwork.metadata, PixivImage::class.java)
        val file = PixivImageDownloader.instance(context).downloadReturnFile(image.url_original, image.illustId)
        return file!!.inputStream()
    }
}