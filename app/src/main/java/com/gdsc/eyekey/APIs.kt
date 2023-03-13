package com.gdsc.eyekey

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface APIs{
    @GET("/uploader")
    fun getFiles():
        Call<ResultImg>

    @Multipart
    @POST("/uploader")
    fun uploadFiles(
        @Part file1: MultipartBody.Part?,
        @Part file2: MultipartBody.Part?
    ):Call<ResultImg>
}