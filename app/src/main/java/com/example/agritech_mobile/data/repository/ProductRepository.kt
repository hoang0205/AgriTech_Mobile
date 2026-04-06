package com.example.agritech_mobile.data.repository

import android.util.Log
import com.example.agritech_mobile.data.remote.ProductApiService
import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ProductRequest
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val apiService: ProductApiService
) {
    suspend fun getProducts(): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.getProducts()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun createProduct(
        name: String,
        category: String,
        price: Double,
        quantity: Double,
        unit: String,
        description: String,
        imageUrls: List<String>
    ): Result<ProductResponse> {
        val productRequest = ProductRequest(name, category, price, quantity, unit, description, imageUrls)
        return try {
            Log.d("CreateProduct", "Sending imageUrls: $imageUrls")

            val response = apiService.createProduct(productRequest)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("CreateProduct", "Response imageUrls: ${body.imageUrls}")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Log.e("CreateProduct", "Error: ${response.code()}")
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CreateProduct", "Exception: ${e.localizedMessage}")
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }


    suspend fun getProductById(productId: String): Result<ProductResponse> {
        return try {
            val response = apiService.getProductById(productId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun searchProducts(query: String): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.searchProducts(query)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getRandomProducts(): Result<List<ProductResponse>> {
        return try {
            val response = apiService.getRandomProducts()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getProductsByCategory(category: String): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.getProductsByCategory(category)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getSuggestedProducts(query: String): Result<List<String>> {
        return try {
            val response = apiService.getSuggestedProducts(query)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }
}