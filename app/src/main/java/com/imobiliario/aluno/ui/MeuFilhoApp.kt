package com.imobiliario.aluno.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.imobiliario.aluno.ui.codigo.CodigoAlunoScreen
import com.imobiliario.aluno.ui.login.LoginScreen
import com.imobiliario.aluno.ui.navigation.Routes
import com.imobiliario.aluno.ui.perfil.PerfilScreen

@Composable
fun MeuFilhoApp() {
    val navController = rememberNavController()

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
