package com.vidlynk.app.data.remote

import com.vidlynk.app.data.model.VideoResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface VidLynkApi {
    /**
     * Resolves a video deep link slug (e.g. /api/v1/videos/{slug})
     * to fetch streamable CDN video URL and metadata.
     */
    @GET("videos/{slug}")
    suspend fun getVideoBySlug(
        @Path("slug") slug: String
    ): VideoResponse
}
