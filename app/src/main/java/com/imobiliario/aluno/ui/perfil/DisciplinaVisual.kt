package com.imobiliario.aluno.ui.perfil

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer

data class DisciplinaVisual(
    val icone: ImageVector,
    val corFundo: Color,
    val corIcone: Color
)

private val paletaPadrao = listOf(
    Color(0xFFFCE4E4) to Color(0xFFC53030),
    Color(0xFFE1ECF7) to Color(0xFF2F6FB0),
    Color(0xFFE3F3E8) to Color(0xFF2F8858),
    Color(0xFFFCEFD8) to Color(0xFFB5720C),
    Color(0xFFEDE3F7) to Color(0xFF6E3FA3),
    Color(0xFFDDF3F1) to Color(0xFF1A8A7E),
)

// Regex pré-compilado para evitar recompilação em tempo de execução
private val REGEX_ACENTOS = Regex("\\p{InCombiningDiacriticalMarks}+")

private fun String.normalizado(): String {
    val semAcento = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_ACENTOS.replace(semAcento, "").lowercase()
}

fun visualParaDisciplina(nomeDisciplina: String): DisciplinaVisual {
    val nome = nomeDisciplina.normalizado()

    // Padronizando o ícone de livro para a versão AutoMirrored
    val iconeLivro = Icons.AutoMirrored.Filled.MenuBook

    val (icone, cores) = when {
        "quimica" in nome -> Icons.Filled.Science to (Color(0xFFFCE4E4) to Color(0xFFD32F2F))
        "biolog" in nome -> Icons.Filled.Eco to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        "fisica" in nome -> Icons.Filled.Biotech to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "historia" in nome -> iconeLivro to (Color(0xFFE1ECF7) to Color(0xFF2F6FB0))
        "matemat" in nome -> Icons.Filled.Straighten to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        "geografia" in nome -> Icons.Filled.Public to (Color(0xFFDDF3F1) to Color(0xFF1A8A7E))
        "portugu" in nome || "literatura" in nome -> iconeLivro to (Color(0xFFFCE4E4) to Color(0xFFC53030))
        "ingles" in nome || "frances" in nome || "idioma" in nome || "lingua" in nome -> Icons.Filled.Language to (Color(0xFFE1ECF7) to Color(0xFF2F6FB0))
        "informat" in nome || "computa" in nome -> Icons.Filled.Computer to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "educacao fisica" in nome || "desporto" in nome || "educ" in nome -> Icons.Filled.SportsSoccer to (Color(0xFFFCEFD8) to Color(0xFFB5720C))
        "arte" in nome || "desenho" in nome -> Icons.Filled.Palette to (Color(0xFFFCEFD8) to Color(0xFFB5720C))
        "filosofia" in nome || "psicolog" in nome -> Icons.Filled.Psychology to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "calculo" in nome -> Icons.Filled.Calculate to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        else -> {
            // Corrige o bug de estouro no Int.MIN_VALUE usando Math.floorMod ou .mod() nativo do Kotlin
            val indice = nome.hashCode().mod(paletaPadrao.size)
            val (fundo, corIcone) = paletaPadrao[indice]
            return DisciplinaVisual(iconeLivro, fundo, corIcone)
        }
    }

    return DisciplinaVisual(icone, cores.first, cores.second)
}