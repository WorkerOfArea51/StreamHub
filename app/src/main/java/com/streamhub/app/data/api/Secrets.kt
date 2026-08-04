package com.streamhub.app.data.api

object Secrets {
    // TMDB API Key
    var TMDB_API_KEY: String = "ec562d9f2a8a07ffb7fa3308fb5bec9c"
    
    // MyAnimeList (MAL) Official Credentials
    var MAL_CLIENT_ID: String = "4f7167fe0e6ff0b5832d117657a1aefb"
    var MAL_CLIENT_SECRET: String = "c721d0b2400eeb7893c2e958514be9279736d7f202b6734e4eef913e098b71df"
    
    // Official MyAnimeList REST API v2 Base URL
    const val MAL_BASE_URL: String = "https://api.myanimelist.net/v2/"
    
    // Optional Telegram Bot Token for private channel stream authorization
    var TELEGRAM_BOT_TOKEN: String = ""
}
