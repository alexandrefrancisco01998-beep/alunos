package com.imobiliario.aluno.ui.detalhes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imobiliario.aluno.ui.components.MeuFilhoTopBar
import com.imobiliario.aluno.ui.theme.CapulanaGreen
import com.imobiliario.aluno.ui.theme.CapulanaRed
import com.imobiliario.aluno.ui.theme.Spacing
import kotlin.math.roundToInt

data class LinhaTrimestre(
    val trimestre: String,
    val acs1: String,
    val acs2: String,
    val acs3: String,
    val macs: String,
    val at: String,
    val mt: String
)

/** Larguras fixas de coluna — permitem scroll horizontal sem espremer 7 colunas em telas pequenas. */
private val LarguraColunaTrimestre = 56.dp
private val LarguraColunaDado = 64.dp
private val TitulosColunas = listOf("Trim.", "1ºACS", "2ºACS", "3ºACS", "MACS", "AT", "MT")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesNotaScreen(
    alunoNome: String,
    alunoNumero: Int,
    turmaNome: String,
    disciplinaNome: String,
    professor: String,
    notas: Map<String, String>,
    onVoltar: () -> Unit
) {
    val linhas = remember(notas) {
        listOf(
            LinhaTrimestre(
                "1º",
                notas["campo_0"] ?: "-", notas["campo_1"] ?: "-", notas["campo_2"] ?: "-",
                notas["campo_3"] ?: "-", notas["campo_4"] ?: "-", notas["campo_5"] ?: "-"
            ),
            LinhaTrimestre(
                "2º",
                notas["campo_6"] ?: "-", notas["campo_7"] ?: "-", notas["campo_8"] ?: "-",
                notas["campo_9"] ?: "-", notas["campo_10"] ?: "-", notas["campo_11"] ?: "-"
            ),
            LinhaTrimestre(
                "3º",
                notas["campo_12"] ?: "-", notas["campo_13"] ?: "-", notas["campo_14"] ?: "-",
                notas["campo_15"] ?: "-", notas["campo_16"] ?: "-", notas["campo_17"] ?: "-"
            )
        )
    }

    val mediaFinal = remember(notas) {
        val mt1 = notas["campo_5"]?.replace(",", ".")?.toFloatOrNull()
        val mt2 = notas["campo_11"]?.replace(",", ".")?.toFloatOrNull()
        val mt3 = notas["campo_17"]?.replace(",", ".")?.toFloatOrNull()
        val validas = listOfNotNull(mt1, mt2, mt3)
        if (validas.isEmpty()) null else (validas.sum() / validas.size).roundToInt()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MeuFilhoTopBar(
                title = disciplinaNome,
                onNavigateBack = onVoltar,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Column {
                    Text(
                        disciplinaNome,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Prof. $professor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$alunoNome (Nº $alunoNumero) · $turmaNome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                TabelaNotas(linhas = linhas)
            }

            item {
                MediaFinalCard(mediaFinal = mediaFinal)
            }
        }
    }
}

@Composable
private fun TabelaNotas(linhas: List<LinhaTrimestre>) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            TabelaHeader()
            linhas.forEachIndexed { index, linha ->
                TabelaLinha(linha = linha, zebra = index % 2 == 1)
            }
        }
    }
}

@Composable
private fun TabelaHeader() {
    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(vertical = Spacing.sm)
        ) {
            TitulosColunas.forEachIndexed { index, titulo ->
                ColunaCelula(index = index, alturaDivisor = 20.dp) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.5.dp
        )
    }
}

@Composable
private fun TabelaLinha(linha: LinhaTrimestre, zebra: Boolean) {
    val corFundo = if (zebra) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val valores = listOf(linha.trimestre, linha.acs1, linha.acs2, linha.acs3, linha.macs, linha.at, linha.mt)

    Column {
        Row(
            modifier = Modifier
                .background(corFundo)
                .padding(vertical = Spacing.sm + 2.dp)
        ) {
            valores.forEachIndexed { index, valor ->
                ColunaCelula(index = index, alturaDivisor = 24.dp) {
                    if (index == 6) {
                        NotaChip(valor)
                    } else {
                        Text(
                            valor,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = if (index == 0 || index == 4) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 1.dp
        )
    }
}

/**
 * Uma célula de largura fixa (não `weight`) — necessário para o scroll
 * horizontal funcionar sem esticar colunas em telas pequenas. Uma borda
 * vertical fininha simula a grade real de uma planilha (Google Sheets).
 */
@Composable
private fun ColunaCelula(
    index: Int,
    alturaDivisor: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val largura = if (index == 0) LarguraColunaTrimestre else LarguraColunaDado
    Box(
        modifier = Modifier.width(largura),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
    if (index != TitulosColunas.lastIndex) {
        VerticalDivider(
            modifier = Modifier.height(alturaDivisor),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

/** Chip arredondado para a coluna MT, com cor de aprovação/reprovação — como os apps Google fazem. */
@Composable
private fun NotaChip(valor: String) {
    val notaFloat = valor.replace(",", ".").toFloatOrNull()
    val cor = when {
        notaFloat == null -> MaterialTheme.colorScheme.onSurfaceVariant
        notaFloat < 10f -> CapulanaRed
        else -> CapulanaGreen
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = cor.copy(alpha = 0.12f)
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = cor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
        )
    }
}

@Composable
private fun MediaFinalCard(mediaFinal: Int?) {
    val cor = when {
        mediaFinal == null -> MaterialTheme.colorScheme.onSurfaceVariant
        mediaFinal < 10 -> CapulanaRed
        else -> CapulanaGreen
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Média final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cor.copy(alpha = 0.15f)
            ) {
                Text(
                    mediaFinal?.toString() ?: "-",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = cor,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                )
            }
        }
    }
}
