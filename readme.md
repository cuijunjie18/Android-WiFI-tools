# Android Wifi direct tools

## 背景

开发过程中适用于不同平台的 wifi direct工具集.

## 工具集

- [Android 创建p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pGroupCreate)   
- [Android networkName连接到p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pConnectGroup)  
- [Android discover连接到p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pConnectGroupDiscover)   
- [Android 常规wifi连接](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/WifiNormalKotlin?tab=readme-ov-file)  
- [Windows Advertiser(兼容Wi-Fi Direct与非Wi-Fi Direct设备)](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/WindowsAdvertiser#)  
- [Windows官方Wifi Direct Sample](https://github.com/microsoft/Windows-universal-samples/tree/main/Samples/WiFiDirect)    


## 收获

- 调试wifip2pManager相关api
  在Android Studio中调试p2p相关的日志信息不够详细，可以在adb中使用下面命令
  ```shell
  logcat -s WifiP2PManager WifiP2PService WifiP2PNative WifiDirect wpa_supplicant
  ```

## 参考
[1] Windows Wi-Fi Direct官方文档: https://learn.microsoft.com/en-us/uwp/api/windows.devices.wifidirect?view=winrt-26100&redirectedfrom=MSDN  

[2] Android P2P api文档：https://developer.android.com/develop/connectivity/wifi/wifip2p?hl=zh-cn  