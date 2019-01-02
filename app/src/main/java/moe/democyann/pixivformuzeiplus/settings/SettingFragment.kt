package moe.democyann.pixivformuzeiplus.settings

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.util.Log

import moe.democyann.pixivformuzeiplus.R

class SettingFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.setting)
        setup_method()
    }

    private fun setup_method() {
        val setting_key_method = findPreference("method") as ListPreference
        val ranking_mode = findPreference("ranking_mode") as ListPreference
        val ranking_mode_count = findPreference("ranking_mode_count") as ListPreference

        ranking_mode.isEnabled = setting_key_method.value.equals("0")
        ranking_mode_count.isEnabled = setting_key_method.value.equals("0")

        Log.d("", (setting_key_method==null).toString())
        Log.d("", (ranking_mode==null).toString())
        setting_key_method.setOnPreferenceChangeListener { _, newValue ->
            Log.d("XXXXXX", newValue.toString())
            val s = newValue.toString()
            ranking_mode.isEnabled = s.equals("0")
            ranking_mode_count.isEnabled = s.equals("0")
            true
        }
    }

}
