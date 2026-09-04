package com.imobiliario.aluno.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val CODIGO_ALUNO = "codigo_aluno"
    const val PERFIL = "perfil/{codigoAluno}"

    fun perfil(codigoAluno: String) = "perfil/$codigoAluno"
}
