package com.imobiliario.aluno

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.imobiliario.aluno.ui.MeuFilhoApp
import com.imobiliario.aluno.ui.theme.MeuFilhoTheme

class MainActivity : ComponentActivity() {

    // Estado mutável (não uma val local) porque o deeplink também pode
    // chegar depois do onCreate, via onNewIntent — quando a app já está
    // aberta em background e o usuário toca numa notificação nova. Sem
    // isso, esse toque não tinha efeito nenhum: a Activity já existia,
    // então o Android chama onNewIntent em vez de recriar a tela.
    private var codigoAlunoDeeplinkState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Código do aluno enviado via push (FCM) — pode vir null se o app
        // foi aberto normalmente, não por toque numa notificação.
        codigoAlunoDeeplinkState.value = intent?.getStringExtra("codigoAluno")

        setContent {
            MeuFilhoTheme {

                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* permissão concedida ou negada — FCM já registra o token separadamente */ }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }

                val codigoAlunoDeeplink by codigoAlunoDeeplinkState
                MeuFilhoApp(codigoAlunoDeeplink = codigoAlunoDeeplink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        codigoAlunoDeeplinkState.value = intent.getStringExtra("codigoAluno")
    }
}
