package com.example.agritech_mobile.data.repository

import android.util.Log
import com.example.agritech_mobile.data.remote.ProductApiService
import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ProductRequest
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val apiService: ProductApiService
) {
    suspend fun updateStock(productId: String, quantity: Double): Result<Unit> {
        return try {
            val response = apiService.updateStock(productId, mapOf("quantity" to quantity))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(when (response.code()) {
                400 -> "Tồn kho phải là số hợp lệ và không được âm."
                403 -> "Bạn chỉ được sửa sản phẩm của mình."
                else -> "Không cập nhật được tồn kho (${response.code()})."
            }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Không kết nối được máy chủ. Vui lòng thử lại."))
        }
    }

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
        imageUrls: List<String>,
        costPrice: Double? = null
    ): Result<ProductResponse> {
        val productRequest = ProductRequest(
            name = name,
            category = category,
            price = price,
            quantity = quantity,
            unit = unit,
            description = description,
            imageUrls = imageUrls,
            costPrice = costPrice
        )
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

    suspend fun getMyProducts(
        page: Int = 0,
        size: Int = 12
    ): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.getMyProducts(
                page = page,
                size = size
            )

            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val body = response.body()
                ?: error("Máy chủ không trả về danh sách sản phẩm")

            Result.success(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(
        query: String,
        page: Int = 0,
        size: Int = 10,
        farmerId: String? = null
    ): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.searchProducts(
                query = query.trim(),
                page = page,
                size = size,
                farmerId = farmerId
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        IllegalStateException("Phản hồi từ server bị rỗng")
                    )
                }
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSuggestedProducts(
        query: String,
        farmerId: String? = null
    ): Result<List<String>> {
        return try {
            val response = apiService.getSuggestedProducts(
                query = query.trim(),
                farmerId = farmerId
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        IllegalStateException("Phản hồi từ server bị rỗng")
                    )
                }
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
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

    suspend fun getRecommendations(): Result<List<ProductResponse>> {
        return try {
            val response = apiService.getRecommendations()
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

    suspend fun getProductsByFarmerId(farmerId: String): Result<PageResponse<ProductResponse>> {
        return try {
            val response = apiService.getProductsByFarmerId(farmerId)
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