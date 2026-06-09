package com.hobsojam.simpleestimation.integration

internal object IntegrationServer {
    // Default targets the host JVM. For emulator runs pass -PintegrationServerUrl=http://10.0.2.2:3000.
    val baseUrl: String
        get() = System.getProperty("integration.serverUrl", "http://localhost:3000")

    val wsBaseUrl: String
        get() = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
}
