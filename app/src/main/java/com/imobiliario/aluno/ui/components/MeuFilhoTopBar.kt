package com.imobiliario.aluno.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TopAppBar única e reutilizável para todas as telas do app.
 *
 * Unifica altura, tipografia (titleLarge, SemiBold) e comportamento de
 * elevação ao rolar (o container muda de cor sutilmente quando o
 * conteúdo passa por baixo dele, como no Gmail) entre Perfil, Detalhes,
 * Notificações e Código do Aluno.
 *
 * @param title Título da tela.
 * @param onNavigateBack Se fornecido, mostra o ícone de voltar padrão.
 *   Passe `null` (o padrão) quando a tela não tiver navegação de volta,
 *   como Perfil.
 * @param navigationIcon Slot alternativo para um ícone de navegação
 *   customizado, ignorado se [onNavigateBack] for fornecido.
 * @param actions Ações à direita da barra (botões, badges etc).
 * @param scrollBehavior Comportamento de rolagem — passe o mesmo
 *   [TopAppBarScrollBehavior] usado no `Modifier.nestedScroll` do
 *   Scaffold para que a elevação reaja ao conteúdo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeuFilhoTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    centered: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    // Slot opcional para substituir o título por algo customizado (ex: campo
    // de pesquisa no Perfil). Quando null, usa o [title] em texto normal —
    // todas as outras telas continuam funcionando sem alteração.
    titleContent: (@Composable () -> Unit)? = null
) {
    val resolvedNavigationIcon: @Composable () -> Unit = if (onNavigateBack != null) {
        {
            MeuFilhoTopBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                onClick = onNavigateBack
            )
        }
    } else if (navigationIcon != null) {
        navigationIcon
    } else {
        {}
    }

    val resolvedTitleContent: @Composable () -> Unit = titleContent ?: {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }

    if (centered) {
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = resolvedTitleContent,
            navigationIcon = resolvedNavigationIcon,
            actions = { actions() },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = resolvedTitleContent,
            navigationIcon = resolvedNavigationIcon,
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Botão de ícone padrão da top bar: 24dp, com fundo tonal circular sutil
 * no touch target, seguindo o padrão do Material You (Gmail, Classroom).
 */
@Composable
fun MeuFilhoTopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}
