package com.example.audioplayerm3u8

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Status>
    enum class Status {
        Available, Lost, Losing, Unavailable
    }
}