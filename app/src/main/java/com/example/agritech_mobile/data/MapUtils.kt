package com.example.agritech_mobile.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class ParsedAddress(
    val fullAddress: String = "",
    val province: String = "",
    val ward: String = "",
    val detail: String = ""
)

suspend fun getAddressComponentsFromLatLng(
    context: Context,
    latitude: Double,
    longitude: Double
): ParsedAddress = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale("vi", "VN"))
        val addressList: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        cont.resume(emptyList())
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
        }

        val addr = addressList.firstOrNull() ?: return@withContext ParsedAddress(
            fullAddress = "Vị trí (${latitude}, ${longitude})"
        )

        val rawFull = addr.getAddressLine(0) ?: ""

        val cleanFull = rawFull
            .replace(Regex(",?\\s*(Việt Nam|Vietnam)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b\\d{5,6}\\b"), "")
            .trim()

        val parts = cleanFull.split(",").map { it.trim() }.filter { it.isNotBlank() }

        val province: String
        val ward: String
        val detail: String

        when {
            parts.size >= 3 -> {
                province = parts.last()
                ward = parts[parts.size - 2]
                detail = parts.subList(0, parts.size - 2).joinToString(", ")
            }
            parts.size == 2 -> {
                province = parts.last()
                ward = parts.first()
                detail = ""
            }
            parts.size == 1 -> {
                province = parts.first()
                ward = ""
                detail = ""
            }
            else -> {
                province = ""
                ward = ""
                detail = ""
            }
        }

        ParsedAddress(
            fullAddress = rawFull,
            province = province,
            ward = ward,
            detail = detail
        )
    } catch (e: Exception) {
        ParsedAddress(fullAddress = "Vị trí (${latitude}, ${longitude})")
    }
}