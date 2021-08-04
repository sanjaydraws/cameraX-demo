package com.example.cameraxapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxapp.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//add permission in manifest
//<uses-feature android:name="android.hardware.camera.any" /> // // it makes sure that device has camera
//<uses-permission android:name="android.permission.CAMERA" />
class MainActivity : AppCompatActivity() {
    var binding:ActivityMainBinding ? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Request Camera Permission
        if(allPermissionsGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }

        // to take photo
        binding?.cameraCaptureButton?.setOnClickListener {
            takePhoto()
        }
        binding?.cameraSwitch?.setOnClickListener {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
        }
//        implement the outputDirectory and cameraExecutor
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getOutputDirectory():File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir() } }
        return if(mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    //Implement Preview use case
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            //a singleton Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //viewfinder is used to let the user preview the photo they will be taking. can  viewfinder using the CameraX Preview class.
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build() // use cases

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

   //    First, you define a configuration object that is used to instantiate the actual use case object
   //    Implement ImageCapture use case
   private fun takePhoto() {
       // Get a stable reference of the modifiable image capture use case
       // it the use case is null exit to the function
//       This will be null If you tap the photo button before image capture is set up. Without the return statement, the app would crash if it was null.
       val imageCapture = imageCapture ?: return

       //create timestamp photo file to hold image(for unique)
       val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

       // create outputOption object which contains file + metadata
       //  OutputFileOptions object where u can specify things about how your output would be
       val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

       // set up image capture listener , which is triggered after photo has been taken
       imageCapture.takePicture(
           outputOption,
           ContextCompat.getMainExecutor(this),
           object :ImageCapture.OnImageSavedCallback{
               override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                   val savedUri = Uri.fromFile(photoFile)
                   val msg = "Photo capture succeeded: $savedUri"
                   Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                   Log.d(TAG, msg)
               }

               override fun onError(exc: ImageCaptureException) {
                   Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
               }

           })

   }

}