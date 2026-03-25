package com.example.agritech_mobile.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.delay


data class SecurityUiState(
    val email: String = "",
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val otp: String = "",

    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null
)


@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit = {},
    onSendCodeClick: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SecurityUiState()) }
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onSendCodeClick(uiState.email)
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).error
                uiState = uiState.copy(errorMessage = errorMsg)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
    fun handleSendCode() {
        if (uiState.email.isNotBlank()) {
            viewModel.requestResetPassword(uiState.email)
        }
    }

    ForgotPasswordContent(
        uiState = uiState.copy(isLoading = authState is AuthState.Loading),
        onEmailChange = { uiState = uiState.copy(email = it, errorMessage = null) },
        onBackClick = onBackClick,
        onSendCodeClick = { handleSendCode() }
    )
}

@Composable
fun ForgotPasswordContent(
    uiState: SecurityUiState,
    onEmailChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSendCodeClick: () -> Unit
) {
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
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryDarkGreen), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
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
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.forgot_password_desc),
                style = Typography.bodyLarge,
                color = TextLightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.enter_email_hint),
                            color = TextLightGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id = R.string.send_code), style = Typography.labelLarge)
            }
        }
    }
}


@Composable
fun VerifyEmailScreen(
    email: String,
    onBackClick: () -> Unit = {},
    onVerifyClick: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SecurityUiState(email = email)) }
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onVerifyClick(uiState.otpCode)
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).error
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    fun handleVerify() {
        if (uiState.otpCode.length == 6) {
            viewModel.verifyEmail(uiState.email, uiState.otpCode)
        } else {
            Toast.makeText(context, "Mã OTP phải gồm 6 số", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleResend() {
        viewModel.requestResetPassword(uiState.email)
        Toast.makeText(context, "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show()
    }

    VerifyEmailContent(
        email = uiState.email,
        uiState = uiState,
        onOtpChange = { uiState = uiState.copy(otpCode = it) },
        onBackClick = onBackClick,
        onVerifyClick = { handleVerify() },
        onResendClick = { handleResend() }
    )
}

@Composable
fun VerifyEmailContent(
    email: String,
    uiState: SecurityUiState,
    onOtpChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit = {}
) {
    var timeLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }
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
                text = stringResource(R.string.verify_email_title),
                style = Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = TextDarkGray)) { append(stringResource(R.string.verify_email_desc_prefix)) }
                    withStyle(
                        style = SpanStyle(
                            color = PrimaryDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append(" " + email) }
                },
                style = Typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
                    OtpInputField(otpText = uiState.otpCode, onOtpTextChange = onOtpChange)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.didnt_receive_code),
                        style = Typography.bodySmall,
                        color = TextDarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (timeLeft > 0) {
                        Text(
                            text = stringResource(
                                R.string.resend_in,
                                timeLeft.toString().padStart(2, '0')
                            ),
                            style = Typography.labelLarge,
                            color = TextLightGray
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.resend),
                            style = Typography.labelLarge,
                            color = PrimaryDarkGreen,
                            modifier = Modifier
                                .clickable {
                                    onResendClick()
                                    timeLeft = 60
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onVerifyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.verify_button),
                            style = Typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ResetPasswordScreen(
    email: String,
    otp: String,
    onBackClick: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SecurityUiState(email = email, otpCode = otp)) }
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onUpdateClick()
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).error
                uiState = uiState.copy(errorMessage = errorMsg)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    fun handleResetPassword() {
        var passError: String? = null
        var isValid = true
        var confirmError: String? = null
        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        if (!passwordRegex.matches(uiState.newPassword)) {
            passError = R.string.security_policy_text.toString()
            isValid = false
        }

        if (uiState.newPassword != uiState.confirmPassword) {
            confirmError = R.string.password_error.toString()
            isValid = false
        }

        uiState = uiState.copy(
            newPasswordError = passError,
            confirmPasswordError = confirmError
        )

        if (isValid) {
            viewModel.resetPassword(uiState.email, uiState.newPassword)
        }
    }

    ResetPasswordContent(
        uiState = uiState,
        onNewPasswordChange = { uiState = uiState.copy(newPassword = it, newPasswordError = null) },
        onConfirmPasswordChange = {
            uiState = uiState.copy(confirmPassword = it, confirmPasswordError = null)
        },
        onTogglePassword = {
            uiState = uiState.copy(isPasswordVisible = !uiState.isPasswordVisible)
        },
        onToggleConfirmPassword = {
            uiState = uiState.copy(isConfirmPasswordVisible = !uiState.isConfirmPasswordVisible)
        },
        onBackClick = onBackClick,
        onUpdateClick = { handleResetPassword() }
    )
}

@Composable
fun ResetPasswordContent(
    uiState: SecurityUiState,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirmPassword: () -> Unit,
    onBackClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
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
                text = stringResource(R.string.reset_password),
                style = Typography.headlineLarge,
                color = TextDarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.security_policy_text),
                style = Typography.bodySmall,
                color = TextLightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            PasswordField(
                label = stringResource(R.string.new_password_label),
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                isVisible = uiState.isPasswordVisible,
                onVisibilityChange = onTogglePassword,
                errorMessage = uiState.newPasswordError,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PasswordField(
                label = stringResource(R.string.confirm_new_password_label),
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                isVisible = uiState.isConfirmPasswordVisible,
                onVisibilityChange = onToggleConfirmPassword,
                errorMessage = uiState.confirmPasswordError
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUpdateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(R.string.update_password), style = Typography.labelLarge)
            }
        }
    }
}

@Composable
fun SecurityTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PrimaryDarkGreen,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBackClick() })
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.security),
            style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryDarkGreen
        )
    }
}

@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    errorMessage: String? = null
) {
    val isError = errorMessage != null
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = Typography.labelSmall,
            color = if (isError) MaterialTheme.colorScheme.error else TextDarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                width = 1.dp,
                color = if (isError) MaterialTheme.colorScheme.error else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardBackgroundGray,
                unfocusedContainerColor = CardBackgroundGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp), singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onVisibilityChange) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = TextLightGray
                    )
                }
            }
        )
        if (isError) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun OtpInputField(otpText: String, onOtpTextChange: (String) -> Unit, otpCount: Int = 6) {
    BasicTextField(
        value = otpText,
        onValueChange = { if (it.length <= otpCount) onOtpTextChange(it) },
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

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    AgritechTheme { ForgotPasswordContent(SecurityUiState(), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
fun VerifyEmailScreenPreview() {
    AgritechTheme { VerifyEmailContent("user@gmail.com", SecurityUiState(), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordScreenPreview() {
    AgritechTheme { ResetPasswordContent(SecurityUiState(), {}, {}, {}, {}, {}, {}) }
}