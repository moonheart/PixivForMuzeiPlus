package moe.democyann.pixivformuzeiplus.util

import android.util.Log

import java.util.regex.Matcher
import java.util.regex.Pattern


object TagFliter {
    private val TAG = "TagFilter"

    fun is_r18(str: String): Boolean {
        Log.i(TAG, "R18 Checking")
        return Regex("(R-18|R18|r18|r-18)").matches(str)
    }

    private fun checkTag(tag: String, str: String): Boolean {
        return str.contains(tag);
    }

    fun checkTagAll(tag: String, str: String): Boolean {
        Log.i(TAG, "Tag Checking")
        val arr = tag.split(",")
        for (itm in arr) {
            Log.i(TAG, "checkTagAll: $itm")
            if ("" == itm) continue
            if (checkTag(itm, str)) {
                Log.i(TAG, "Find Tag $itm")
                return true
            }
        }
        return false
    }
}
