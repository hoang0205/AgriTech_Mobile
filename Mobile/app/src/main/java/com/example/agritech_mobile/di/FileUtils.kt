package com.example.agritech_mobile.di

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

fun uriToMultipartBodyPart(context: Context, uri: Uri, paramName: String = "file"): MultipartBody.Part? {
    val contentResolver = context.contentResolver

    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

    return try {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        MultipartBody.Part.createFormData(paramName, file.name, requestFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    val file = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
    return try {
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}