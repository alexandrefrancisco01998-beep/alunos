package com.imobiliario.aluno.ui.perfil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imobiliario.aluno.ui.codigo.CodigoAlunoViewModel
import com.imobiliario.aluno.ui.theme.Spacing

/**
 * Bottom sheet para adicionar (ou trocar para) outro aluno, aberta a
 * partir do botão "+" da bottom bar, por cima da Home — sem navegar para
 * uma tela cheia separada. Segue o mesmo padrão visual de telas de
 * criação "profissionais" (ex.: Google Chat "Criar um espaço"): ícone
 * grande, rótulo em linha acima do campo, contador de caracteres e ação
 * principal em destaque.
 *
 * Ao confirmar o código com sucesso, [onAlunoAtivado] é chamado — a Home
 * já observa o perfil ativo via Flow, então os dados trocam
 * instantaneamente assim que a sheet fecha, sem passo extra de
 * confirmação manual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdicionarAlunoSheet(
    onDismiss: () -> Unit,
    onAlunoAtivado: () -> Unit,
    viewModel: CodigoAlunoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Assim que a consulta tem sucesso, o aluno já foi marcado como ativo
    // no banco (ver CodigoAlunoViewModel.consultar) — a Home reage sozinha
    // via Flow, então aqui só fechamos a sheet.
    LaunchedEffect(uiState.consultaSucesso) {
        uiState.consultaSucesso?.let {
            viewModel.consumirEventoConsulta()
            onAlunoAtivado()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg + 4.dp)
                .padding(bottom = Spacing.lg)
        ) {
            // Cabeçalho: título à esquerda, ação principal em destaque à
            // direita — mesma composição da referência ("Criar um
            // espaço" + botão "Criar" no canto).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adicionar aluno",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                val buttonWidth by animateDpAsState(
                    targetValue = if (uiState.carregando) 120.dp else 96.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "confirmarButtonWidth"
                )
                Button(
                    onClick = viewModel::consultar,
                    enabled = uiState.botaoHabilitado,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.width(buttonWidth)
                ) {
                    AnimatedVisibility(visible = uiState.carregando) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(if (uiState.carregando) "Adicionando" else "Adicionar")
                }
            }

            Spacer(Modifier.height(Spacing.lg + 4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(Spacing.md))

                // Campo com rótulo apoiado na própria borda superior,
                // igual ao "Nome do espaço" da referência.
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.codigo,
                        onValueChange = viewModel::onCodigoChange,
                        label = { Text("Código do aluno") },
                        placeholder = { Text("XXX-XXX") },
                        singleLine = true,
                        isError = uiState.erro != null,
                        enabled = !uiState.carregando,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${uiState.codigoLimpo.length}/6",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, end = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = "Digite o código fornecido pela escola para vincular mais um aluno à sua conta. Os alunos já adicionados continuam salvos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 56.dp + Spacing.md)
            )

            AnimatedVisibility(
                visible = uiState.erro != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm, start = 56.dp + Spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = uiState.erro ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (uiState.erroTemporario) {
                        Text(
                            text = "Tentar novamente",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 6.dp, start = 22.dp)
                                .clickable { viewModel.consultar() }
                        )
                    }
                }
            }
        }
    }
}
