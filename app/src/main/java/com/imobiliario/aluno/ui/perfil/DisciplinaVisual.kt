package com.imobiliario.aluno.ui.perfil

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ícone + par de cores (fundo pastel / cor do ícone) para o círculo de
 * cada disciplina na tela de Início, no mesmo espírito visual da
 * referência (frasco vermelho em círculo rosa para Química, livro azul
 * para História, régua verde para Matemática, folha verde para Biologia).
 */
data class DisciplinaVisual(
    val icone: ImageVector,
    val corFundo: Color,
    val corIcone: Color
)

/** Paleta de fallback para disciplinas sem correspondência conhecida, escolhida de forma determinística pelo nome — assim a mesma disciplina sempre cai na mesma cor entre sessões. */
private val paletaPadrao = listOf(
    Color(0xFFFCE4E4) to Color(0xFFC53030),
    Color(0xFFE1ECF7) to Color(0xFF2F6FB0),
    Color(0xFFE3F3E8) to Color(0xFF2F8858),
    Color(0xFFFCEFD8) to Color(0xFFB5720C),
    Color(0xFFEDE3F7) to Color(0xFF6E3FA3),
    Color(0xFFDDF3F1) to Color(0xFF1A8A7E),
)

/**
 * Remove acentos e normaliza para minúsculas, para casar "Química",
 * "quimica", "QUÍMICA" etc. com a mesma chave de busca.
 */
private fun String.normalizado(): String {
    val semAcento = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    return semAcento.lowercase()
}

fun visualParaDisciplina(nomeDisciplina: String): DisciplinaVisual {
    val nome = nomeDisciplina.normalizado()

    val conhecida: Pair<ImageVector, Pair<Color, Color>>? = when {
        "quimica" in nome ->
            Icons.Filled.Science to (Color(0xFFFCE4E4) to Color(0xFFD32F2F))
        "biolog" in nome ->
            Icons.Filled.Eco to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        "fisica" in nome ->
            Icons.Filled.Biotech to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "historia" in nome ->
            Icons.AutoMirrored.Filled.MenuBook to (Color(0xFFE1ECF7) to Color(0xFF2F6FB0))
        "matemat" in nome ->
            Icons.Filled.Straighten to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        "geografia" in nome ->
            Icons.Filled.Public to (Color(0xFFDDF3F1) to Color(0xFF1A8A7E))
        "portugu" in nome || "literatura" in nome ->
            Icons.Filled.MenuBook to (Color(0xFFFCE4E4) to Color(0xFFC53030))
        "ingles" in nome || "frances" in nome || "idioma" in nome || "lingua" in nome ->
            Icons.Filled.Language to (Color(0xFFE1ECF7) to Color(0xFF2F6FB0))
        "informat" in nome || "computa" in nome ->
            Icons.Filled.Computer to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "educacao fisica" in nome || "desporto" in nome || "educ. fisica" in nome ->
            Icons.Filled.SportsSoccer to (Color(0xFFFCEFD8) to Color(0xFFB5720C))
        "arte" in nome || "desenho" in nome ->
            Icons.Filled.Palette to (Color(0xFFFCEFD8) to Color(0xFFB5720C))
        "filosofia" in nome || "psicolog" in nome ->
            Icons.Filled.Psychology to (Color(0xFFEDE3F7) to Color(0xFF6E3FA3))
        "calculo" in nome ->
            Icons.Filled.Calculate to (Color(0xFFE3F3E8) to Color(0xFF2F8858))
        else -> null
    }

    if (conhecida != null) {
        val (icone, cores) = conhecida
        return DisciplinaVisual(icone, cores.first, cores.second)
    }

    val indice = (nome.hashCode().let { if (it < 0) -it else it }) % paletaPadrao.size
    val (fundo, icone) = paletaPadrao[indice]
    return DisciplinaVisual(Icons.Filled.MenuBook, fundo, icone)
}
