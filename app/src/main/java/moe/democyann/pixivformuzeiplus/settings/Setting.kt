package moe.democyann.pixivformuzeiplus.settings

import android.app.Activity
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.util.Log

class Setting : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingFragment()).commit()
    }

}
