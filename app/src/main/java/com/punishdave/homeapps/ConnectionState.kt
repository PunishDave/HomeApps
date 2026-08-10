package com.punishdave.homeapps

enum class ServiceDisplayState { NotChecked, Refreshing, Live, Cached, Unavailable }

fun serviceDisplayState(refreshing: Boolean, healthy: Boolean?, hasCache: Boolean): ServiceDisplayState = when {
    refreshing -> ServiceDisplayState.Refreshing
    healthy == true -> ServiceDisplayState.Live
    hasCache -> ServiceDisplayState.Cached
    healthy == false -> ServiceDisplayState.Unavailable
    else -> ServiceDisplayState.NotChecked
}

fun sophonConnectionLabel(url: String): String = when {
    url.contains(".ts.net", ignoreCase = true) -> "Tailscale"
    url.contains("192.168.") || url.contains("10.") || url.contains("172.16.") -> "Home network"
    else -> "Remote connection"
}
