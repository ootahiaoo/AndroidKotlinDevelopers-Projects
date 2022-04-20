package com.udacity

enum class FileType(val url: String, val description: String) {
    Glide(
        "https://github.com/bumptech/glide",
        "Glide - Image Loading Library by BumpTech"
    ),
    LoadApp(
        "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter" +
                "/archive/master.zip",
        "LoadApp - Current repository by Udacity"
    ),
    Retrofit(
        "https://github.com/square/retrofit",
        "Retrofit - Type-safe HTTP client for Android and Java by Square, Inc"
    )
}