package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.repository.AuthRepository
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isSignUpTab by remember { mutableStateOf(false) }
    var emailOrUsername by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Automatic silent Google account detection & login (mirroring YouTube on Android)
    LaunchedEffect(Unit) {
        try {
            val autoResult = authRepo.attemptAutoGoogleLogin(context)
            autoResult.onSuccess {
                onAuthSuccess()
            }
        } catch (_: Exception) {}
    }

    fun handleGoogleSignIn() {
        isGoogleLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepo.signInWithGoogle(context, autoSelectOnly = false)
            isGoogleLoading = false
            result.onSuccess {
                onAuthSuccess()
            }.onFailure { err ->
                errorMessage = err.message ?: "Kuingia na Google kumeshindwa. Tafadhali jaribu tena."
            }
        }
    }

    fun handleSignIn() {
        val identifier = emailOrUsername.trim()
        val pass = password.trim()
        if (identifier.isBlank()) {
            errorMessage = "Tafadhali ingiza barua pepe au jina la mtumiaji."
            return
        }
        if (pass.isBlank()) {
            errorMessage = "Tafadhali ingiza nenosiri lako."
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            val result = authRepo.signInWithEmail(identifier, pass)
            isLoading = false
            result.onSuccess {
                onAuthSuccess()
            }.onFailure { err ->
                errorMessage = err.message ?: "Kuingia kumeshindwa. Tafadhali jaribu tena."
            }
        }
    }

    fun handleSignUp() {
        val user = username.trim()
        val mail = email.trim()
        val phone = phoneNumber.trim()
        val pass = password.trim()

        if (user.isBlank()) {
            errorMessage = "Tafadhali ingiza jina lako."
            return
        }
        if (mail.isBlank() || !mail.contains("@")) {
            errorMessage = "Tafadhali ingiza barua pepe sahihi."
            return
        }
        if (pass.length < 6) {
            errorMessage = "Nenosiri lazima liwe na herufi angalau 6."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            val result = authRepo.signUpWithEmail(mail, pass, user, phoneNumber = phone)
            result.onSuccess {
                isLoading = false
                onAuthSuccess()
            }.onFailure { err ->
                isLoading = false
                errorMessage = err.message ?: "Usajili haukufanikiwa. Tafadhali jaribu tena."
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeliVoid,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NeliVoid)
                .statusBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Logo & Glow
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NeliBluePrimary.copy(alpha = 0.4f),
                                NeliCyanAccent.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(2.dp, NeliCyanAccent.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "NeliPlay Logo",
                    tint = NeliCyanAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Neliplay",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeliCyanAccent)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SWAHILI",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tanzania Only Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeliSurfaceElevated)
                    .border(1.dp, NeliSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "🇹🇿", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tanzania Pekee (+255)",
                    color = NeliCyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tazama na pakua filamu na tamthilia zote bora zilizotafsiriwa kwa Kiswahili.",
                color = NeliTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NeliSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeliSurfaceVariant, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Google One-Tap / Auto-Login Button (Like YouTube)
                    Button(
                        onClick = { handleGoogleSignIn() },
                        enabled = !isGoogleLoading && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_login_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F3F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "Ingia na Google (Bila Nenosiri)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1F1F1F)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "⚡ Inasoma akaunti ya Google ya simu yako kama ilivyo kwenye YouTube.",
                        color = NeliTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(NeliSurfaceVariant)
                        )
                        Text(
                            text = "  AU WEKA NENOSIRI  ",
                            color = NeliTextSecondary.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(NeliSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Segmented Control Tabs (Ingia vs Jisajili)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeliSurfaceElevated)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpTab) NeliBluePrimary else Color.Transparent)
                                .clickable {
                                    isSignUpTab = false
                                    errorMessage = null
                                }
                                .testTag("tab_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ingia (Login)",
                                color = if (!isSignUpTab) Color.White else NeliTextSecondary,
                                fontWeight = if (!isSignUpTab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpTab) NeliBluePrimary else Color.Transparent)
                                .clickable {
                                    isSignUpTab = true
                                    errorMessage = null
                                }
                                .testTag("tab_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Jisajili (Sign Up)",
                                color = if (isSignUpTab) Color.White else NeliTextSecondary,
                                fontWeight = if (isSignUpTab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4A1212))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (!isSignUpTab) {
                        // ----------------------------------------
                        // LOGIN FORM
                        // ----------------------------------------
                        Text(
                            text = "Barua Pepe au Jina la Mtumiaji",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emailOrUsername,
                            onValueChange = { emailOrUsername = it },
                            placeholder = { Text("mfano: juma@gmail.com au juma", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_login_identifier_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Nenosiri (Password)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Weka nenosiri lako", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = NeliTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_login_password_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { handleSignIn() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeliBluePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_login_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Ingia kwenye Akaunti", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // ----------------------------------------
                        // SIGN UP FORM
                        // ----------------------------------------
                        Text(
                            text = "Jina Kamili / Jina la Mtumiaji",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text("mfano: Ally Juma", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Barua Pepe (Email)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("mfano: ally@gmail.com", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Namba ya Simu (Tanzania)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = { Text("07XXXXXXXX au 06XXXXXXXX", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_phone_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nenosiri (Angalau herufi 6)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeliTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Tengeneza nenosiri imara", color = NeliTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = NeliCyanAccent, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = NeliTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeliSurfaceElevated,
                                unfocusedContainerColor = NeliSurfaceElevated,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_password_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { handleSignUp() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeliCyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_signup_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Tengeneza Akaunti Mpya", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Kwa kuingia au kujisajili unakubaliana na Sheria na Masharti ya NeliPlay App Tanzania.",
                fontSize = 11.sp,
                color = NeliTextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}
