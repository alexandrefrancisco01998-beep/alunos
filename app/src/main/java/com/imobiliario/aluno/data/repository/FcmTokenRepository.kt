package com.imobiliario.aluno.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.imobiliario.aluno.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Registra o token FCM no backend.
 *
 * A Cloud Function registrarFcmToken grava o token em:
 *
 * Firestore
 * users/{uid}/fcmToken
 *
 * É chamado:
 *  - logo após o login;
 *  - sempre que o Firebase gerar um novo token.
 */
class FcmTokenRepository(
    private val functions: FirebaseFunctions =
        Firebase.functions("us-central1")
) {

    companion object {
        private const val TAG = "FcmTokenRepository"
    }

    suspend fun registrarTokenAtual() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            registrar(token)
        } catch (e: Exception) {
            logAvisoDebug(
                "Falha ao obter/registar token FCM: ${e.message}"
            )
        }
    }

    suspend fun registrar(token: String) {
        try {
            functions
                .getHttpsCallable("registrarFcmToken")
                .call(
                    hashMapOf(
                        "fcmToken" to token
                    )
                )
                .await()

            logAvisoDebug("Token FCM registado com sucesso.")

        } catch (e: Exception) {
            logAvisoDebug(
                "Falha ao registar token FCM: ${e.message}"
            )
        }
    }

    private fun logAvisoDebug(mensagem: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, mensagem)
        }
    }
}