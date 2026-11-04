package com.vidlynk.app.data.model

import com.google.gson.annotations.SerializedName

data class VideoResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: VideoDto?
)

data class VideoDto(
    @SerializedName("id") val id: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("duration") val duration: Int,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("resolution") val resolution: String?,
    @SerializedName("viewsCount") val viewsCount: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("shareUrl") val shareUrl: String?
)
