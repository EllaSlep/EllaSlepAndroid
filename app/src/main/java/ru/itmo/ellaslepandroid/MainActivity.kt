package ru.itmo.ellaslepandroid

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAuthenticationResult
import com.vk.api.sdk.auth.VKScope
import com.vk.dto.common.id.UserId
import com.vk.sdk.api.photos.dto.PhotosPhotoAlbumFullDto
import com.vk.sdk.api.photos.dto.PhotosPhotoSizesDto
import com.vk.sdk.api.photos.dto.PhotosPhotoSizesTypeDto

class MainActivity : AppCompatActivity() {
    private val READ_STORAGE_PERMISSION_REQUEST = 123
    private val VK_UPLOAD_PERMISSION_REQUEST = 124
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var viewModel: PhotoUploadViewModel
    private lateinit var uploadButton: Button
    private var isVKPermissionRequested = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        setContentView(R.layout.activity_photo_upload)
        Log.d("onCreate", "started")

        photoAdapter = PhotoAdapter()

        requestGalleryPermission()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(
            this,
            3
        ) // Используем сетку с 3 столбцами
        recyclerView.adapter = photoAdapter

        viewModel = ViewModelProvider(this).get(PhotoUploadViewModel::class.java)

        loadPhotos()

        uploadButton = findViewById(R.id.uploadButton)
    }

    override fun onStart() {
        super.onStart()
        if (!isVKPermissionRequested) {
            requestVKPermission()
            isVKPermissionRequested = true
        }
    }

    private fun requestVKPermission() {
        val authLauncher = VK.login(this) { result: VKAuthenticationResult ->
            when (result) {
                is VKAuthenticationResult.Success -> {
                    uploadPhotosToAlbum()
                }

                is VKAuthenticationResult.Failed -> {
                    Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        authLauncher.launch(arrayListOf(VKScope.PHOTOS))
    }

    private fun checkWriteExternalPermission(): Boolean {
        val res: Int = ContextCompat.checkSelfPermission(
            this,
            READ_EXTERNAL_STORAGE)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun requestGalleryPermission() {
        if (checkWriteExternalPermission()
        ) {
            // Проверяем, должны ли мы показать объяснение, почему нужно это разрешение
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    READ_EXTERNAL_STORAGE
                )
            ) {
                // Показываем объяснение пользователю и запрашиваем разрешение
                showPermissionExplanationDialog()
            } else {
                // Просто запрашиваем разрешение
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_REQUEST
                )
            }
        } else {
            // Разрешение уже предоставлено
            // loadPhotos()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("We need permission to access photos in order to use this feature.")
            .setPositiveButton("OK") { _, _ ->
                // Запрашиваем разрешение после объяснения
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_REQUEST
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Действия при отмене запроса разрешения
            }
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение предоставлено
                    loadPhotos()
                } else {
                    // Разрешение не предоставлено, показываем диалог
                    showPermissionDeniedDialog()
                }
            }
        }
    }


    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("You need to grant permission to access photos in order to use this feature.")
            .setPositiveButton("Retry") { _, _ ->
                requestGalleryPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Действия при отмене запроса разрешения
            }
            .show()
    }

    private fun loadPhotos() {
        val photoList: MutableList<PhotosPhotoAlbumFullDto> = mutableListOf()

        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val imagePath = it.getString(columnIndex)
                Log.d("PhotoUploadActivity", "Image path: $imagePath")

                // Convert file path to Uri
                val imageUri = Uri.parse("file://$imagePath")

                val photo = PhotosPhotoAlbumFullDto(id = 0, ownerId = UserId(0), size = 0, title = "", sizes = listOf(
                    PhotosPhotoSizesDto(height = 0, type = PhotosPhotoSizesTypeDto.X, width = 0, url = imageUri.toString())
                ))
                photoList.add(photo)
            }
        }

        photoAdapter.setPhotoList(photoList)
    }

    private fun uploadPhotosToAlbum() {
        Log.d("uploadPhotosToAlbum", "started")
        val selectedPhotos = photoAdapter.getSelectedPhotos()

        if (selectedPhotos.isNotEmpty()) {
            // Выполняйте действия для загрузки фотографий в альбом VK
            // Например, используйте VK SDK для получения адреса сервера для загрузки
            // и последующей загрузки фотографий на этот сервер
            val albumId = 301111354
            viewModel.getUploadServer(photoAdapter, albumId)
        } else {
            Toast.makeText(this, "Select at least one photo", Toast.LENGTH_SHORT).show()
        }
    }
}


class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private var photoList: List<PhotosPhotoAlbumFullDto> = listOf()
    private val selectedPhotos: MutableSet<Int> = mutableSetOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photoList[position]
        Glide.with(holder.itemView)
            .load(photo.sizes?.find { it.type == PhotosPhotoSizesTypeDto.X }?.url)
            .centerCrop()
            .into(holder.photoImageView)

        holder.itemView.setOnClickListener {
            toggleSelection(position)
        }

        // Пример обработки состояния выбора
        if (selectedPhotos.contains(position)) {
            // Элемент выбран
            holder.itemView.setBackgroundResource(R.drawable.selected_background)
        } else {
            // Элемент не выбран
            holder.itemView.setBackgroundResource(0)
        }
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    fun setPhotoList(newList: List<PhotosPhotoAlbumFullDto>) {
        photoList = newList
        notifyDataSetChanged()
    }

    private fun toggleSelection(position: Int) {
        if (selectedPhotos.contains(position)) {
            selectedPhotos.remove(position)
        } else {
            selectedPhotos.add(position)
        }
        notifyItemChanged(position)
    }

    fun getSelectedPhotos(): List<PhotosPhotoAlbumFullDto> {
        return selectedPhotos.mapNotNull { position ->
            photoList.getOrNull(position)
        }
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
    }
}