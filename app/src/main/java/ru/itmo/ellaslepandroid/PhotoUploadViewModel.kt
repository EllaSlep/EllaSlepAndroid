package ru.itmo.ellaslepandroid

import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.sdk.api.photos.PhotosService
import com.vk.sdk.api.photos.dto.PhotosGetAlbumsResponseDto
import com.vk.sdk.api.photos.dto.PhotosPhotoAlbumFullDto
import com.vk.sdk.api.photos.dto.PhotosPhotoDto
import com.vk.sdk.api.photos.dto.PhotosPhotoUploadDto
import ru.itmo.ellaslepandroid.PhotoAdapter

class PhotoUploadViewModel :ViewModel() {

    private val photoService = PhotosService()

    fun getUploadServer(photoAdapter : PhotoAdapter, albumId: Int?) {
        // Получаем адрес сервера для загрузки фотографий
        VK.execute(photoService.photosGetUploadServer(albumId), object : VKApiCallback<PhotosPhotoUploadDto> {
            override fun success(result: PhotosPhotoUploadDto) {
                val selectedPhotos = photoAdapter.getSelectedPhotos()
                uploadPhotos(result, selectedPhotos)
            }

            override fun fail(error: Exception) {
                Toast.makeText(
                    MainActivity(),
                    "Failed to get upload server",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun getAlbums(callback: (PhotosGetAlbumsResponseDto) -> Unit) {
        VK.execute(photoService.photosGetAlbums(), object : VKApiCallback<PhotosGetAlbumsResponseDto> {
            override fun success(result: PhotosGetAlbumsResponseDto) {
                callback(result)
            }

            override fun fail(error: Exception) {
                Toast.makeText(
                    MainActivity(),
                    "Failed to get upload server",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun uploadPhotos(uploadServer: PhotosPhotoUploadDto, selectedPhotos: List<PhotosPhotoAlbumFullDto>) {
        // Выполняем загрузку фотографий на сервер hgjhg
        for (photo in selectedPhotos){
            VK.execute(photoService.photosSave(uploadServer.albumId, photosList = photo.title), object : VKApiCallback<List<PhotosPhotoDto>> {
                override fun success(result: List<PhotosPhotoDto>) {
                    Toast.makeText(
                        MainActivity(),
                        "Photos uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun fail(error: Exception) {
                    Toast.makeText(
                        MainActivity(),
                        "Failed to upload photos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}