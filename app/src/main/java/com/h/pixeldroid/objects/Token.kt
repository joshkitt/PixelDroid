package com.h.pixeldroid.objects

data class Token(
    val access_token: String?,
    val refresh_token: String?,
    val token_type: String?,
    val scope: String?,
    val created_at: Int?
)