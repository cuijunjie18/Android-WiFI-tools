package com.example.wifi_kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WifiConnect"
    }

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var etSsid: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView

    // 当前的网络回调，用于断开连接时取消注册
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            connectToWifi()
        } else {
            Toast.makeText(this, "需要相关权限才能连接WIFI", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化系统服务
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 初始化UI组件
        etSsid = findViewById(R.id.etSsid)
        etPassword = findViewById(R.id.etPassword)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)

        // 设置连接按钮点击事件
        btnConnect.setOnClickListener {
            checkPermissionsAndConnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理网络回调
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "取消注册网络回调失败: ${e.message}")
            }
        }
    }

    private fun checkPermissionsAndConnect() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        )

        // Android 10以下需要位置权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val permissions = requiredPermissions.toTypedArray()

        if (permissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }) {
            connectToWifi()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun connectToWifi() {
        val ssid = etSsid.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (ssid.isEmpty()) {
            Toast.makeText(this, "请输入WIFI名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入WIFI密码", Toast.LENGTH_SHORT).show()
            return
        }

        // 确保WIFI已开启
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "请先开启WIFI", Toast.LENGTH_SHORT).show()
            return
        }

        updateStatus("正在连接 $ssid ...")
        btnConnect.isEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用新API
            connectWifiAndroid10Plus(ssid, password)
        } else {
            // Android 10以下使用旧API
            connectWifiLegacy(ssid, password)
        }
    }

    /**
     * Android 10+ (API 29+) 使用 WifiNetworkSpecifier 连接WiFi
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectWifiAndroid10Plus(ssid: String, password: String) {
        // 先取消之前的网络请求
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "取消旧回调失败: ${e.message}")
            }
        }

        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    // 绑定进程到该网络
                    connectivityManager.bindProcessToNetwork(network)
                    Log.i(TAG, "WiFi连接成功: $ssid")
                    runOnUiThread {
                        updateStatus("✅ 已连接到 $ssid")
                        btnConnect.isEnabled = true
                        Toast.makeText(this@MainActivity, "连接WIFI成功: $ssid", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.w(TAG, "WiFi连接失败: $ssid")
                    runOnUiThread {
                        updateStatus("❌ 连接失败，请检查WIFI名称和密码")
                        btnConnect.isEnabled = true
                        Toast.makeText(this@MainActivity, "连接WIFI失败，请检查名称和密码", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.w(TAG, "WiFi连接已断开: $ssid")
                    runOnUiThread {
                        updateStatus("⚠️ WIFI连接已断开")
                        btnConnect.isEnabled = true
                    }
                }
            }

            networkCallback = callback
            connectivityManager.requestNetwork(request, callback)

        } catch (e: Exception) {
            Log.e(TAG, "连接WiFi异常: ${e.message}", e)
            updateStatus("❌ 连接异常: ${e.message}")
            btnConnect.isEnabled = true
            Toast.makeText(this, "连接WIFI失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Android 10以下 使用 WifiConfiguration 连接WiFi（旧API）
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun connectWifiLegacy(ssid: String, password: String) {
        try {
            // 检查是否已存在该网络配置，如果有则先移除
            val existingConfigs = wifiManager.configuredNetworks
            existingConfigs?.forEach { config ->
                if (config.SSID == "\"$ssid\"") {
                    wifiManager.removeNetwork(config.networkId)
                }
            }

            // 创建新的Wifi配置
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
                // 设置安全类型
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            }

            val netId = wifiManager.addNetwork(wifiConfig)

            if (netId == -1) {
                updateStatus("❌ 添加网络配置失败")
                btnConnect.isEnabled = true
                Toast.makeText(this, "添加网络配置失败，请检查权限设置", Toast.LENGTH_SHORT).show()
                return
            }

            // 断开当前连接并连接到新网络
            wifiManager.disconnect()
            val success = wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            if (success) {
                updateStatus("✅ 正在连接 $ssid ...")
                Toast.makeText(this, "正在连接WIFI: $ssid", Toast.LENGTH_LONG).show()
            } else {
                updateStatus("❌ 启用网络失败")
                Toast.makeText(this, "启用网络失败", Toast.LENGTH_SHORT).show()
            }
            btnConnect.isEnabled = true

        } catch (e: Exception) {
            Log.e(TAG, "连接WiFi异常: ${e.message}", e)
            updateStatus("❌ 连接异常: ${e.message}")
            btnConnect.isEnabled = true
            Toast.makeText(this, "连接WIFI失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus(message: String) {
        tvStatus.text = message
    }
}