package com.gdsc.eyekey

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException


class FileUploadUtils {
    fun send2Server(file: File, name: String){
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(name, file.name, RequestBody.create(MultipartBody.FORM, file))
            .build()

        val request: Request = Request.Builder()
            .url("http://34.64.59.193:5000/uploader") // Server URL 은 본인 IP를 입력
            .post(requestBody)
            .build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("RESPONSE : ", response.body!!.string())
            }
        })
    }
}