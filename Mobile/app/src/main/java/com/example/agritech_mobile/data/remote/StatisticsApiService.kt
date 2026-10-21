package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.StatisticsOverviewDto
import retrofit2.http.GET
import retrofit2.http.Query

interface StatisticsApiService {

    @GET("api/statistics/overview")
    suspend fun getOverview(
        @Query("year") year: Int,
        @Query("month") month: Int?
    ): StatisticsOverviewDto
}