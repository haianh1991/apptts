package com.example.webreader.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate

@Composable
fun LoginScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val appStrings = LocalAppStrings.current
    var isSigningIn by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    isSigningIn = true
                    viewModel.signInWithGoogle(
                        idToken = idToken,
                        onSuccess = {
                            isSigningIn = false
                            Toast.makeText(context, appStrings.toastLoginSuccess, Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            isSigningIn = false
                            Toast.makeText(context, "${appStrings.toastLoginFailed}$error", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    isSigningIn = false
                    Toast.makeText(context, appStrings.toastNoGoogleToken, Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                isSigningIn = false
                val errorMsg = String.format(appStrings.toastLoginFailedCode, e.statusCode.toString())
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            isSigningIn = false
        }
    }

    // A beautiful background gradient for tech premium style
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F2027), // Deep dark black/teal
            Color(0xFF203A43), // Slate teal
            Color(0xFF2C5364)  // Teal blue
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Decorative blurred vector circles in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF6200EE).copy(alpha = 0.12f),
                radius = 400f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f)
            )
            drawCircle(
                color = Color(0xFF03DAC6).copy(alpha = 0.12f),
                radius = 500f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
            )
        }

        // Language Selector in TopEnd
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            val displayLang by viewModel.appDisplayLanguage.collectAsState()

            TextButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = "Select Language",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (displayLang) {
                        "en" -> "EN"
                        "zh" -> "ZH"
                        else -> "VI"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E1E1E))
            ) {
                DropdownMenuItem(
                    text = { Text("Tiếng Việt (VI)", color = Color.White) },
                    onClick = {
                        viewModel.setAppDisplayLanguage("vi")
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("English (EN)", color = Color.White) },
                    onClick = {
                        viewModel.setAppDisplayLanguage("en")
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("简体中文 (ZH)", color = Color.White) },
                    onClick = {
                        viewModel.setAppDisplayLanguage("zh")
                        expanded = false
                    }
                )
            }
        }

        // Main Login Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xE61E1E1E) // Semi-transparent dark
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Branded Logo/Icon (Tech-style representation in Compose)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF6200EE),
                                    Color(0xFF03DAC6),
                                    Color(0xFFBB86FC),
                                    Color(0xFF6200EE)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "W",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Slogan/Titles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = appStrings.appName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = appStrings.appDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )

                // App Features list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureRow(
                        title = appStrings.loginFeature1Title,
                        desc = appStrings.loginFeature1Desc
                    )
                    FeatureRow(
                        title = appStrings.loginFeature2Title,
                        desc = appStrings.loginFeature2Desc
                    )
                    FeatureRow(
                        title = appStrings.loginFeature3Title,
                        desc = appStrings.loginFeature3Desc
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSigningIn) {
                    CircularProgressIndicator(
                        color = Color(0xFF03DAC6),
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    // Modern Styled Google Sign-In Button
                    Button(
                        onClick = {
                            isSigningIn = true
                            val client = viewModel.authManager.getGoogleSignInClient()
                            signInLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = appStrings.loginGoogleButton,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }
                }

                Text(
                    text = appStrings.loginRequiredSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .background(Color(0xFF03DAC6), shape = RoundedCornerShape(3.dp))
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
