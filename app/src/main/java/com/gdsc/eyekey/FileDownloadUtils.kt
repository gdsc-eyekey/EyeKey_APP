package com.gdsc.eyekey

import android.net.Uri
import android.provider.MediaStore
import okhttp3.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object FileDownloadUtils {

    fun dowload4Server(): Uri? {
        val request: Request = Request.Builder()
            .url("http://34.64.59.193:5000/uploader") // Server URL 은 본인 IP를 입력
            .build()
        val client = OkHttpClient()

        val now = SimpleDateFormat("yyMMdd_HHmmss").format(Date())

        val cbToDownloadFile = CallbackToDownloadFile(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(),
            "search_result_${now}"
        )
        client.newCall(request).enqueue(cbToDownloadFile)

        return Uri.fromFile(File(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "search_result_${now}"))
    }

    private class CallbackToDownloadFile(directory: String?, fileName: String) :
        Callback {
        private val directory: File
        private val fileToBeDownloaded: File

        init {
            this.directory = File(directory)
            fileToBeDownloaded = File(this.directory.getAbsolutePath() + "/" + fileName)
        }


        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            if (fileToBeDownloaded.exists()) {
                fileToBeDownloaded.delete()
            }
            try {
                fileToBeDownloaded.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
            val `is`: InputStream = response.body!!.byteStream()
            val os: OutputStream = FileOutputStream(fileToBeDownloaded)
            val BUFFER_SIZE = 2048
            val data = ByteArray(BUFFER_SIZE)
            var count: Int
            var total: Long = 0
            while (`is`.read(data).also { count = it } != -1) {
                total += count.toLong()
                os.write(data, 0, count)
            }
            os.flush()
            os.close()
            `is`.close()
        }
    }
}