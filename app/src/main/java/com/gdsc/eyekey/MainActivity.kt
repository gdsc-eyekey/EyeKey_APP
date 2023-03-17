package com.gdsc.eyekey

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gdsc.eyekey.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64


class MainActivity : AppCompatActivity() {

    // ViewBinding
    lateinit var binding : ActivityMainBinding

    private var imageView: ImageView? = null

    var customnProgressDialog: Dialog? = null

    //photo
    private var mCurrentPhotoPath: String? = null
    private var photoUri: Uri? = null

    //record mp3
    private var outputPath: String? = null
    private var state: Boolean = false
    private var soundUri: Uri? = null
    private var recorder: MediaRecorder? = null
    private var resultUri: Uri? = null

    //base64 img
    private var resultImg: String? = null


    // 요청하고자 하는 권한들
    private val PERMISSIONS = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    val PERMISSIONS_REQUEST = 100
    private val REQUEST_TAKE_PHOTO = 100
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
    @RequiresApi(Build.VERSION_CODES.P)
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
            captureCamera()
            Log.d("mCurrentPhotoPath", "${mCurrentPhotoPath}")
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

    //사진 임시파일 생성
    @Throws(IOException::class)
    fun createImageFile(): File? { // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"
        var imageFile: File? = null
        val storageDir = File(
            Environment.getExternalStorageDirectory().toString()+"/Pictures",
            "Eyekey"
        )
        if (!storageDir.exists()) {
            Log.i("mCurrentPhotoPath", storageDir.toString())
            storageDir.mkdirs()
        }
        imageFile = File(storageDir, imageFileName)
        mCurrentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    //카메라 실행 및 데이터 전달
    private fun captureCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {

            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Log.e("captureCamera Error", ex.toString())
                return
            }
            if (photoFile != null) { // getUriForFile의 두 번째 인자는 Manifest provier의 authorites와 일치해야 함
                val providerURI =
                    FileProvider.getUriForFile(this, "com.gdsc.eyekey.fileProvider", photoFile)
                // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }
    //카메라에서 찍은 사진 결과호출
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TAKE_PHOTO -> {
                Log.i("REQUEST_TAKE_PHOTO", "${Activity.RESULT_OK}" + " " + "${resultCode}");
                if (resultCode == RESULT_OK) {
                    try {
                        galleryAddPic();
                    } catch (e: Exception) {
                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
                    }

                } else {
                    Toast.makeText(this@MainActivity, "사진찍기를 취소하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    //이미지 로컬폴더에 저장
    private fun galleryAddPic() {
        Log.i("galleryAddPic", "Call")
        val mediaScanIntent: Intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
        val f: File = File(mCurrentPhotoPath)
        val contentUri: Uri = Uri.fromFile(f)
        mediaScanIntent.setData(contentUri)
        sendBroadcast(mediaScanIntent)
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        if(mCurrentPhotoPath != null){
            val imageBackground: ImageView = findViewById(R.id.imagePreView)
            val myBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)
            imageBackground.setImageBitmap(myBitmap)
        }
    }

   //음성 녹음
    private fun startRecord() {

        val fileName: String = Date().getTime().toString() + ".mp3"
        outputPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Download/" +fileName //내장메모리 밑에 위치
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
            if (mCurrentPhotoPath != null && soundUri != null) {
                val file1 = File(mCurrentPhotoPath)
                val file2 = File(soundUri!!.path)
                Log.d("POST", file1.toString())
                Log.d("POST", file2.toString())
                val imageRequestBody1 = file1.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val audioRequestBody2 = file2.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                val filePart1 = MultipartBody.Part.createFormData("file1", file1.name, imageRequestBody1)
                val filePart2 = MultipartBody.Part.createFormData("file2", file2.name, audioRequestBody2)

                Log.d("POST", "${file1}")
                var gson = GsonBuilder().setLenient().create()
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://34.64.228.205:5000/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

                val api: APIs = retrofit.create(APIs::class.java)

                val callResultImg = api.uploadFiles(filePart1,filePart2)

                callResultImg.enqueue(object : retrofit2.Callback<ResultImg>{
                    override fun onResponse(
                        call: Call<ResultImg>,
                        response: Response<ResultImg>
                    ) {
                        Log.d("POST", "성공 : ${response.toString()}")
                        val body = response.body()?.let {
                            resultImg = it.resultImg
                            Log.d("POST", "resultImg : ${resultImg}")
                            val imageBackground: ImageView = findViewById(R.id.imagePreView)
                            val imageBytes = Base64.decode(resultImg, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            imageBackground.setImageBitmap(decodedImage)
                        }
                    }
                    override fun onFailure(call: Call<ResultImg>, t: Throwable) {
                        Log.d("POST", "실패 : $t")
                    }
                })
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
