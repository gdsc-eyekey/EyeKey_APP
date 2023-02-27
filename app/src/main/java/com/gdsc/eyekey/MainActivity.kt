package com.gdsc.eyekey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var imageView:ImageView? = null

    private val CAMERA_PERMISSION = arrayOf(android.Manifest.permission.CAMERA)
    private val STORAGE_PERMISSION = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    // using at startActivityForResult
    private  val PERMISSON_CAMERA = 1
    private  val PERMISSON_STORAGE = 2
    private  val REQUEST_CAMERA = 3
    private  val REQUEST_STORAGE = 4
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imagePreView)
        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            openGallery()
        }

        val ibCamera : ImageButton = findViewById(R.id.ib_camera)
        ibCamera.setOnClickListener{
            openCamera()
        }

    }

    private fun checkPermission(permissons: Array<out String>, flag: Int):Boolean{
        for(permisson in permissons){
            if(ContextCompat.checkSelfPermission(this, permisson) !=
                PackageManager.PERMISSION_GRANTED){
                    androidx.core.app.ActivityCompat.requestPermissions(this, permissons, flag)
                return false
            }
        }
        return true
    }

    private fun openCamera(){
        if(checkPermission(CAMERA_PERMISSION, PERMISSON_CAMERA)){
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }
    private fun openGallery(){
        if(checkPermission(STORAGE_PERMISSION, PERMISSON_STORAGE)){
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(intent, REQUEST_STORAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                REQUEST_CAMERA -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    imageView?.setImageBitmap(bitmap)
                    // TODO: 갤러리에 사진 저장 기능 넣기
                }
                REQUEST_STORAGE -> {
                    val uri = data?.data
                    imageView?.setImageURI(uri)
                }
            }

        }
    }





}