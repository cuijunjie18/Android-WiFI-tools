#include "pch.h"
#include "SocketReaderWriter.h"

#pragma comment(lib, "WindowsApp.lib")

using namespace winrt;
using namespace winrt::Windows::Foundation;
using namespace winrt::Windows::Devices::WiFiDirect;
using namespace winrt::Windows::Devices::Enumeration;
using namespace winrt::Windows::Networking::Sockets;
using namespace winrt::Windows::Storage::Streams;

void RunAdvertiserMode() {
    std::wcout << L"\n=== Wi-Fi Direct Advertiser Mode ===" << std::endl;

    // Prompt settings
    auto listenStateMode = WiFiDirectAdvertisementListenStateDiscoverability::Normal;
    bool isGO = true;
    bool enableListener = true;
    int16_t goIntent = 15;

    // Data structures
    std::vector<std::shared_ptr<ConnectedDevice>> connectedDevices;
    std::mutex devicesMutex;

    // Create publisher
    WiFiDirectAdvertisementPublisher publisher;
    publisher.Advertisement().IsAutonomousGroupOwnerEnabled(isGO);
    publisher.Advertisement().ListenStateDiscoverability(listenStateMode);


    auto statusToken = publisher.StatusChanged(
        [](WiFiDirectAdvertisementPublisher const&,
            WiFiDirectAdvertisementPublisherStatusChangedEventArgs const& args)
        {
            LogMessage(L"Advertisement Status: " +
                std::to_wstring(static_cast<int>(args.Status())) +
                L" Error: " + std::to_wstring(static_cast<int>(args.Error())));
        });

    // Connection listener
    WiFiDirectConnectionListener listener{ nullptr };
    winrt::event_token connectionToken{};
    StreamSocketListener listenerSocket{ nullptr };

    if (enableListener) {
        listener = WiFiDirectConnectionListener();

        connectionToken = listener.ConnectionRequested(
            [&connectedDevices, &devicesMutex, &listenerSocket, goIntent](
                WiFiDirectConnectionListener const&,
                WiFiDirectConnectionRequestedEventArgs const& args) 
            {
                try {
                    auto connectionRequest = args.GetConnectionRequest();
                    std::wstring deviceName = connectionRequest.DeviceInformation().Name().c_str();
                    LogMessage(L"Connection request received from " + deviceName);
                    LogMessage(L"Auto-accepting connection...");

                    WiFiDirectConnectionParameters connectionParams;
                    connectionParams.GroupOwnerIntent(goIntent);

                    auto wfdDevice = WiFiDirectDevice::FromIdAsync(
                        connectionRequest.DeviceInformation().Id(), connectionParams).get();

                    wfdDevice.ConnectionStatusChanged(
                        [](WiFiDirectDevice const& sender, auto const&)
                        {
                            LogMessage(L"Connection status changed: " +
                                std::to_wstring(static_cast<int>(sender.ConnectionStatus())));
                        });

                    auto connectedDevice = std::make_shared<ConnectedDevice>(
                        L"Waiting for client to connect...", wfdDevice);

                    {
                        std::lock_guard<std::mutex> lock(devicesMutex);
                        connectedDevices.push_back(connectedDevice);
                    }

                    auto endpointPairs = wfdDevice.GetConnectionEndpointPairs();
					auto ep = endpointPairs.GetAt(0);
                    std::wstring remoteHostName = ep.RemoteHostName().DisplayName().c_str();
					LogMessage(L"Connecting to client at " + remoteHostName, false);
                }
                catch (const winrt::hresult_error& e) {
                    LogMessage(L"Connect operation threw an exception: " + std::wstring(e.message().c_str()), true);
                }
            });
    }

    // Start advertisement
    try {
        publisher.Start();
        LogMessage(L"Advertisement started, waiting for connections...");
    } catch (const winrt::hresult_error& e) {
        LogMessage(L"Error starting advertisement: " + std::wstring(e.message().c_str()), true);
        return;
    }

    while (true) {
        std::wcout << L"\n> ";
        std::wstring cmd;
        std::getline(std::wcin, cmd);

        if (cmd == L"stop" || cmd == L"quit" || cmd == L"exit") {
            break;
        }
    }

    publisher.Stop();
    publisher.StatusChanged(statusToken);
    if (listener) {
        listener.ConnectionRequested(connectionToken);
    }
}

int main() {
    winrt::init_apartment();

	RunAdvertiserMode();

    winrt::uninit_apartment();
    return 0;
}
