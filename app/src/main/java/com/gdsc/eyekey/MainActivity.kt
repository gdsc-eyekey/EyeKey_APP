package com.gdsc.eyekey

import android.app.Activity
import android.app.AlertDialog
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

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround: ImageView = findViewById(R.id.imagePreView)
                imageBackGround.setImageURI(result.data?.data)
            }
        }


    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted && permissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_LONG).show()
                    //url 받아서 image 넘기기
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                if(isGranted && permissionName == android.Manifest.permission.CAMERA){
                    Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_LONG).show()
                    //Todo Camera 어플 실행
                }
                else{
                    if(permissionName == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity, "Permission denied can't use external storgae",
                            Toast.LENGTH_LONG).show()
                    }
                    if(permissionName == android.Manifest.permission.CAMERA){
                        Toast.makeText(
                            this@MainActivity, "Permission denied can't use camera",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imagePreView)

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ibCamera : ImageButton = findViewById(R.id.ib_camera)
        ibCamera.setOnClickListener{
            requestCameraPermission()
        }

    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("EyeKey App", "This App " +
                    "needs to Access your External storage for getting image")
        }else{
            requestPermission.launch(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }
    private fun requestCameraPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("EyeKey App", "This App " +
                    "needs to Access your Camera for getting image")
        }else{
            requestPermission.launch(arrayOf(
                android.Manifest.permission.CAMERA
            ))
        }
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



}