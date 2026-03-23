#pragma once
#include "pch.h"
#include "utils.h"
#include <winsock2.h>
#include <ws2tcpip.h>         // 包含 inet_ntop / inet_pton
#include <stdio.h>
#include <stdlib.h>

#pragma comment(lib, "ws2_32.lib")

#define PORT 50001
#define BUFFER_SIZE 1024

void tcpTest() {
    std::wcout << L"Begin tcp Test..." << std::endl;

    WSADATA wsaData;
    SOCKET listenSocket, clientSocket;
    struct sockaddr_in serverAddr, clientAddr;
    int clientAddrLen = sizeof(clientAddr);
    char buffer[BUFFER_SIZE];
    int recvLen;
    char clientIP[INET_ADDRSTRLEN];  // 存放客户端 IP 字符串

    // 1. 初始化 Winsock
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        std::wcerr << L"WSAStartup failed." << std::endl;
        return;
    }

    // 2. 创建 TCP 套接字
    listenSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (listenSocket == INVALID_SOCKET) {
		std::wcerr << "socket failed: " << WSAGetLastError() << std::endl;
        WSACleanup();
        return;
    }

    // 3. 绑定地址和端口
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_addr.s_addr = INADDR_ANY;
    serverAddr.sin_port = htons(PORT);

    if (bind(listenSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
		std::wcerr << L"bind failed: " << WSAGetLastError() << std::endl;
        closesocket(listenSocket);
        WSACleanup();
        return;
    }

    // 4. 开始监听
    if (listen(listenSocket, SOMAXCONN) == SOCKET_ERROR) {
		std::wcerr << L"listen failed: " << WSAGetLastError() << std::endl;
        closesocket(listenSocket);
        WSACleanup();
        return;
    }

	std::wcout << L"Waiting for client to connect..." << std::endl;

    // 5. 接受客户端连接
    clientSocket = accept(listenSocket, (struct sockaddr*)&clientAddr, &clientAddrLen);
    if (clientSocket == INVALID_SOCKET) {
		std::wcerr << L"accept failed: " << WSAGetLastError() << std::endl;
        closesocket(listenSocket);
        WSACleanup();
        return;
    }

    // 使用 inet_ntop 将二进制地址转换为字符串
    if (inet_ntop(AF_INET, &clientAddr.sin_addr, clientIP, INET_ADDRSTRLEN) == NULL) {
        printf("inet_ntop failed\n");
        strcpy_s(clientIP, "unknown");
    }
	std::wcout << L"Client connected from " << clientIP << L":" << ntohs(clientAddr.sin_port) << std::endl;

    // 6. 接收并回显数据
    while ((recvLen = recv(clientSocket, buffer, BUFFER_SIZE, 0)) > 0) {
        buffer[recvLen] = '\0';
		std::wcout << L"Received data from client, length: " << recvLen << std::endl;
		std::wcout << L"Echoing back to client..." << std::endl;

        if (send(clientSocket, buffer, recvLen, 0) == SOCKET_ERROR) {
			std::wcerr << L"send failed: " << WSAGetLastError() << std::endl;
            break;
        }
    }

    if (recvLen == SOCKET_ERROR) {
		std::wcerr << L"recv failed: " << WSAGetLastError() << std::endl;
    }

    // 7. 关闭连接并清理
    closesocket(clientSocket);
    closesocket(listenSocket);
    WSACleanup();
}
