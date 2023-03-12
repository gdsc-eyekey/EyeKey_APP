package com.gdsc.eyekey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.ImageDecoder
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.FileProvider.getUriForFile
import com.gdsc.eyekey.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // ViewBinding
    lateinit var binding : ActivityMainBinding

    private var imageView: ImageView? = null
    private var recorder: MediaRecorder? = null
    var customnProgressDialog: Dialog? = null
    private var outputPath: String? = null
    private var state: Boolean = false

    private var photoUri: Uri? = null
    private var soundUri: Uri? = null
    private var resultUri: Uri? = null




    // 요청하고자 하는 권한들
    private val PERMISSIONS = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    val PERMISSIONS_REQUEST = 100
    private val CameraPermission = 300

    // 권한을 허용하도록 요청
    private fun checkPermissions(permissions: Array<String>, permissionsRequest: Int): Boolean {
        val permissionList : MutableList<String> = mutableListOf()
        for(permission in permissions){
            val result = ContextCompat.checkSelfPermission(this, permission)
            if(result != PackageManager.PERMISSION_GRANTED){
                permissionList.add(permission)
            }
        }
        if(permissionList.isNotEmpty()){
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), PERMISSIONS_REQUEST)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for(result in grantResults){
            if(result != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "권한 승인이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        checkPermissions(PERMISSIONS, PERMISSIONS_REQUEST)

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {

        }

        val ibCamera: ImageButton = findViewById(R.id.ib_camera)
        ibCamera.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = File(
                File("${filesDir}/image").apply{
                    if(!this.exists()){
                        this.mkdirs()
                    }
                },
                newJpgFileName()
            )
            photoUri = FileProvider.getUriForFile(
                        this,
                "com.gdsc.eyekey.fileProvider",
                photoFile
            )
            takePictureIntent.resolveActivity(packageManager)?.also{
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, CameraPermission)
            }
        }

        val ibMike: ImageButton = findViewById(R.id.ib_mike)
        ibMike.setOnClickListener {
            if (!state) {
                startRecord()
            } else {
                stopRecord()
                val imageBackground: ImageView = findViewById(R.id.imagePreView)
                if (resultUri != null) {
                    imageBackground.setImageURI(resultUri)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode) {
                CameraPermission -> {
                    imageView = findViewById(R.id.imagePreView)
                    val imageBitmap = photoUri?.let { ImageDecoder.createSource(this.contentResolver, it) }
                    imageView?.setImageBitmap(imageBitmap?.let { ImageDecoder.decodeBitmap(it) })
                    Toast.makeText(this, photoUri?.path, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun newJpgFileName() : String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return "${filename}.jpg"
    }


   //음성 녹음
    private fun startRecord() {

        val fileName: String = Date().getTime().toString() + ".mp3"
        outputPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName + fileName //내장메모리 밑에 위치
        recorder = MediaRecorder()
        recorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        recorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder?.setOutputFile(outputPath)
        if (outputPath != null) {
            soundUri = Uri.fromFile(File(outputPath))
        }


        try {
            recorder?.prepare()
            recorder?.start()
            state = true
            Toast.makeText(this, "녹음이 시작되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecord() {
        if (state) {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
            state = false
            Toast.makeText(this, "녹음이 되었습니다.", Toast.LENGTH_SHORT).show()

            //사진 파일 전송
            if (photoUri != null && soundUri != null) {
                val file1 = File(photoUri!!.path)
                val file2 = File(soundUri!!.path)

                val imageRequestBody1 = file1.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val audioRequestBody2 = file2.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                val filePart1 = MultipartBody.Part.createFormData("file1", file1.name, imageRequestBody1)
                val filePart2 = MultipartBody.Part.createFormData("file2", file2.name, audioRequestBody2)

                Toast.makeText(this, "파일 ${file1}.", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "파일 ${file2}.", Toast.LENGTH_SHORT).show()

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://34.64.228.205:5000/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api: APIs = retrofit.create(APIs::class.java)

                val callResultImg = api.uploadFiles(filePart1,filePart2)


                callResultImg.enqueue(object : retrofit2.Callback<ResultImg>{
                    override fun onResponse(
                        call: Call<ResultImg>,
                        response: Response<ResultImg>
                    ) {
                        Log.d("POST", "성공 : ${response.raw()}")
                    }

                    override fun onFailure(call: Call<ResultImg>, t: Throwable) {
                        Log.d("POST", "실패 : $t")
                    }
                })


//                val imageBackground: ImageView = findViewById(R.id.imagePreView)
//                imageBackground.setImageBitmap(resultImg.)
                Toast.makeText(this, "파일이 전송되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "파일이 인식되지 않습니다.", Toast.LENGTH_SHORT).show()
            }

//            cancelProgressDiaglog()
        } else {
            Toast.makeText(this, "녹음 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }

    }

}
