package com.example.p2pconnect

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.p2pconnect.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity(), WifiDirectManager.ConnectionListener {

  private lateinit var binding: ActivityMainBinding
  private lateinit var wifiDirectManager: WifiDirectManager
  private var targetDevice: WifiP2pDevice? = null

  // 权限列表
  private val requiredPermissions: Array<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
      )
    }

  @SuppressLint("MissingPermission")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    if (checkPermissions()) {
      initWifiDirect()
    }

    binding.btnDiscover.setOnClickListener {
      if (checkPermissions()) {
        appendStatus("正在扫描设备...")
        wifiDirectManager.discoverPeers()
      }
    }

    binding.btnConnect.setOnClickListener {
      targetDevice?.let { device ->
        appendStatus("正在连接 ${device.deviceName}...")
        wifiDirectManager.connectToDevice(device)
      } ?: Toast.makeText(this, "未发现设备", Toast.LENGTH_SHORT).show()
    }
  }

  private fun initWifiDirect() {
    wifiDirectManager = WifiDirectManager(this, this)
  }

  private fun checkPermissions(): Boolean {
    val missingPermissions = requiredPermissions.filter {
      ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    return if (missingPermissions.isEmpty()) {
      true
    } else {
      ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
      false
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
      initWifiDirect()
    } else {
      Toast.makeText(this, "权限被拒绝，无法使用 WiFi Direct", Toast.LENGTH_LONG).show()
    }
  }

  // 回调：发现设备
  override fun onDeviceFound(device: WifiP2pDevice) {
    // 这里可以添加过滤逻辑，比如只连接特定名称的设备
    targetDevice = device
    runOnUiThread {
      appendStatus("\n发现设备：${device.deviceName}")
      Toast.makeText(this, "发现 Linux 设备", Toast.LENGTH_SHORT).show()
    }
  }

  // 回调：连接成功
  override fun onConnectionSuccess(info: WifiP2pInfo) {
    val goIp = info.groupOwnerAddress?.hostAddress ?: return
    runOnUiThread {
      appendStatus("\n连接成功！\nGO IP: $goIp")
      // 启动协程进行 TCP 通信
      testTcpConnection(goIp)
    }
  }

  override fun onConnectionFailed() {
    runOnUiThread { appendStatus("\n连接失败") }
  }

  // 协程处理网络 IO
  private fun testTcpConnection(hostIp: String) {
    lifecycleScope.launch {
      appendStatus("\n正在尝试 TCP 通信...")
      val result = withContext(Dispatchers.IO) {
        sendTcpMessage(hostIp, 8988, "Hello from Android Kotlin!")
      }
      appendStatus("\n通信结果：$result")
    }
  }

  private fun sendTcpMessage(host: String, port: Int, message: String): String {
    val socket = Socket()
    return try {
      socket.connect(InetSocketAddress(host, port), 5000)
      val outputStream = socket.getOutputStream()
      val inputStream = socket.getInputStream()

      // 发送
      outputStream.write(message.toByteArray())
      outputStream.flush()

      // 接收响应
      val buffer = ByteArray(1024)
      val read = inputStream.read(buffer)
      if (read > 0) "收到响应：${String(buffer, 0, read)}" else "发送成功，无响应"
    } catch (e: IOException) {
      "通信错误：${e.message}"
    } finally {
      try { socket.close() } catch (e: IOException) {}
    }
  }

  private fun appendStatus(text: String) {
    binding.statusText.append(text)
  }

  override fun onDestroy() {
    super.onDestroy()
    wifiDirectManager.onDestroy()
  }
}