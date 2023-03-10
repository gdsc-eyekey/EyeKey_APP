package com.gdsc.eyekey

import android.R.attr.bitmap
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gdsc.eyekey.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var imageView:ImageView? = null
    private var recorder: MediaRecorder? = null
    var customnProgressDialog : Dialog? = null
    private var outputPath: String? = null
    private var state : Boolean = false

    var pictureUri: Uri? = null
    private var soundUri: Uri? = null
    private var resultUri: Uri? = null



    // 파일 불러오기
    private val getContentImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null){
            pictureUri = uri
            uri.let { binding?.imagePreView?.setImageURI(uri)}
            val imageBackground:ImageView = findViewById(R.id.imagePreView)
            imageBackground.setImageURI(uri)
        }


    }

    // 카메라를 실행한 후 찍은 사진을 저장

    private val getTakePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if(it) {
            pictureUri.let { binding?.imagePreView?.setImageURI(pictureUri) }
            val imageBackground:ImageView = findViewById(R.id.imagePreView)
            imageBackground.setImageURI(pictureUri)
        }
    }

    // 요청하고자 하는 권한들
    private val permissionList = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.INTERNET
    )

    // 권한을 허용하도록 요청
    private val requestMultiplePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        results.forEach {
            if(!it.value) {
                Toast.makeText(applicationContext, "권한 허용 필요", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        requestMultiplePermission.launch(permissionList)

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            getContentImage.launch("image/*")
        }

        val ibCamera : ImageButton = findViewById(R.id.ib_camera)
        ibCamera.setOnClickListener{
            pictureUri = createImageFile()
            getTakePicture.launch(pictureUri)
        }

        val ibMike : ImageButton = findViewById(R.id.ib_mike)
        ibMike.setOnClickListener{
            if(!state){
                startRecord()
            }else{
                stopRecord()
                val imageBackground:ImageView = findViewById(R.id.imagePreView)
                if(resultUri !=null) {
                    imageBackground.setImageURI(resultUri)
                }
            }
        }



    }

    //사진 저장 부분, 내부 캐시 이용
    private fun createImageFile(): Uri? {
//        // save bitmap to cache directory
        val now = SimpleDateFormat("yyMMdd_HHmmss").format(Date())
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "img_$now.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
    }

    private fun showRationalDialog(
        title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancle"){
                    dialog, _ -> dialog.dismiss()
            }
        builder.create().show()
    }


//    private fun showProgressDialog(){
//        customnProgressDialog = Dialog(this@MainActivity)
//        customnProgressDialog?.setContentView(R.layout.dialog_custom_progress)
//        customnProgressDialog?.show()
//    }
//    private fun cancelProgressDiaglog(){
//        if(customnProgressDialog != null){
//            customnProgressDialog?.dismiss()
//            customnProgressDialog = null
//        }
//    }
    private fun startRecord(){

        val fileName: String = Date().getTime().toString() + ".mp3"
        outputPath = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName  + fileName //내장메모리 밑에 위치
        recorder = MediaRecorder()
        recorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        recorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder?.setOutputFile(outputPath)
        if(outputPath!=null){
            soundUri = Uri.fromFile(File(outputPath))
        }


        try {
            recorder?.prepare()
            recorder?.start()
            state = true
            Toast.makeText(this, "녹음이 시작되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun stopRecord(){
        if(state){
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
            state = false
            Toast.makeText(this, "녹음이 되었습니다.", Toast.LENGTH_SHORT).show()

            //사진 파일 전송
            if(pictureUri!=null && soundUri!=null){
                val imageFile = File(pictureUri!!.path)
                val requestImg = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
                val file1 = MultipartBody.Part.createFormData("file1", "file1", requestImg)

                val recordFile = File(soundUri!!.getPath())
                val requestAudio = RequestBody.create("audio/mpeg".toMediaTypeOrNull(), recordFile)
                val file2 = MultipartBody.Part.createFormData("file2", "file2", requestAudio)

                val result = RetrofitClient.service.uploadImage(file1, file2)
                Log.d("response", "결과는 ${result.toString()}")
                


                val imageBackground:ImageView = findViewById(R.id.imagePreView)
//                imageBackground.setImageBitmap(resultImg.)


                Toast.makeText(this, "파일이 전송되었습니다.", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "파일이 인식되지 않습니다.", Toast.LENGTH_SHORT).show()
            }

//            cancelProgressDiaglog()
        } else {
            Toast.makeText(this, "녹음 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }

}