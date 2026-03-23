# 需求文档

这是一个windows wifi direct advertiser的demo，支持Android wifi direct设备扫描并连接。现在的问题是，之前Android端通过下面的代码连接
```kotlin
fun connectToDevice(device: WifiP2pDevice) {
    val config = WifiP2pConfig.Builder()
      .setDeviceAddress(MacAddress.fromString(device.deviceAddress))
      .build()


    manager.connect(channel, config, object : WifiP2pManager.ActionListener {
      override fun onSuccess() { Log.d(TAG, "连接请求已发送") }
      override fun onFailure(reason: Int) { Log.e(TAG, "连接请求失败：$reason") }
    })
  }
```

一开始的时候成功了几次，后面再测试的时候windows端会输出到L"Auto-accepting connection..."，但是最后等待一定时间后，windows端程序自己结束了，基本连不上，windows端的代码是有默认超时值吗？且似乎整个连接过程成功率不是很高，分析整个问题所在，必要的时候修改代码