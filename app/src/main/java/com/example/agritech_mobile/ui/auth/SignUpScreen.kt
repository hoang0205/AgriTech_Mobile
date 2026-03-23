package com.example.agritech_mobile.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme


data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,

    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val backendError: String? = null
)

@Composable
fun SignUpScreen(
    onLoginClick: () -> Unit = {},
    onRegisterSucces: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SignUpUiState()) }

    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                onRegisterSucces()
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).error
                uiState = uiState.copy(backendError = errorMsg)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
    fun validateAndSignUp() {
        var isValid = true
        var passError: String? = null
        var confirmError: String? = null
        var beError: String? = null

        uiState = uiState.copy(backendError = null)

        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        if (!passwordRegex.matches(uiState.password)) {
            passError = "Mật khẩu phải từ 8 kí tự, bao gồm số và chữ"
            isValid = false
        }

        if (uiState.password != uiState.confirmPassword) {
            confirmError = "Mật khẩu không khớp"
            isValid = false
        }

        if (uiState.fullName.isBlank() || uiState.email.isBlank() || uiState.phone.isBlank()) {
            beError = "Vui lòng nhập đầy đủ thông tin"
            isValid = false
        }

        uiState = uiState.copy(
            passwordError = passError,
            confirmPasswordError = confirmError,
            backendError = beError
        )

        if (isValid) {
            viewModel.register(uiState.fullName, uiState.phone, uiState.email, uiState.password)
        }
    }

    SignUpContent(
        uiState = uiState.copy(isLoading = authState is AuthState.Loading),
        onFullNameChange = { uiState = uiState.copy(fullName = it) },
        onEmailChange = { uiState = uiState.copy(email = it) },
        onPhoneChange = { uiState = uiState.copy(phone = it) },
        onPasswordChange = {
            uiState = uiState.copy(password = it, passwordError = null)
        },
        onConfirmPasswordChange = {
            uiState = uiState.copy(confirmPassword = it, confirmPasswordError = null)
        },
        onSignUpClick = { validateAndSignUp() },
        onLoginClick = onLoginClick
    )
}

@Composable
fun SignUpContent(
    uiState: SignUpUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.farm_svgrepo_com),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AgriTech",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.LightGray)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text = stringResource(id = R.string.create_account),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.join_digital_arboretum),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                CustomInputField(
                    label = stringResource(id = R.string.full_name),
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    placeholder = stringResource(id = R.string.full_name_placeholder),
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomInputField(
                    label = stringResource(id = R.string.email_address),
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(id = R.string.email_placeholder),
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomInputField(
                    label = stringResource(id = R.string.phone_number),
                    value = uiState.phone,
                    onValueChange = onPhoneChange,
                    placeholder = stringResource(id = R.string.phone_placeholder),
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomInputField(
                    label = stringResource(id = R.string.password),
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(id = R.string.password_placeholder),
                    isPassword = true,
                    errorMessage = uiState.passwordError,
                    helperText = stringResource(R.string.password_des)
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomInputField(
                    label = stringResource(id = R.string.confirm_password),
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = stringResource(id = R.string.password_placeholder),
                    isPassword = true,
                    errorMessage = uiState.confirmPasswordError
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.backendError != null) {
                    Text(
                        text = uiState.backendError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = onSignUpClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.create_account),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val loginText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)) {
                        append(stringResource(id = R.string.already_have_account))
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                        append(stringResource(id = R.string.login_button))
                    }
                }
                Text(
                    text = loginText,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLoginClick
                    )
                )
            }
        }
    }
}


@Composable
fun CustomInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null
) {
    val isError = errorMessage != null

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = icon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(8.dp)
        )

        if (isError) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "1. Trạng thái trống")
@Composable
fun SignUpScreenPreview() {
    AgritechTheme {
        SignUpContent(
            uiState = SignUpUiState(),
            onFullNameChange = {}, onEmailChange = {}, onPhoneChange = {},
            onPasswordChange = {}, onConfirmPasswordChange = {},
            onSignUpClick = {}, onLoginClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "2. Trạng thái báo lỗi")
@Composable
fun SignUpScreenErrorPreview() {
    AgritechTheme {
        SignUpContent(
            uiState = SignUpUiState(
                password = "123",
                confirmPassword = "12",
                passwordError = "Mật khẩu phải từ 8 kí tự, bao gồm số và chữ",
                confirmPasswordError = "Mật khẩu không khớp",
                backendError = "Email nguyenvana@gmail.com đã tồn tại!"
            ),
            onFullNameChange = {}, onEmailChange = {}, onPhoneChange = {},
            onPasswordChange = {}, onConfirmPasswordChange = {},
            onSignUpClick = {}, onLoginClick = {}
        )
    }
}