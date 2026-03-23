package com.example.agritech_mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.*


@Composable
fun SecurityTopBar(
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryDarkGreen,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.security),
                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = PrimaryDarkGreen
            )
        }
    }
}


@Composable
fun ForgotPasswordScreen(onBackClick: () -> Unit = {}, onSendCodeClick: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .statusBarsPadding()
    ) {
        SecurityTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryDarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "Lock Reset",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.forgot_password_x),
                style = Typography.headlineMedium,
                color = TextDarkGray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.forgot_password_desc),
                style = Typography.bodyLarge,
                color = TextLightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.email_address_label),
                    style = Typography.labelSmall,
                    color = TextDarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.enter_email_hint),
                            color = TextLightGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = PrimaryDarkGreen
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardBackgroundGray,
                        unfocusedContainerColor = CardBackgroundGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSendCodeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.send_code),
                    style = Typography.labelLarge
                )
//                if (uiState.isLoading) CircularProgressIndicator(
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    modifier = Modifier.size(24.dp)
//                )
//                else Text(
//                    stringResource(R.string.login_button),
//                    style = MaterialTheme.typography.labelLarge
//                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Text
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.padding(bottom = 32.dp)
//            ) {
//                Text(
//                    text = stringResource(id = R.string.trouble_receiving_code),
//                    style = Typography.bodySmall,
//                    color = TextDarkGray
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.clickable { /* Handle support click */ }
//                ) {
//                    Text(
//                        text = stringResource(id = R.string.contact_support),
//                        style = Typography.labelLarge,
//                        color = PrimaryDarkGreen
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Icon(
//                        imageVector = Icons.Default.SupportAgent,
//                        contentDescription = "Support",
//                        tint = PrimaryDarkGreen,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    email: String = "user@email.com",
    onBackClick: () -> Unit = {},
    onVerifyClick: () -> Unit = {}
) {
    var otpCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .statusBarsPadding()
    ) {
        SecurityTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.verify_email_title),
                style = Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = TextDarkGray)) {
                        append(stringResource(id = R.string.verify_email_desc_prefix))
                    }
                    withStyle(
                        style = SpanStyle(
                            color = PrimaryDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(" ", email)
                    }
                    withStyle(style = SpanStyle(color = TextDarkGray)) {
                        append(stringResource(id = R.string.verify_email_desc_suffix))
                    }
                },
                style = Typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGray)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    OtpInputField(
                        otpText = otpCode,
                        onOtpTextChange = { otpCode = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(id = R.string.didnt_receive_code),
                        style = Typography.bodySmall,
                        color = TextDarkGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = R.string.resend_in).format("00:45"),
                        style = Typography.labelLarge,
                        color = LightLeafGreen
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onVerifyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.verify_button),
                            style = Typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtpInputField(
    otpText: String,
    onOtpTextChange: (String) -> Unit,
    otpCount: Int = 6
) {
    BasicTextField(
        value = otpText,
        onValueChange = {
            if (it.length <= otpCount) {
                onOtpTextChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(otpCount) { index ->
                    val char = when {
                        index >= otpText.length -> ""
                        else -> otpText[index].toString()
                    }

                    val isFocused = otpText.length == index

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = if (isFocused) 1.5.dp else 0.dp,
                                color = if (isFocused) PrimaryDarkGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = Typography.headlineLarge.copy(fontSize = 24.sp),
                            color = PrimaryDarkGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ResetPasswordScreen(onBackClick: () -> Unit = {}, onUpdateClick: () -> Unit = {}) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .statusBarsPadding()
    ) {
        SecurityTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.reset_password),
                style = Typography.headlineLarge,
                color = TextDarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.reset_password_desc),
                style = Typography.bodyLarge,
                color = TextLightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundGray)
                    .border(
                        width = 4.dp,
                        color = PrimaryDarkGreen,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(start = 4.dp)
                    .background(CardBackgroundGray)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = PrimaryDarkGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.security_policy_text),
                    style = Typography.bodySmall.copy(lineHeight = 20.sp),
                    color = TextDarkGray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PasswordField(
                label = stringResource(id = R.string.new_password_label),
                value = newPassword,
                onValueChange = { newPassword = it },
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(24.dp))

            PasswordField(
                label = stringResource(id = R.string.confirm_new_password_label),
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                isVisible = confirmPasswordVisible,
                onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUpdateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.update_password),
                    style = Typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = Typography.labelSmall,
            color = TextDarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardBackgroundGray,
                unfocusedContainerColor = CardBackgroundGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onVisibilityChange) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password Visibility",
                        tint = TextLightGray
                    )
                }
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    AgritechTheme {
        ForgotPasswordScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun VerifyEmailScreenPreview() {
    AgritechTheme {
        VerifyEmailScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordScreenPreview() {
    AgritechTheme {
        ResetPasswordScreen()
    }
}