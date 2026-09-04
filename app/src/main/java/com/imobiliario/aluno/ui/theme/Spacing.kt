package com.imobiliario.aluno.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Constantes de espaçamento centralizadas, seguindo o grid de 8dp do
 * Material Design. Usar sempre estes valores em vez de `.dp` soltos
 * espalhados pelas telas, para manter o ritmo visual consistente em
 * todo o app (mesmo padrão que Gmail/Classroom/Sheets seguem
 * internamente com seus próprios design tokens).
 */
object Spacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp

    /** Padding horizontal de conteúdo — único valor usado em todas as telas do app. */
    val screenHorizontal = 16.dp
}
