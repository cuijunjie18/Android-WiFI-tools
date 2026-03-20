package com.example.groupcreate

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.groupcreate.MainActivity.Companion.TAG

class Utils(val context: Context) {
  fun generateRandomString(len: Int): String {
    val allowedChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return (1..len)
      .map { allowedChars.random() }
      .joinToString("")
  }

  fun getCurrentDeviceName(): String {
    val defaultDeviceName = "Android"
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        Settings.Global.getString(
          context.contentResolver,
          Settings.Global.DEVICE_NAME
        ) ?: defaultDeviceName
      } else {
        defaultDeviceName
      }
    } catch (e: Exception) {
      Log.e(TAG, "获取设备名称失败")
      e.printStackTrace()
      defaultDeviceName
    }
  }

  fun truncateByBytes(str: String, maxBytes: Int): String {
    var currentBytes = 0
    val sb = StringBuilder()

    for (char in str) {
      val charByteLength = char.toString().toByteArray(Charsets.UTF_8).size
      if (currentBytes + charByteLength <= maxBytes) {
        sb.append(char)
        currentBytes += charByteLength
      } else {
        break // 字节数不够了，停止截取
      }
    }
    return sb.toString()
  }
}