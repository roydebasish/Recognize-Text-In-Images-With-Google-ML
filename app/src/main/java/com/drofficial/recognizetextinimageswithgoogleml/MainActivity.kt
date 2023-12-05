package com.drofficial.recognizetextinimageswithgoogleml

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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.drofficial.recognizetextinimageswithgoogleml.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private companion object{
        private const val CAMERA_REQUEST_CODE =100;
        private const val STRAGE_REQUEST_CODE =101;
    }

    //Uri of the image that we will take from Camera/Gallery
    private var imageUri:Uri? = null

    //arrays of permission required to pick image from Camera/Gallery
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>

    private lateinit var progressDialog: ProgressDialog
    private lateinit var textRecognizer: TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init arrays of permissions required for Camera,Gallery
        cameraPermission = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.READ_MEDIA_IMAGES)
        storagePermission = arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)


        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait..")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        binding.btnTakeImage.setOnClickListener {
            showInputImageDialog()
        }


        binding.btnRecognizeText.setOnClickListener {
            //check if image is picked or not, picked if imageUri is not null
            if (imageUri == null){

                //imageUri is null , which means we haven't picked image yet,can't recognize text
                showToast("Pick Image First...")
            }else{

                //imageUri is not null, which means we have picked image,we can recognize text
                recognizeTextFromImage()
            }
        }

    }

    private fun recognizeTextFromImage(){
        //set message and show progress dialogue
        progressDialog.setTitle("Preparing Image...")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setTitle("Recognizing text...")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener {text ->
                    progressDialog.dismiss()
                    val recognizedText = text.text
                    binding.etRecognizedText.setText(recognizedText)
                }
                .addOnFailureListener {e->
                    progressDialog.dismiss()
                    showToast("Failed to recognizing text due to ${e.message}")
                }


        }catch (e:Exception){
            showToast("Failed to prepare image due to ${e.message}")
        }

    }

    private fun showInputImageDialog(){

        val popupMenu = PopupMenu(this,binding.btnTakeImage)
        popupMenu.menu.add(Menu.NONE,1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE,2,2,"GALLERY")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {menuItem ->
            //get item id that is clicked from PopupMenu
            val id = menuItem.itemId

            if (id == 1){
                //Camera is clicked, check if camera permissions are granted or not
                if (checkCameraPermission()){
                    pickImageFromCamera()
                }else{
                    //Camera permissions not granted, request the camera permissions
                    requestCameraPermission()
                }
            }else if (id == 2){
                //Gallery is clicked, check if storage permissions are granted or not
                if (checkStoragePermission()){
                    pickImageFromGallery()
                }else{
                    //Gallery permissions not granted, request the storage permissions
                    requestStoragePermission()
                }
            }
          return@setOnMenuItemClickListener true
        }
    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLuncher.launch(intent)
    }

    private val galleryActivityResultLuncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        //here we will receive the image,if picked
        if (result.resultCode == Activity.RESULT_OK){
            //image picked
            val data = result.data
            imageUri = data!!.data

            //set to imageview
            binding.ivSImageView.setImageURI(imageUri)
        }else{
            //cancelled
            showToast("Cancelled...!!")
        }
    }

    private fun pickImageFromCamera(){
        //get ready the image data to store in MediaStore
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION,"Sample Description")

        //image Uri
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        //intent to launch camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLuncher.launch(intent)
    }

    private val cameraActivityResultLuncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->

            //here we will receive the image,if taken from camera
            if (result.resultCode == Activity.RESULT_OK){
                //image is taken from camera
                //we already have the image in imageUri using function pickImageFromCamera
                binding.ivSImageView.setImageURI(imageUri)
            }else{
                showToast("Cancelled...!")
            }
        }

    private fun checkStoragePermission() : Boolean{
        //check storage permission allowed or not
        return ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)  == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() : Boolean{
        //check camera permission allowed or not
        val cameraResult = ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)  == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)  == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission(){
        //request Storage Permission
        ActivityCompat.requestPermissions(this,storagePermission, STRAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission(){
        //request Camera Permission
        ActivityCompat.requestPermissions(this,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted){
                        pickImageFromCamera()
                    }else{
                        showToast("Camera & Storage permission are required...")
                    }

                }
            }

            STRAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted){
                        pickImageFromGallery()
                    }else{
                        showToast("Storage permission are required...")
                    }

                }
            }
        }
    }

    private fun showToast(message:String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }

}