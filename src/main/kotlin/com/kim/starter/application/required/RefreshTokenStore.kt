package com.kim.starter.application.required

import java.time.Duration

interface RefreshTokenStore {
    fun save(
        subject: String,
        token: String,
        ttl: Duration,
    )

    fun find(subject: String): String?

    fun revoke(subject: String)
}
