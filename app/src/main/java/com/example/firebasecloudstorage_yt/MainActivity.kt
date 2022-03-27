package com.example.firebasecloudstorage_yt

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebasecloudstorage_yt.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val REQUEST_CODE_IMAGE_PICK = 0
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var curFile: Uri? = null

    var imageRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it,REQUEST_CODE_IMAGE_PICK)
            }
        }

        binding.btnUploadImage.setOnClickListener {
            uploadImageToStorage("myImage")
        }

        binding.btnDownloadImage.setOnClickListener {
            downloadImage("myImage")
        }

        binding.btnDeleteImage.setOnClickListener {
            deleteImage("myImage")
        }

        listFiles()

    }

    private fun listFiles() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val images = imageRef.child("images/").listAll().await()
            val imageUrls = mutableListOf<String>()
            for (image in images.items) {
                val url = image.downloadUrl.await()
                imageUrls.add(url.toString())
            }
            withContext(Dispatchers.Main) {
                val imageAdapter = ImageAdapter(imageUrls)
                rvImages.apply {
                    adapter = imageAdapter
                    layoutManager = LinearLayoutManager(this@MainActivity)
                }
            }
        }  catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteImage(fileName: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            //delete image
            imageRef.child("images/$fileName").delete().await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"Succesfully deleted image.",Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadImage(fileName: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            //max size to download
            val maxDownloadSize = 5L * 1024 * 1024
            val bytes = imageRef.child("images/$fileName").getBytes(maxDownloadSize).await()
            // convert bytes to image
            val bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
            withContext(Dispatchers.Main) {
                binding.ivImage.setImageBitmap(bmp)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImageToStorage(fileName: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            //upload image
            curFile?.let {
                imageRef.child("images/$fileName").putFile(it).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Succesfully uploaded image",Toast.LENGTH_LONG).show()
                }

            }
        } catch (e: Exception){
           withContext(Dispatchers.Main) {
               Toast.makeText(this@MainActivity, e.message,Toast.LENGTH_LONG).show()
           }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_IMAGE_PICK) {
            data?.data?.let {
                curFile = it
                binding.ivImage.setImageURI(it)
            }
        }
    }
}