package com.imobiliario.aluno.ui.notificacoes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imobiliario.aluno.ui.components.MeuFilhoTopBar
import com.imobiliario.aluno.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacoesScreen(
    codigoAluno: String,
    onVoltar: () -> Unit,
    viewModel: NotificacoesViewModel = viewModel()
) {
    LaunchedEffect(codigoAluno) {
        viewModel.iniciar(codigoAluno)
    }

    val notificacoes by viewModel.notificacoes.collectAsState()
    val carregando by viewModel.carregando.collectAsState()
    var menuAberto by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MeuFilhoTopBar(
                title = "Notificações",
                onNavigateBack = onVoltar,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = { menuAberto = true },
                        enabled = notificacoes.isNotEmpty(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = "Mais opções")
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text("Marcar todas como lidas") },
                            onClick = {
                                menuAberto = false
                                viewModel.marcarTodasComoLidas()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
                notificacoes.isEmpty() -> EmptyNotificacoes()
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        vertical = Spacing.sm,
                        horizontal = Spacing.screenHorizontal - 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(notificacoes, key = { it.id }) { notificacao ->
                        NotificacaoItem(
                            notificacao = notificacao,
                            onClick = { viewModel.toggleLida(notificacao) },
                            onDeletar = { viewModel.deletar(notificacao) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun EmptyNotificacoes() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(Spacing.sm + 4.dp))
        Text(
            "Nenhuma notificação por aqui",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Você será avisado quando uma nova nota for lançada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificacaoItem(
    notificacao: NotificacaoNota,
    onClick: () -> Unit,
    onDeletar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeletar()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.lg - 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Deletar",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                // Ícone quadrado arredondado, mesma cor para todas as
                // disciplinas (conforme definido) — varia só por tipo de
                // alteração (lançada / atualizada / removida).
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconePorTipo(notificacao.tipo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(Spacing.sm + 4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Linha 1: "Professor(a) Nome lançou/atualizou/removeu
                    // nota de Disciplina" — nome do professor e disciplina
                    // em destaque, texto de ação em peso normal.
                    Text(
                        text = textoNotificacao(notificacao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(if (notificacao.lida) 0.6f else 1f)
                    )
                    Spacer(Modifier.height(2.dp))
                    // Linha 2: turma, no mesmo estilo secundário da referência.
                    Text(
                        text = "Turma ${notificacao.turmaNome}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(Spacing.sm))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatarDataHora(notificacao.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.xs + 2.dp))
                    if (!notificacao.lida) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Texto principal do card, no formato da referência: nome do professor
 * (ou "Um professor", se não vier preenchido) + verbo de ação + disciplina
 * em destaque. Ex: "Professor Alexandre lançou nota de Química".
 */
@Composable
private fun textoNotificacao(notificacao: NotificacaoNota) = buildAnnotatedString {
    val quem = notificacao.professorNome.ifBlank { null }
    if (quem != null) {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(quem) }
        append(" ")
    } else {
        append("Um professor ")
    }
    append(verboAcao(notificacao.tipo))
    append(" de ")
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(notificacao.disciplina) }
}

private fun verboAcao(tipo: TipoNotificacao): String = when (tipo) {
    TipoNotificacao.NOTA_LANCADA -> "lançou nota"
    TipoNotificacao.NOTA_ALTERADA -> "atualizou a nota"
    TipoNotificacao.NOTA_DELETADA -> "removeu a nota"
}

private fun iconePorTipo(tipo: TipoNotificacao) = when (tipo) {
    TipoNotificacao.NOTA_LANCADA -> Icons.AutoMirrored.Filled.Assignment
    TipoNotificacao.NOTA_ALTERADA -> Icons.Filled.Edit
    TipoNotificacao.NOTA_DELETADA -> Icons.Filled.Delete
}

private fun formatarDataHora(timestamp: Long): String {
    val agora = System.currentTimeMillis()
    val diferenca = agora - timestamp

    return when {
        diferenca < 60_000 -> "agora"
        diferenca < 3_600_000 -> "${diferenca / 60_000}min"
        diferenca < 86_400_000 -> "${diferenca / 3_600_000}h"
        diferenca < 604_800_000 -> "${diferenca / 86_400_000}d"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
