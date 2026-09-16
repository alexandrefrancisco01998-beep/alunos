package com.imobiliario.aluno.data.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.imobiliario.aluno.BuildConfig
import com.imobiliario.aluno.MainActivity
import com.imobiliario.aluno.MeufilhoApplication
import com.imobiliario.aluno.R
import com.imobiliario.aluno.data.repository.FcmTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeuFilhoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MeuFilhoFcm"
        private const val CHANNEL_ID = "pautaa_notificacoes"
    }

    private val job = SupervisorJob()
    private val escopo = CoroutineScope(job + Dispatchers.IO)
    private val tokenRepository = FcmTokenRepository()

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "Novo token FCM recebido")

        registrarTokenNoBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(
            TAG,
            "Push FCM recebido. data=${message.data}, " +
                "titulo=${message.notification?.title}, " +
                "corpo=${message.notification?.body}"
        )

        val dados = message.data

        /*
         * O codigoAluno é útil para abrir o aluno correto,
         * mas NÃO pode impedir a exibição da notificação.
         */
        val codigoAluno = dados["codigoAluno"]

        val titulo = message.notification?.title
            ?.takeIf { it.isNotBlank() }
            ?: "Notas atualizadas"

        val corpo = message.notification?.body
            ?.takeIf { it.isNotBlank() }
            ?: "Confira as notas atualizadas no app."

        exibirNotificacaoSistema(
            titulo = titulo,
            corpo = corpo,
            codigoAluno = codigoAluno
        )
    }

    private fun registrarTokenNoBackend(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            logAvisoDebug(
                "Token FCM recebido antes do login. " +
                    "Será registado quando o usuário entrar."
            )
            return
        }

        escopo.launch {
            try {
                tokenRepository.registrar(token)

                Log.d(
                    TAG,
                    "Token FCM registado no backend para o usuário."
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Erro ao registar token FCM",
                    e
                )
            }
        }
    }

    private fun exibirNotificacaoSistema(
        titulo: String,
        corpo: String,
        codigoAluno: String?
    ) {
        /*
         * Android 13+ exige permissão POST_NOTIFICATIONS.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permitido = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permitido) {
                Log.w(
                    TAG,
                    "Notificação não exibida: " +
                        "POST_NOTIFICATIONS não foi concedida."
                )
                return
            }
        }

        criarCanalSeNecessario()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            if (!codigoAluno.isNullOrBlank()) {
                putExtra("codigoAluno", codigoAluno)
            }
        }

        /*
         * Se não houver codigoAluno, ainda criamos uma PendingIntent
         * válida para abrir o aplicativo.
         */
        val requestCode = codigoAluno
            ?.hashCode()
            ?: System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable._000327606)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(corpo)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager =
            getSystemService(NotificationManager::class.java)

        if (manager == null) {
            Log.e(
                TAG,
                "NotificationManager não disponível."
            )
            return
        }

        val notificationId = System.currentTimeMillis().toInt()

        manager.notify(
            notificationId,
            notificacao
        )

        Log.d(
            TAG,
            "Notificação exibida. id=$notificationId titulo=$titulo"
        )
    }

    private fun criarCanalSeNecessario() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            getSystemService(NotificationManager::class.java)
                ?: return

        /*
         * O Application já cria o canal.
         * Esta verificação mantém o Service seguro caso seja
         * utilizado independentemente.
         */
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val canal = NotificationChannel(
            CHANNEL_ID,
            "Notas Atualizadas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Avisos de novas notas lançadas pelos professores"
        }

        manager.createNotificationChannel(canal)
    }

    private fun logAvisoDebug(mensagem: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, mensagem)
        }
    }
}