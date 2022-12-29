package com.sanghm2.recognizetext

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var inputImageBtn :MaterialButton
    private lateinit var recognizeTextBtn :MaterialButton
    private lateinit var  imageIv : ImageView
    private lateinit var recognizeTextEdit : EditText
    private lateinit var textRecognize : TextRecognizer

    private companion object {
        private const val  CAMERA_REQUEST_CODE = 100
        private const val  STORAGE_REQUEST_CODE = 101
    }

    private var  imageUri : Uri? = null
    private lateinit var cameraPermission : Array<String>
    private lateinit var storagePermission : Array<String>

    private lateinit var progressDialog : ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputImageBtn = findViewById(R.id.takePictureBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        imageIv  = findViewById(R.id.imageIv)
        recognizeTextEdit = findViewById(R.id.recognizeTextEdit)

        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognize = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        inputImageBtn.setOnClickListener {
            showInputImageDialog()
        }

        recognizeTextBtn.setOnClickListener {
            if(imageUri == null){
                showToast("Pick Image First...")
            }else {
                recognizeTextFromImage()
            }
        }
    }

    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image...")
        progressDialog.show()
        try {
            val  inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setMessage("Recognize text...")
            val textTaskResult = textRecognize.process(inputImage)
                .addOnSuccessListener {
                    val recognizeText  = it.text
                    recognizeTextEdit.setText(recognizeText)
                    progressDialog.dismiss()
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to  ${it.message}")
                }
        }catch (e: Exception){
            showToast("Fail to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(this, inputImageBtn)

        popupMenu.menu.add(Menu.NONE , 1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE , 2,2,"GALLERY")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem->

            val id = menuItem.itemId
            if(id == 1){
                if(checkCameraPermission()){
                    pickImageCamera()
                }else {
                    requestCameraPermission()
                }
            }else if(id == 2){
                if(checkStoragePermission()){
                    pickImageGallery()
                }
                else {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }


    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    private val galleryActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data!!.data
            imageIv.setImageURI(imageUri)
        }else {

        }
    }
    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE , "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION , "Sample Description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        val intent  = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLauncher.launch(intent)
    }
    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        if(result.resultCode == Activity.RESULT_OK){
            imageIv.setImageURI(imageUri)
        }else {
            showToast("Cancelled....")
        }

    }
    private fun checkStoragePermission() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkCameraPermission(): Boolean {
        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission, STORAGE_REQUEST_CODE)
    }
    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                    if(grantResults.isNotEmpty()){
                        val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                        val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                        if(cameraAccepted && storageAccepted){
                            pickImageCamera()
                        }else {
                            showToast("Camera & Storage permission are required...")
                        }
                    }
            }
            STORAGE_REQUEST_CODE->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){
                        pickImageGallery()
                    }else {
                        showToast("Storage permission are required...")
                    }
                }
            }
        }
    }
    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}