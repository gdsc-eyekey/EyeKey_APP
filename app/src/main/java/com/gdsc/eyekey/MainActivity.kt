package com.gdsc.eyekey

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gdsc.eyekey.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var imageView:ImageView? = null
    var customnProgressDialog : Dialog? = null

    // 파일 불러오기
    private val getContentImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null){
            uri.let { binding?.imagePreView?.setImageURI(uri)}
            val imageBackground:ImageView = findViewById(R.id.imagePreView)
            imageBackground.setImageURI(uri)
        }


    }

    // 카메라를 실행한 후 찍은 사진을 저장
    var pictureUri: Uri? = null
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
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE)

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
            //Todo add mike function
        }

    }
    private fun createImageFile(): Uri? {
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


    private fun showProgressDialog(){
        customnProgressDialog = Dialog(this@MainActivity)
        customnProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customnProgressDialog?.show()
    }

}