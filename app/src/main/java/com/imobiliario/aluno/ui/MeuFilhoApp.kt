package com.imobiliario.aluno.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.imobiliario.aluno.ui.login.LoginScreen
import com.imobiliario.aluno.ui.navigation.Routes
import com.imobiliario.aluno.ui.perfil.PerfilScreen

/**
 * @param codigoAlunoDeeplink Código vindo de uma notificação push (FCM).
 *   Quando não-nulo e o usuário já está autenticado, é repassado direto
 *   para [PerfilScreen] assim que a Home abre — sem tela cheia
 *   intermediária de "código do aluno", que deixou de existir. A Home
 *   observa esse aluno via cache do Realtime Database, mesmo que ele
 *   ainda não seja o perfil ativo salvo localmente.
 */
@Composable
fun MeuFilhoApp(codigoAlunoDeeplink: String? = null) {
    val navController = rememberNavController()

    // Deeplink do FCM: só navega se o usuário já estiver autenticado e
    // o código vier preenchido. Caso contrário, o fluxo normal (login →
    // Home) resolve a situação por conta própria.
    LaunchedEffect(codigoAlunoDeeplink) {
        val codigo = codigoAlunoDeeplink?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val autenticado = FirebaseAuth.getInstance().currentUser != null
        if (autenticado) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 6 } }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onAutenticado = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            // codigoAluno só vem preenchido no primeiro frame quando a
            // Home é aberta por deeplink do FCM. No fluxo normal (login
            // direto), a própria PerfilScreen observa o perfil ativo do
            // cache e decide sozinha entre estado vazio ou conteúdo.
            PerfilScreen(
                codigoAluno = codigoAlunoDeeplink?.takeIf { it.isNotBlank() },
                onSairDaConta = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
