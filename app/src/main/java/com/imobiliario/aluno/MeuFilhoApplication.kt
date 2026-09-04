package com.imobiliario.aluno

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class MeuFilhoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Ativa a persistência offline do Firestore aqui, uma única vez,
        // antes de qualquer tela ou repositório usar o Firestore. Feito no
        // onCreate do Application, nunca corre risco de "instância já em
        // uso" — o app inteiro passa a ler/escrever notificações (e
        // qualquer outro dado do Firestore) mesmo sem internet.
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder(firestoreSettings)
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
    }
}
