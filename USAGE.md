# GroupConnect - WiFi Direct连接工具

## 应用简介

GroupConnect是一个Android客户端应用，专门用于通过WiFi Direct技术连接Direct热点。该应用简化了WiFi Direct连接过程，用户只需输入网络名称和密码即可建立连接。

## 核心功能

- ✅ 通过networkname和passphrase连接Direct热点
- ✅ 自动权限申请和管理
- ✅ 实时连接状态显示
- ✅ 对等设备发现功能
- ✅ 错误处理和状态反馈

## 技术实现

### 主要技术栈
- **语言**: Kotlin
- **框架**: Android SDK
- **WiFi Direct**: Android WiFi P2P API
- **权限管理**: Android权限系统
- **UI框架**: Material Design

### 关键特性
1. **权限自动管理**: 应用启动时自动请求必要的WiFi Direct权限
2. **状态监听**: 实时监听WiFi Direct状态变化
3. **连接重试机制**: 包含连接失败的重试逻辑
4. **反射配置**: 使用反射设置WiFi Direct配置参数

## 使用步骤

### 1. 权限授权
- 首次启动应用时，系统会请求以下权限：
  - ACCESS_WIFI_STATE
  - CHANGE_WIFI_STATE  
  - ACCESS_FINE_LOCATION
  - ACCESS_COARSE_LOCATION
  - NEARBY_WIFI_DEVICES

### 2. 输入连接信息
- 在"网络名称"输入框中输入Direct热点的SSID
- 在"密码"输入框中输入Direct热点的密码

### 3. 建立连接
- 点击"连接Direct热点"按钮开始连接过程
- 应用会显示实时连接状态
- 连接成功后会显示确认信息

## 开发说明

### 项目结构
```
app/src/main/java/com/example/groupconnect/
├── MainActivity.kt          # 主Activity，包含WiFi Direct逻辑

app/src/main/res/
├── layout/activity_main.xml # 主界面布局
├── values/strings.xml        # 字符串资源
└── AndroidManifest.xml       # 应用配置和权限
```

### 主要类说明

#### MainActivity
- 管理WiFi Direct连接的生命周期
- 处理权限申请和回调
- 实现连接状态监听
- 提供用户界面交互

### WiFi Direct API使用
应用使用Android标准的WiFi P2P API：
- `WifiP2pManager` - 管理WiFi Direct操作
- `WifiP2pConfig` - 配置连接参数
- `BroadcastReceiver` - 监听状态变化

## 兼容性要求

- **最低Android版本**: API 24 (Android 7.0)
- **目标Android版本**: API 33 (Android 13)
- **硬件要求**: 支持WiFi Direct功能的Android设备

## 注意事项

1. **设备兼容性**: 确保设备硬件支持WiFi Direct功能
2. **权限管理**: 应用需要位置权限才能发现对等设备
3. **网络环境**: 确保WiFi功能已启用
4. **连接稳定性**: WiFi Direct连接可能受环境因素影响

## 故障排除

### 常见问题
1. **连接失败**: 检查网络名称和密码是否正确
2. **无法发现设备**: 确认对方设备WiFi Direct功能已开启
3. **权限问题**: 重新授权应用权限
4. **硬件不支持**: 确认设备支持WiFi Direct

### 调试建议
- 查看Logcat输出获取详细错误信息
- 检查WiFi Direct系统设置
- 验证设备兼容性

## 后续优化方向

- 添加设备发现和选择功能
- 实现连接历史记录
- 添加连接质量监控
- 支持更多WiFi Direct高级功能