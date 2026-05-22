package com.example.webreader.data

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val versionName: String,
    val minVersionCode: Int,
    val updateUrl: String,
    val releaseNotes: String
)
