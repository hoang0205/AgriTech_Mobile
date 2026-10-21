package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

@Composable
fun SellerPickupScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onPickMapClick: (String, String) -> Unit,
    pickedAddress: String? = null,
    pickedProvince: String? = null,
    pickedWard: String? = null,
    pickedDetail: String? = null,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    pickedSelectionId: String? = null,
    viewModel: SellerPickupViewModel = hiltViewModel(),
    addressViewModel: UserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val provinces by addressViewModel.provinces
        .collectAsStateWithLifecycle()
    val wards by addressViewModel.wards
        .collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        addressViewModel.getProvinces()
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.loadFailed) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                state.error ?: "Không tải được địa điểm lấy hàng",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = viewModel::load) {
                Text("Tải lại")
            }

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    val storedParts = remember(state.address) {
        state.address
            .replace(
                Regex(
                    """,\s*(Việt Nam|Vietnam)\s*$""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val storedProvince = if (storedParts.size >= 3) {
        storedParts.last()
    } else {
        ""
    }

    val storedWard = if (storedParts.size >= 3) {
        storedParts[storedParts.lastIndex - 1]
    } else {
        ""
    }

    val storedDetail = if (storedParts.size >= 3) {
        storedParts.dropLast(2).joinToString(", ")
    } else {
        state.address
    }

    val hasMapResult = pickedSelectionId != null

    val formState: AddressState = when {
        state.isSaving -> AddressState.Loading
        state.saved -> AddressState.Success(emptyList())
        state.error != null -> AddressState.Error(
            state.error ?: "Không lưu được địa điểm lấy hàng"
        )
        else -> AddressState.Idle
    }

    AddAddressContent(
        onBackClick = {
            if (!state.isSaving) onBackClick()
        },
        onSaveSuccess = {
            viewModel.clearSaved()
            onSaveSuccess()
        },
        addressState = formState,
        provinces = provinces.map { it.name },
        wards = wards.map { it.name },
        isPickupLocation = true,
        pickedProvince = if (hasMapResult) {
            pickedProvince
        } else {
            storedProvince
        },
        pickedWard = if (hasMapResult) {
            pickedWard
        } else {
            storedWard
        },
        pickedDetail = if (hasMapResult) {
            pickedDetail ?: pickedAddress
        } else {
            storedDetail
        },
        pickedLatitude = if (hasMapResult) {
            pickedLatitude
        } else {
            state.latitude
        },
        pickedLongitude = if (hasMapResult) {
            pickedLongitude
        } else {
            state.longitude
        },
        pickedSelectionId = if (hasMapResult) {
            pickedSelectionId
        } else if (
            state.latitude != null &&
            state.longitude != null
        ) {
            "stored-pickup"
        } else {
            null
        },
        onPickMapClick = { addressQuery, areaQuery ->
            if (!state.isSaving) {
                onPickMapClick(addressQuery, areaQuery)
            }
        },
        onProvinceSelected = { provinceName ->
            val provinceCode = provinces
                .find { it.name == provinceName }
                ?.code

            if (provinceCode != null) {
                addressViewModel.getWards(provinceCode)
            }
        },
        onAddNewAddress = {
                _, _, address, _, latitude, longitude ->

            viewModel.changeAddress(address)

            viewModel.applyPickedLocation(
                selectionId = UUID.randomUUID().toString(),
                address = address,
                latitude = requireNotNull(latitude),
                longitude = requireNotNull(longitude)
            )

            viewModel.save()
        }
    )
}