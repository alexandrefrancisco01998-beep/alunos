package com.imobiliario.aluno

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class MeuFilhoApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pautaa_notificacoes"
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // Cria o canal ANTES de qualquer mensagem FCM chegar.
        criarCanalNotificacoes()

        // Persistência offline do Firestore.
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder(firestoreSettings)
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder().build()
                )
                .build()
        }

        // Persistência offline do Realtime Database.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    private fun criarCanalNotificacoes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val canal = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Notas Atualizadas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de novas notas lançadas pelos professores"
        }

        manager.createNotificationChannel(canal)
    }
}