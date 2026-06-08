package com.hobsojam.simpleestimation.integration

internal object IntegrationServer {
    val baseUrl: String
        get() = System.getProperty("integration.serverUrl", "http://10.0.2.2:3000")

    val wsBaseUrl: String
        get() = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
}
