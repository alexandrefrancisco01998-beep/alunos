package com.imobiliario.aluno.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.imobiliario.aluno.ui.codigo.CodigoAlunoScreen
import com.imobiliario.aluno.ui.login.LoginScreen
import com.imobiliario.aluno.ui.navigation.Routes
import com.imobiliario.aluno.ui.perfil.PerfilScreen

/**
 * @param codigoAlunoDeeplink Código vindo de uma notificação push (FCM).
 *   Quando não-nulo e o usuário já está autenticado, o app pula login e
 *   código e abre direto o perfil do aluno correspondente.
 */
@Composable
fun MeuFilhoApp(codigoAlunoDeeplink: String? = null) {
    val navController = rememberNavController()

    // Deeplink do FCM: só navega se o usuário já estiver autenticado e
    // o código vier preenchido. Caso contrário, o fluxo normal (login →
    // código → perfil) resolve a situação por conta própria.
    LaunchedEffect(codigoAlunoDeeplink) {
        val codigo = codigoAlunoDeeplink?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val autenticado = FirebaseAuth.getInstance().currentUser != null
        if (autenticado) {
            navController.navigate(Routes.perfil(codigo)) {
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
                    navController.navigate(Routes.CODIGO_ALUNO) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CODIGO_ALUNO) {
            CodigoAlunoScreen(
                onCodigoConfirmado = { codigo ->
                    navController.navigate(Routes.perfil(codigo)) {
                        popUpTo(Routes.CODIGO_ALUNO) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PERFIL,
            arguments = listOf(navArgument("codigoAluno") { type = NavType.StringType })
        ) { backStackEntry ->
            val codigo = backStackEntry.arguments?.getString("codigoAluno") ?: ""
            PerfilScreen(
                codigoAluno = codigo,
                onNovaConsulta = {
                    navController.navigate(Routes.CODIGO_ALUNO) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSairDaConta = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
