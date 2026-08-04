package com.streamhub.app.data.api

object Secrets {
    // TMDB API Key
    var TMDB_API_KEY: String = "ec562d9f2a8a07ffb7fa3308fb5bec9c"
    
    // MyAnimeList (MAL) Official Credentials
    var MAL_CLIENT_ID: String = "61070e26017201303b6320a0a973d40c"
    var MAL_CLIENT_SECRET: String = ""
    
    // Official MyAnimeList REST API v2 Base URL
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"
    
    // Optional Telegram Bot Token for private channel stream authorization
    var TELEGRAM_BOT_TOKEN: String = ""
}
