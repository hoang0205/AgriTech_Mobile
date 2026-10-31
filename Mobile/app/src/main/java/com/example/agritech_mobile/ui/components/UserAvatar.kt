package com.example.agritech_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun UserAvatar(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription,
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = { AvatarPlaceholder() },
        error = { AvatarPlaceholder() }
    )
}

@Composable
private fun AvatarPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFF558B2F),
            modifier = Modifier.fillMaxSize().padding(5.dp)
        )
    }
}
