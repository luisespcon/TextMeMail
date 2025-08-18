package auth

import com.google.firebase.auth.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
    val name: String,
    val language: String, // "es" | "en"
    val email: String
)

class EmailAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Crea usuario en Auth, envía verificación y guarda perfil en Firestore */
    fun register(
        name: String,
        email: String,
        password: String,
        language: String,
        done: (ok: Boolean, message: String) -> Unit
    ) {
        if (!email.isValidEmail() || password.length < 6 || name.isBlank()) {
            done(false, "Datos inválidos. Revisa nombre, correo y contraseña (≥6).")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    val msg = when (val e = t.exception) {
                        is FirebaseAuthUserCollisionException -> "El correo ya está registrado."
                        is FirebaseAuthInvalidCredentialsException -> "Correo inválido."
                        else -> e?.localizedMessage ?: "No se pudo crear la cuenta."
                    }
                    done(false, msg)
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    done(false, "Usuario no disponible tras registro.")
                    return@addOnCompleteListener
                }

                // Enviar verificación
                user.sendEmailVerification()

                // Guardar perfil en Firestore
                val doc = mapOf(
                    "name" to name,
                    "language" to language,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(user.uid)
                    .set(doc)
                    .addOnSuccessListener {
                        done(true, "Cuenta creada. Revisa tu correo para verificar.")
                    }
                    .addOnFailureListener { e ->
                        done(false, "Cuenta creada, pero fallo al crear perfil: ${e.localizedMessage}")
                    }
            }
    }

    /** Login simple por email/password */
    fun login(
        email: String,
        password: String,
        done: (ok: Boolean, message: String) -> Unit
    ) {
        if (!email.isValidEmail() || password.isBlank()) {
            done(false, "Revisa correo y contraseña.")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    done(true, "Inicio de sesión OK.")
                } else {
                    val msg = when (val e = t.exception) {
                        is FirebaseAuthInvalidUserException -> "Usuario no existe."
                        is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta."
                        else -> e?.localizedMessage ?: "No se pudo iniciar sesión."
                    }
                    done(false, msg)
                }
            }
    }

    /** Reenviar verificación de email */
    fun resendVerification(done: (ok: Boolean, message: String) -> Unit) {
        val user = auth.currentUser
        if (user == null) { done(false, "Sin usuario activo."); return }
        user.sendEmailVerification()
            .addOnSuccessListener { done(true, "Correo de verificación reenviado.") }
            .addOnFailureListener { e -> done(false, e.localizedMessage ?: "No se pudo reenviar.") }
    }

    /** Refresca el usuario y dice si ya está verificado */
    fun reloadAndIsVerified(done: (verified: Boolean) -> Unit) {
        val user = auth.currentUser ?: return done(false)
        user.reload().addOnCompleteListener {
            done(auth.currentUser?.isEmailVerified == true)
        }
    }

    /** Cambiar idioma en Firestore */
    fun updateLanguage(newLanguage: String, done: (ok: Boolean, message: String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return done(false, "Sin usuario.")
        db.collection("users").document(uid)
            .update(mapOf("language" to newLanguage, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { done(true, "Idioma actualizado.") }
            .addOnFailureListener { e -> done(false, e.localizedMessage ?: "No se pudo actualizar idioma.") }
    }

    /** Cambiar email en Auth (puede pedir re-auth en algunos casos) */
    fun updateEmail(newEmail: String, done: (ok: Boolean, message: String) -> Unit) {
        val user = auth.currentUser ?: return done(false, "Sin usuario.")
        if (!newEmail.isValidEmail()) { done(false, "Correo inválido."); return }
        user.updateEmail(newEmail)
            .addOnSuccessListener {
                // actualizar Firestore también
                db.collection("users").document(user.uid)
                    .update(mapOf("email" to newEmail, "updatedAt" to FieldValue.serverTimestamp()))
                done(true, "Correo actualizado.")
            }
            .addOnFailureListener { e ->
                done(false, e.localizedMessage ?: "No se pudo actualizar correo (puede requerir reautenticación).")
            }
    }

    /** Enviar email para restablecer contraseña */
    fun sendPasswordReset(email: String, done: (ok: Boolean, message: String) -> Unit) {
        if (!email.isValidEmail()) { done(false, "Correo inválido."); return }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { done(true, "Email de restablecimiento enviado.") }
            .addOnFailureListener { e -> done(false, e.localizedMessage ?: "No se pudo enviar reset.") }
    }

    fun signOut() = auth.signOut()
}

private fun String.isValidEmail(): Boolean =
    isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()