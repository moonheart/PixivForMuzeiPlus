package moe.democyann.pixivformuzeiplus

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import java.util.ArrayList

class ConfigManger(val context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val TAG = "ConfigManager"


    val password: String?
        get() {
            val defaultValue = ""
            return preferences.getString("password", defaultValue)
        }

    val username: String?
        get() {
            val defaultValue = ""
            return preferences.getString("pixivid", defaultValue)
        }

    val isOnlyUpdateOnWifi: Boolean
        get() {
            val defaultValue = false
            val v = preferences.getBoolean("only_wifi", defaultValue)
            Log.d(TAG, "pref_onlyWifi = $v")
            return v
        }

    val tage: String?
        get() {
            val defaultValue = ""
            return preferences.getString("tags", defaultValue)
        }

    val view: Long
        get() {
            val defaultValue = "0"
            val s = preferences.getString("views", defaultValue)
            var v: Long = 0
            try {
                v = java.lang.Long.valueOf(s)
            } catch (e: Exception) {
                Log.e(TAG, "getViews: ", e)
            }

            if (v > 50000) v = 50000
            return v
        }
    val bootmarkCount: Long
        get() {
            val defaultValue = "0"
            val s = preferences.getString("BookmarkCount", defaultValue)
            var v: Long = 0
            try {
                v = java.lang.Long.valueOf(s)
            } catch (e: Exception) {
                Log.e(TAG, "getBootmarkCount: ", e)
            }

            if (v > 50000) v = 50000
            return v
        }

    val is_no_R18: Boolean
        get() {
            val defaultValue = true
            return preferences.getBoolean("is_no_r18", defaultValue)
        }

    /**
     * 获取更新时间间隔
     * @return 分钟
     */
    val changeInterval: Int
        get() {

            val defaultValue = context.getString(R.string.time_default)
            val s = preferences.getString("time_change", defaultValue)
            Log.d(TAG, "time_change = \"$s\"")
            try {
                return Integer.parseInt(s)
            } catch (e: NumberFormatException) {
                Log.w(TAG, e.toString(), e)
                return 0
            }

        }

    /**
     * 获取随机模式
     * @return
     */
    val method: Int
        get() {
            val defaultValue = context.getString(R.string.method_default)
            val s = preferences.getString("method", defaultValue)
            try {
                return s!!.toInt()
            } catch (e: NumberFormatException) {
                Log.w(TAG, e.toString(), e)
                return 0
            }
        }

    val is_check_Tag: Boolean
        get() {
            val defaultValue = false
            return preferences.getBoolean("is_tag", defaultValue)
        }
    val is_autopx: Boolean
        get() {
            val defaultValue = false
            return preferences.getBoolean("is_autopx", defaultValue)
        }

    var icon: Boolean
        get() {
            val defaultValue = true
            return preferences.getBoolean("icon", defaultValue)
        }
        set(v) {
            val edit = preferences.edit()
            edit.putBoolean("icon", v)
            edit.commit()
        }
    val ranking_mode: String
        get() {
            val defaultValue = "daily"
            return preferences.getString("ranking_mode", defaultValue)
        }
    val ranking_mode_count: Int
        get() {
            val defaultValue = "50"
            return preferences.getString("ranking_mode_count", defaultValue).toInt()
        }

    val px: Double
        get() {
            val dm = context.resources.displayMetrics
            val w_screen = dm.widthPixels
            val h_screen = dm.heightPixels
            Log.i(TAG, "getPx: $w_screen * $h_screen")
            return w_screen * 1.00 / h_screen
        }


}
