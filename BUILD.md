# WiFi Direct Legacy AP - Automated Demo

## Build Instructions

### Prerequisites
- Windows 10/11
- Visual Studio 2019 or 2022 (with "Desktop development with C++" workload)
- Windows SDK 10.0 or later

### Method 1: Visual Studio IDE
1. Open `WiFiDirectLegacyAPDemo.sln` in Visual Studio
2. Select **Release | x64** (or Debug | x64) configuration
3. Build → Build Solution (Ctrl+Shift+B)
4. Output binary: `x64\Release\WiFiDirectLegacyAPDemo.exe`

### Method 2: MSBuild Command Line
Open **"Developer Command Prompt for VS"** or **"x64 Native Tools Command Prompt"**, then:

```cmd
cd demo
msbuild WiFiDirectLegacyAPDemo.sln /p:Configuration=Release /p:Platform=x64
```

Output binary: `x64\Release\WiFiDirectLegacyAPDemo.exe`

### Method 3: cl.exe Direct Compilation
Open **"x64 Native Tools Command Prompt for VS"**, then:

```cmd
cd demo
cl.exe /EHsc /DUNICODE /D_UNICODE /DWIN32 /std:c++17 /I"." /Yc"stdafx.h" stdafx.cpp /Fo"stdafx.obj"
cl.exe /EHsc /DUNICODE /D_UNICODE /DWIN32 /std:c++17 /I"." /Yu"stdafx.h" WlanHostedNetworkWinRT.cpp /c /Fo"WlanHostedNetworkWinRT.obj"
cl.exe /EHsc /DUNICODE /D_UNICODE /DWIN32 /std:c++17 /I"." /Yu"stdafx.h" main.cpp /c /Fo"main.obj"
link.exe stdafx.obj WlanHostedNetworkWinRT.obj main.obj /OUT:WiFiDirectLegacyAPDemo.exe runtimeobject.lib
```

### Method 4: Simplified (without precompiled headers)
Open **"x64 Native Tools Command Prompt for VS"**, then:

```cmd
cd demo
cl.exe /EHsc /DUNICODE /D_UNICODE /DWIN32 /std:c++17 main.cpp WlanHostedNetworkWinRT.cpp /Fe:WiFiDirectLegacyAPDemo.exe /link runtimeobject.lib
```

## Running

```cmd
WiFiDirectLegacyAPDemo.exe
```

The program will:
1. Automatically start a Soft AP with SSID = `MyWiFiDirectAP`, Passphrase = `12345678`
2. Auto-accept all incoming peer connections
3. Print connection/disconnection events to the console
4. Wait for the user to press **Enter** to stop and exit

### Customization
Edit the constants at the top of `main.cpp` to change SSID and passphrase:

```cpp
static const wchar_t* AP_SSID       = L"MyWiFiDirectAP";
static const wchar_t* AP_PASSPHRASE = L"12345678";
```

> **Note**: This program must be run with Administrator privileges, and the system must have a Wi-Fi adapter that supports Wi-Fi Direct.
