package com.example.textmemail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import auth.EmailAuthManager
import com.example.textmemail.ui_auth.AuthEmailScreen
import com.example.textmemail.ui_auth.VerifyAndManageScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val emailAuth by lazy { EmailAuthManager(auth) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
            var isVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
            var currentEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
            var currentLanguage by remember { mutableStateOf("es") } // (puedes leerlo de Firestore tras login verificado)

            // Listener de auth
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { fa ->
                    val u = fa.currentUser
                    isLoggedIn = u != null
                    isVerified = u?.isEmailVerified == true
                    currentEmail = u?.email ?: ""
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    when {
                        !isLoggedIn -> {
                            // Pantalla de login/registro
                            AuthEmailScreen(
                                onRegister = { name, email, pass, lang, done ->
                                    emailAuth.register(name, email, pass, lang) { ok, msg ->
                                        if (ok) {
                                            currentLanguage = lang
                                        }
                                        done(ok, msg)
                                    }
                                },
                                onLogin = { email, pass, done ->
                                    emailAuth.login(email, pass) { ok, msg ->
                                        done(ok, msg)
                                    }
                                }
                            )
                        }
                        isLoggedIn && !isVerified -> {
                            // Registrado / logueado pero sin verificar: pantalla de verificación y gestión
                            VerifyAndManageScreen(
                                currentEmail = currentEmail,
                                currentLanguage = currentLanguage,
                                onResendVerification = { cb -> emailAuth.resendVerification(cb) },
                                onCheckVerified = { cb ->
                                    emailAuth.reloadAndIsVerified { verified ->
                                        isVerified = verified
                                        cb(verified)
                                    }
                                },
                                onUpdateLanguage = { newLang, cb ->
                                    emailAuth.updateLanguage(newLang) { ok, msg ->
                                        if (ok) currentLanguage = newLang
                                        cb(ok, msg)
                                    }
                                },
                                onUpdateEmail = { newEmail, cb ->
                                    emailAuth.updateEmail(newEmail) { ok, msg ->
                                        if (ok) currentEmail = newEmail
                                        cb(ok, msg)
                                    }
                                },
                                onPasswordReset = { email, cb -> emailAuth.sendPasswordReset(email, cb) },
                                onSignOut = { emailAuth.signOut() }
                            )
                        }
                        else -> {
                            // Verificado: Home
                            HomeScreen(
                                email = currentEmail,
                                onSignOut = { emailAuth.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(email: String, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sesión iniciada (verificado)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(if (email.isNotBlank()) email else "(sin correo)")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSignOut) { Text("Cerrar sesión") }
    }
}