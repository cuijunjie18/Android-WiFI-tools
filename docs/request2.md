# 需求文档

忽略docs/文件夹

当前Android客户端实现了一个连接到p2p group的功能，为什么当前使用的连接方式不需要通过认证即可连接上group了，成功连接上的group使用的创建逻辑如下

```kotlin
  private fun createGroup() {
    Log.d(TAG, "create Group start")
    val listener = object : WifiP2pManager.ActionListener {
      override fun onSuccess() {
        Log.d(TAG, "create Group success")
        Toast.makeText(this@MainActivity, "创建群组成功", Toast.LENGTH_SHORT).show()
      }

      override fun onFailure(reason: Int) {
        Log.e(TAG, "create Group fail: error code: $reason")
        Toast.makeText(this@MainActivity, "创建群组失败", Toast.LENGTH_SHORT).show()
      }
    }

    val config = WifiP2pConfig.Builder()
      .setNetworkName("DIRECT-123")
      .setPassphrase("00000000")
      .build()

    // 移除旧群组
    removeGroup()
    wifiP2pManager.createGroup(channel, config, listener)
    Log.d(TAG, "create Group done")
  }
```

给出分析