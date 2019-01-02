package moe.democyann.pixivformuzeiplus.activity

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import moe.democyann.pixivformuzeiplus.ConfigManger
import moe.democyann.pixivformuzeiplus.R

class IcoActivity : AppCompatActivity() {

    private var conf: ConfigManger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ico)
        conf = ConfigManger(this)
        if (conf!!.icon) {
            val p = packageManager
            val test = ComponentName("moe.democyann.pixivformuzeiplus", "moe.democyann.pixivformuzeiplus.activity.MainActivity")
            p.setComponentEnabledSetting(test,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
            Log.i("ICO", "onCreate: DISABLED")
            Toast.makeText(applicationContext, "隐藏图标",
                    Toast.LENGTH_SHORT).show()
            conf!!.icon = false
        } else {
            val p = packageManager
            val test = ComponentName("moe.democyann.pixivformuzeiplus", "moe.democyann.pixivformuzeiplus.activity.MainActivity")
            p.setComponentEnabledSetting(test,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
            Log.i("ICO", "onCreate: ENABLED")
            Toast.makeText(applicationContext, "显示图标",
                    Toast.LENGTH_SHORT).show()
            conf!!.icon = true
        }

        finish()
    }
}
