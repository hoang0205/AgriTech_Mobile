package com.example.agritech_mobile.ui.order

import com.example.agritech_mobile.data.remote.dto.GhnArea
import java.text.Normalizer
import java.util.Locale

// Match whole address components only. An ambiguous name must remain a manual choice.
internal fun matchRecipientProvince(address: String, provinces: List<GhnArea>): GhnArea? {
    val parts = addressParts(address)
    val provinceName = parts.lastOrNull() ?: return null
    return provinces.filter { areaName(it.name) == provinceName }.singleOrNull()
}

internal fun matchRecipientWard(address: String, province: GhnArea, wards: List<GhnArea>): GhnArea? {
    val parts = addressParts(address)
    if (parts.lastOrNull() != areaName(province.name)) return null
    val components = parts.dropLast(1).toSet()
    return wards.filter { areaName(it.name) in components }.singleOrNull()
}

private fun addressParts(address: String): List<String> = address.split(',', ';', '\n')
    .map(::areaName).filter { it.isNotEmpty() }.let { parts ->
        if (parts.lastOrNull() in setOf("viet nam", "vietnam")) parts.dropLast(1) else parts
    }

private fun areaName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace('đ', 'd').replace('Đ', 'D')
    .lowercase(Locale.ROOT).trim()
    .replace(Regex("^(tinh|thanh pho|tp\\.?|xa|phuong|thi tran|p\\.)(?:\\s+|$)"), "")
    .replace(Regex("\\s+"), " ").trim()
