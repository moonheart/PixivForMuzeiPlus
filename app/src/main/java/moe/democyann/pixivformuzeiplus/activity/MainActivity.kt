package moe.democyann.pixivformuzeiplus.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Locale

import moe.democyann.pixivformuzeiplus.R
import moe.democyann.pixivformuzeiplus.settings.Setting

class MainActivity : AppCompatActivity() {

    private var open_btn: Button? = null
    private var setting_btn: Button? = null
    private var tv_1: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verifyStoragePermissions(this)

        open_btn = findViewById<View>(R.id.open_btn) as Button
        setting_btn = findViewById<View>(R.id.setting_btn) as Button
        tv_1 = findViewById<View>(R.id.tv_1) as TextView

        open_btn!!.setOnClickListener {
            try {
                val packageManager = this@MainActivity.packageManager
                val intent: Intent
                intent = packageManager.getLaunchIntentForPackage("net.nurik.roman.muzei")!!
                startActivity(intent)
            } catch (e: Exception) {
                val uri = Uri.parse("market://details?id=net.nurik.roman.muzei")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                startActivity(goToMarket)
            }
        }
        setting_btn!!.setOnClickListener {
            val intent = Intent(this@MainActivity, Setting::class.java)
            startActivity(intent)
        }

        val arr = arrayOf("KP", "PRK", "408", "KR", "KOR", "410", "ko", "kor")
        val ct = Locale.getDefault().country
        val lg = Locale.getDefault().language
        for (te in arr) {
            if (ct == te || lg == te) {
                tv_1!!.text = "The app is not currently available in your country"
            }

        }
    }

    companion object {

        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")

        fun verifyStoragePermissions(activity: Activity) {

            try {
                //检测是否有写的权限
                val permission = ActivityCompat.checkSelfPermission(activity,
                        "android.permission.WRITE_EXTERNAL_STORAGE")
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // 没有写的权限，去申请写的权限，会弹出对话框
                    ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}
