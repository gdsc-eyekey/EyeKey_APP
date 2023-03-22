package com.gdsc.eyekey

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.SensorManager.getOrientation
import android.media.ExifInterface
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.transition.Transition
import android.util.Base64
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.CustomTarget
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
import java.io.*
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : AppCompatActivity() {

    // ViewBinding
    lateinit var binding: ActivityMainBinding

    private var imageView: ImageView? = null

    var customnProgressDialog: Dialog? = null

    //photo
    private var photoUri: Uri? = null
    private var mCurrentPhotoPath: String? = null
    private var exif: ExifInterface? = null

    //record mp3
    private var outputPath: String? = null
    private var state: Boolean = false
    private var soundUri: Uri? = null
    private var recorder: MediaRecorder? = null
    private var resultUri: Uri? = null

    //base64 img
    private var resultImg: String? = null

    //result

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
        val permissionList: MutableList<String> = mutableListOf()
        for (permission in permissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission)
            }
        }
        if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionList.toTypedArray(),
                PERMISSIONS_REQUEST
            )
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
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
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
            dispatchTakePictureIntent()
        }

        val ibMike: ImageButton = findViewById(R.id.ib_mike)
        ibMike.setOnClickListener {
            if (!state) {
                startRecord()
            } else {
                stopRecord()
            }
        }
    }

    //get exif code and rotation orientation of photo
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getOrientation(path: String): Int {
        try {
            exif = ExifInterface(File(path))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientation = exif!!.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        return orientation
    }

    //img 회전 돌리기
    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1F, 1F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90F)
            else -> return bitmap
        }
        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun setImageBitmapWithoutRotation(uri: Uri, imageView: ImageView, bitmap: Bitmap) {
        val exif = getContentResolver().openInputStream(uri)?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            else -> { } // Do nothing
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        imageView.setImageBitmap(rotatedBitmap)
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoUri = FileProvider.getUriForFile(
                        this,
                        "com.gdsc.eyekey.fileProvider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val storageDir: File? = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Image captured and saved to fileUri specified in the Intent
            val file = File(mCurrentPhotoPath)
            galleryAddPic()
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
//            val base64Str = loadImageFile(mCurrentPhotoPath)
//            Log.d("base64", "${base64Str}"
//            )
//            saveJpgFileFromBase64(base64Str, mCurrentPhotoPath)

            if (mCurrentPhotoPath != null) {
                Log.d("mCurrentPhotoPath", "${mCurrentPhotoPath}")
                //imagebackground에 보여주기
                val imageBackground: ImageView = findViewById(R.id.imagePreView)
                Glide.with(this).load(photoUri).into(imageBackground);
            }
        }
    }
    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(mCurrentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    // Load the image file and encode it to Base64
    fun loadImageFile(filePath: String?): String? {
        val file = filePath?.let { File(it) }
        if (file != null) {
            if (!file.exists()) {
                return null
            }
        }
        val byteArray = file?.readBytes()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Convert a Base64-encoded string to a JPG file and save it to disk
    fun saveJpgFileFromBase64(base64Str: String?, filePath: String?): Boolean {
        try {
            val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val file = filePath?.let { File(it) }
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            Log.d("base64 성공", "${filePath}")
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


    //음성 녹음
    private fun startRecord() {

        val fileName: String = Date().getTime().toString() + ".mp3"
        outputPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName //내장메모리 밑에 위치
        recorder = MediaRecorder()
        recorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        recorder?.setOutputFormat((MediaRecorder.OutputFormat.AMR_WB))
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
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
                val imageRequestBody1 = file1.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val audioRequestBody2 = file2.asRequestBody("audio/AMR-WB".toMediaTypeOrNull())
                val filePart1 =
                    MultipartBody.Part.createFormData("file1", file1.name, imageRequestBody1)
                val filePart2 =
                    MultipartBody.Part.createFormData("file2", file2.name, audioRequestBody2)

                var gson = GsonBuilder().setLenient().create()

                val okHttpClient = OkHttpClient().newBuilder()
                    .retryOnConnectionFailure(true)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://34.64.228.205:5000/")
                    .client(okHttpClient)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

                val api: APIs = retrofit.create(APIs::class.java)

                val callResultImg = api.uploadFiles(filePart1, filePart2)

                callResultImg.enqueue(object : retrofit2.Callback<ResultImg> {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun onResponse(
                        call: Call<ResultImg>,
                        response: Response<ResultImg>
                    ) {
                        Log.d("POST", "성공 : ${response.toString()}")
                        val body = response.body()?.let {
                            resultImg = it.resultImg
                            Log.d("POST", "성공 resultImg : ${resultImg}")
                            val imageBackground: ImageView = findViewById(R.id.imagePreView)
                            val imageBytes = Base64.decode(resultImg, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                            val context = getApplicationContext()
                            val resolver = context.contentResolver
                            resultUri = getImageUri(context, decodedImage)
                            Log.d("resultURI", "성공 resultURI : ${resultUri}")
                            setImageBitmapWithoutRotation(resultUri!!,imageBackground, decodedImage)
//                            imageBackground.setImageBitmap(rotateBitmapFile)
//                            Glide.with(this@MainActivity).load(resultUri).into(imageBackground);
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

        val imageBackground: ImageView = findViewById(R.id.imagePreView)


    }

    fun getImageUri(context: Context, image: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, image, "Title", null)
        return Uri.parse(path)
    }

    fun exportUri(uri:Uri): Uri{
        return uri
    }
}

//bitmap compress이용 -> 화질 저하 이슈
//    private fun dispatchTakePictureIntent() {
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            takePictureIntent.resolveActivity(packageManager)?.also {
//                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
//            }
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            val imageBitmap = data?.extras?.get("data") as Bitmap
//            saveImageToGallery(imageBitmap)
//        }
//    }
//
//    private fun saveImageToGallery(bitmap: Bitmap) {
//        val imageFileName = "JPEG_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".jpeg"
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//        }
//
//        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//
//        uri?.let {
//            val outputStream = contentResolver.openOutputStream(uri)
//            outputStream?.use {
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//                Toast.makeText(this, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
//            }
//        }

        //        try {
//            val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
//            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
//            val file = filePath?.let { File(it) }
//            val fos = FileOutputStream(file)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
//            fos.flush()
//            fos.close()
//            Log.d("base64 성공", "${filePath}")
//            return true
//        } catch (e: IOException) {
//            e.printStackTrace()
//            return false
//        }


//    fun saveJpgFileFromBase64(base64Str: String?, filePath: String?): Boolean {
//        try {
//            val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
//            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
//            val file = filePath?.let { File(it) }
//            val fos = FileOutputStream(file)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
//            fos.flush()
//            fos.close()
//            Log.d("base64 성공", "${filePath}")
//            return true
//        } catch (e: IOException) {
//            e.printStackTrace()
//            return false
//        }
//    }

        //방법2 -> 기존 실패 방법
//    //사진 임시파일 생성
//    @Throws(IOException::class)
//    fun createImageFile(): File? { // Create an image file name
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val imageFileName = "JPEG_$timeStamp.jpeg"
//        var imageFile: File? = null
//        val storageDir = File(
//            Environment.getExternalStorageDirectory().toString()+"/Pictures",
//            "Eyekey"
//        )
//        if (!storageDir.exists()) {
//            storageDir.mkdirs()
//        }
//        imageFile = File(storageDir, imageFileName)
//        mCurrentPhotoPath = imageFile.absolutePath
//        return imageFile
//    }
//
//    //카메라 실행 및 데이터 전달
//    private fun captureCamera() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//
//            var photoFile: File? = null
//            try {
//                photoFile = createImageFile()
//            } catch (ex: IOException) {
//                Log.e("captureCamera Error", ex.toString())
//                return
//            }
//            if (photoFile != null) { // getUriForFile의 두 번째 인자는 Manifest provier의 authorites와 일치해야 함
//                val providerURI =
//                    FileProvider.getUriForFile(this, "com.gdsc.eyekey.fileProvider", photoFile)
//                // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI)
//                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
//            }
//        }
//    }
//    //카메라에서 찍은 사진 결과호출
//    @RequiresApi(Build.VERSION_CODES.Q)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            REQUEST_TAKE_PHOTO -> {
//                Log.i("REQUEST_TAKE_PHOTO", "${Activity.RESULT_OK}" + " " + "${resultCode}");
//                if (resultCode == RESULT_OK) {
//                    try {
//                        galleryAddPic();
//                    } catch (e: Exception) {
//                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
//                    }
//
//                } else {
//                    Toast.makeText(this@MainActivity, "사진찍기를 취소하였습니다.", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    }
//    //이미지 로컬폴더에 저장
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun galleryAddPic() {
//        Log.i("galleryAddPic", "Call")
//        val mediaScanIntent: Intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
//        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
//        val f: File = File(mCurrentPhotoPath)
//        val contentUri: Uri = Uri.fromFile(f)
//        mediaScanIntent.setData(contentUri)
//        sendBroadcast(mediaScanIntent)
//        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show()
//        if(mCurrentPhotoPath != null){
//            //imagebackground에 보여주기
//            val imageBackground: ImageView = findViewById(R.id.imagePreView)
//            val myBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)
//            val bmRotated = rotateBitmap(myBitmap, getOrientation(mCurrentPhotoPath!!))
//            imageBackground.setImageBitmap(bmRotated)
//        }
//    }


