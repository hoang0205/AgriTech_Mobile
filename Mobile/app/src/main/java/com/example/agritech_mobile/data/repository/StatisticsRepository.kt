package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.StatisticsApiService
import com.example.agritech_mobile.data.remote.dto.StatisticsOverviewDto
import retrofit2.Retrofit
import javax.inject.Inject

class StatisticsRepository @Inject constructor(
    retrofit: Retrofit
) {
    private val api = retrofit.create(
        StatisticsApiService::class.java
    )

    suspend fun getOverview(
        year: Int,
        month: Int?
    ): StatisticsOverviewDto {
        return api.getOverview(
            year = year,
            month = month
        )
    }
}