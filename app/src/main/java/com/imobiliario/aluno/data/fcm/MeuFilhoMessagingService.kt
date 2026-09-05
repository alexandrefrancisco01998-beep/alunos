package com.imobiliario.aluno.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.imobiliario.aluno.BuildConfig
import com.imobiliario.aluno.MainActivity
import com.imobiliario.aluno.R
import com.imobiliario.aluno.data.repository.FcmTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recebe pushes de notas atualizadas enviados pela Cloud Function
 * `lancarNotasIndividuais` (ver enviarPush.ts) e mantém o token FCM do
 * usuário logado sincronizado no RTDB via `registrarFcmToken`.
 *
 * O `data` payload esperado contém:
 *   tipo, turmaNome, codigoTurma, codigoAluno, disciplina
 *
 * Este serviço só exibe a notificação do sistema (barra de notificações
 * do Android). Não existe mais gravação local em Room: a lista de
 * notificações mostrada dentro do app vem sempre do Firestore (coleção
 * `notificacoesAluno`, já criada pela própria Cloud Function que envia o
 * push), então gravar aqui de novo só duplicava a informação sem nunca
 * ser lido por ninguém.
 */
class MeuFilhoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MeuFilhoFcm"
        private const val CHANNEL_ID = "pautaa_notificacoes" // mesmo ID usado em enviarPush.ts
    }

    // Service não é ViewModel — não tem viewModelScope. Usa um escopo
    // próprio com SupervisorJob, cancelado explicitamente em onDestroy
    // para não vazar coroutines caso o sistema encerre o Service antes
    // de todas terminarem.
    private val job = SupervisorJob()
    private val escopo = CoroutineScope(job + Dispatchers.IO)
    private val tokenRepository = FcmTokenRepository()

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registrarTokenNoBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val dados = message.data
        val codigoAluno = dados["codigoAluno"]
        if (codigoAluno.isNullOrBlank()) {
            logAvisoDebug("Push recebido sem codigoAluno no payload — ignorado")
            return
        }

        val titulo = message.notification?.title ?: "Notas atualizadas"
        val corpo = message.notification?.body ?: "Confira as notas atualizadas no app."

        // A notificação já foi gravada no Firestore pela própria Cloud
        // Function antes de enviar o push (coleção `notificacoesAluno`).
        // O listener em NotificacaoRepository.observarPorAluno pega essa
        // gravação em tempo real assim que o app abre — não é preciso
        // (nem desejável) gravar nada localmente aqui.
        exibirNotificacaoSistema(titulo, corpo, codigoAluno)
    }

    private fun registrarTokenNoBackend(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            logAvisoDebug("Token FCM recebido antes do login — será registado após entrarComGoogle")
            return
        }
        escopo.launch { tokenRepository.registrar(token) }
    }

    private fun exibirNotificacaoSistema(titulo: String, corpo: String, codigoAluno: String) {
        criarCanalSeNecessario()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("codigoAluno", codigoAluno)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            codigoAluno.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable._000327606)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(System.currentTimeMillis().toInt(), notificacao)
    }

    private fun criarCanalSeNecessario() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val canal = NotificationChannel(
            CHANNEL_ID,
            "Notas Atualizadas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de novas notas lançadas pelos professores"
        }
        manager.createNotificationChannel(canal)
    }

    private fun logAvisoDebug(mensagem: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, mensagem)
    }
}


