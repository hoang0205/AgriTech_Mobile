package com.example.agritech_mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.cart.CartScreen
import com.example.agritech_mobile.ui.dashboard.HomeScreen

@Composable
fun MainScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateProduct: () -> Unit,
    onNavigateToCheckout: (String) -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf("HOME") }

    Scaffold(
        bottomBar = {
            AgritechBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { selectedTab -> currentTab = selectedTab },
                onCreateProductClick = onNavigateToCreateProduct
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                "HOME" -> HomeScreen(
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToCreateProduct = onNavigateToCreateProduct
                )

                "ORDERS" -> {
                    // TODO:
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("Màn hình Đơn hàng") }
                }

                "CART" -> CartScreen(
                    onNavigateToCheckout = onNavigateToCheckout
                )

                "ACCOUNT" -> {
                    // TODO:
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("Màn hình Tài khoản") }
                }
            }
        }
    }
}


@Composable
fun AgritechBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onCreateProductClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.nav_home),
                    isSelected = currentTab == "HOME",
                    onClick = { onTabSelected("HOME") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Outlined.ReceiptLong,
                    label = stringResource(R.string.nav_orders),
                    isSelected = currentTab == "ORDERS",
                    onClick = { onTabSelected("ORDERS") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(60.dp))
                BottomNavItem(
                    icon = Icons.Outlined.ShoppingCart,
                    label = stringResource(R.string.nav_cart),
                    isSelected = currentTab == "CART",
                    onClick = { onTabSelected("CART") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Outlined.AccountCircle,
                    label = stringResource(R.string.nav_account),
                    isSelected = currentTab == "ACCOUNT",
                    onClick = { onTabSelected("ACCOUNT") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-15).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onCreateProductClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
    val formattedLabel = label.lowercase().split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedLabel,
                fontSize = with(LocalDensity.current) { 10.dp.toSp() },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
    }
}