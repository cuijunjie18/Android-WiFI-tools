# Android Wifi direct tools

## 背景

一个我调试过程中Android端p2p的Wifi工具集

## 工具集

- [Android 创建p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pGroupCreate)   
- [Android networkName连接到p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pConnectGroup)  
- [Android discover连接到p2p组群](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/P2pConnectGroupDiscover)   
- [Android 常规wifi连接](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/WifiNormalKotlin?tab=readme-ov-file)  
- [Windows Advertiser demo版](https://github.com/cuijunjie18/Android-WiFI-tools/tree/cjj/WindowsAdvertiser#)  
- [Windows 官方Wifi Direct工具](https://github.com/microsoft/Windows-classic-samples/tree/main/Samples/WiFiDirectLegacyAP)  


## 收货

- 调试wifip2pManager相关api
  在Android Studio中调试p2p相关的日志信息不够详细，可以在adb中使用下面命令
  ```shell
  logcat -s WifiP2PManager WifiP2PService WifiP2PNative WifiDirect wpa_supplicant
  ```
