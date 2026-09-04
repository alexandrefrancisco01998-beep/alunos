package com.imobiliario.aluno.ui.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.imobiliario.aluno.data.model.DisciplinaComNotas
import com.imobiliario.aluno.ui.components.MeuFilhoTopBar
import com.imobiliario.aluno.ui.components.MeuFilhoTopBarIconButton
import com.imobiliario.aluno.ui.notificacoes.NotificacaoNota
import com.imobiliario.aluno.ui.notificacoes.NotificacoesViewModel
import com.imobiliario.aluno.ui.notificacoes.TipoNotificacao
import com.imobiliario.aluno.ui.theme.CapulanaGreen
import com.imobiliario.aluno.ui.theme.CapulanaRed
import com.imobiliario.aluno.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Aba ativa — controla o que é exibido no corpo e no título da TopBar.
// Não existe navegação nova: é só um enum de estado local.
// ---------------------------------------------------------------------------
private enum class Aba { INICIO, NOTIFICACOES, DETALHES }

// Dados mínimos para mostrar os detalhes de uma disciplina sem navegação.
private data class DetalhesAtivos(
    val disciplina: DisciplinaComNotas,
    val alunoNome: String,
    val alunoNumero: Int,
    val turmaNome: String
)

// Larguras das colunas da tabela de notas
private val LarguraColunaTrimestre = 56.dp
private val LarguraColunaDado = 64.dp
private val TitulosColunas = listOf("Trim.", "1ºACS", "2ºACS", "3ºACS", "MACS", "AT", "MT")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    codigoAluno: String,
    onNovaConsulta: () -> Unit,
    onSairDaConta: () -> Unit,
    perfilViewModel: PerfilViewModel = viewModel(),
    notificacoesViewModel: NotificacoesViewModel = viewModel()
) {
    LaunchedEffect(codigoAluno) {
        perfilViewModel.carregar(codigoAluno)
        notificacoesViewModel.iniciar(codigoAluno)
    }

    val perfilAtivo by perfilViewModel.perfilAtivo.collectAsState()
    LaunchedEffect(perfilAtivo?.codigoAluno) {
        perfilAtivo?.codigoAluno?.let { codigo ->
            if (codigo != codigoAluno) notificacoesViewModel.iniciar(codigo)
        }
    }

    val perfisSalvos by perfilViewModel.perfisSalvos.collectAsState()
    var mostrarSeletorAluno by remember { mutableStateOf(false) }
    var mostrarAdicionarAluno by remember { mutableStateOf(false) }

    val uiState by perfilViewModel.uiState.collectAsState()
    val naoLidas by perfilViewModel.notificacoesNaoLidas.collectAsState()
    val notificacoes by notificacoesViewModel.notificacoes.collectAsState()
    val notifCarregando by notificacoesViewModel.carregando.collectAsState()

    var abaAtiva by remember { mutableStateOf(Aba.INICIO) }
    var detalhesAtivos by remember { mutableStateOf<DetalhesAtivos?>(null) }

    var mostrarDialogoSair by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeDrawer = rememberCoroutineScope()
    var mostrarMenuNotif by remember { mutableStateOf(false) }
    var termoPesquisa by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    BackHandler(enabled = drawerState.isOpen) {
        scopeDrawer.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && abaAtiva == Aba.DETALHES) {
        abaAtiva = Aba.INICIO
    }

    val onAbrirGerenciarAlunos: () -> Unit = {
        if (perfisSalvos.size > 1) mostrarSeletorAluno = true
        else mostrarAdicionarAluno = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = abaAtiva == Aba.INICIO,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(304.dp)) {
                val estadoAtual = uiState
                if (estadoAtual is PerfilUiState.Sucesso) {
                    MenuHamburguerCabecalho(
                        fotoUrl = perfilViewModel.fotoPerfilUrl,
                        alunoNome = estadoAtual.dados.alunoNome,
                        alunoNumero = estadoAtual.dados.alunoNumero,
                        turmaNome = estadoAtual.dados.turmaNome
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                }
                NavigationDrawerItem(
                    label = { Text("Adicionar ou trocar aluno") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scopeDrawer.launch { drawerState.close() }
                        onAbrirGerenciarAlunos()
                    },
                    modifier = Modifier.padding(horizontal = Spacing.sm)
                )
                NavigationDrawerItem(
                    label = { Text("Sair da conta") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scopeDrawer.launch { drawerState.close() }
                        mostrarDialogoSair = true
                    },
                    modifier = Modifier.padding(horizontal = Spacing.sm)
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                MeuFilhoTopBar(
                    title = "",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MeuFilhoTopBarIconButton(
                            icon = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            onClick = { scopeDrawer.launch { drawerState.open() } }
                        )
                    },
                    titleContent = {
                        PesquisarDisciplinasField(
                            valor = termoPesquisa,
                            onValorChange = { termoPesquisa = it }
                        )
                    },
                    actions = {
                        Box(modifier = Modifier.padding(end = Spacing.xs)) {
                            PerfilAvatarButton(
                                fotoUrl = perfilViewModel.fotoPerfilUrl,
                                onClick = {}
                            )
                        }
                    }
                )
            },
            bottomBar = {
                InicioBottomBar(
                    abaAtiva = abaAtiva,
                    naoLidas = naoLidas,
                    onIrInicio = { abaAtiva = Aba.INICIO },
                    onIrNotificacoes = { abaAtiva = Aba.NOTIFICACOES },
                    onNovaConsulta = onAbrirGerenciarAlunos
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = abaAtiva,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "abaContent"
                ) { aba ->
                    when (aba) {
                        Aba.INICIO -> {
                            AnimatedContent(
                                targetState = uiState,
                                transitionSpec = {
                                    if (initialState::class != targetState::class) {
                                        fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                                    } else {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    }
                                },
                                label = "perfilState"
                            ) { state ->
                                when (state) {
                                    is PerfilUiState.Carregando -> LoadingState()
                                    is PerfilUiState.Erro -> ErroState(state.mensagem)
                                    is PerfilUiState.Sucesso -> {
                                        InicioContent(
                                            state = state,
                                            termoPesquisa = termoPesquisa,
                                            onAbrirDetalhes = { disciplina ->
                                                detalhesAtivos = DetalhesAtivos(
                                                    disciplina = disciplina,
                                                    alunoNome = state.dados.alunoNome,
                                                    alunoNumero = state.dados.alunoNumero,
                                                    turmaNome = state.dados.turmaNome
                                                )
                                                abaAtiva = Aba.DETALHES
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Aba.NOTIFICACOES -> {
                            NotificacoesContent(
                                notificacoes = notificacoes,
                                carregando = notifCarregando,
                                onToggleLida = { notificacoesViewModel.toggleLida(it) },
                                onDeletar = { notificacoesViewModel.deletar(it) },
                                mostrarMenu = mostrarMenuNotif,
                                onMostrarMenuChange = { mostrarMenuNotif = it },
                                onMarcarTodasLidas = { notificacoesViewModel.marcarTodasComoLidas() }
                            )
                        }

                        Aba.DETALHES -> {
                            val d = detalhesAtivos
                            if (d != null) {
                                DetalhesContent(
                                    disciplinaNome = d.disciplina.nomeDisciplina,
                                    professor = d.disciplina.professor,
                                    alunoNome = d.alunoNome,
                                    alunoNumero = d.alunoNumero,
                                    turmaNome = d.turmaNome,
                                    notas = d.disciplina.notas
                                )
                            }
                        }
                    }
                }
            }
        }

        if (mostrarDialogoSair) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoSair = false },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                title = { Text("Sair da conta") },
                text = { Text("Deseja sair da sua conta? Será preciso entrar novamente para acompanhar as notas.") },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarDialogoSair = false
                        perfilViewModel.sairDaConta(onSairDaConta)
                    }) { Text("Sair") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoSair = false }) { Text("Cancelar") }
                }
            )
        }

        if (mostrarSeletorAluno) {
            SeletorAlunoSheet(
                perfis = perfisSalvos,
                onSelecionar = { perfil ->
                    mostrarSeletorAluno = false
                    perfilViewModel.ativarPerfil(perfil.codigoAluno)
                },
                onAdicionarOutro = {
                    mostrarSeletorAluno = false
                    mostrarAdicionarAluno = true
                },
                onDismiss = { mostrarSeletorAluno = false }
            )
        }

        if (mostrarAdicionarAluno) {
            AdicionarAlunoSheet(
                onDismiss = { mostrarAdicionarAluno = false },
                onAlunoAtivado = { mostrarAdicionarAluno = false }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Conteúdo: Início
// ---------------------------------------------------------------------------
@Composable
private fun InicioContent(
    state: PerfilUiState.Sucesso,
    termoPesquisa: String,
    onAbrirDetalhes: (DisciplinaComNotas) -> Unit
) {
    val disciplinasFiltradas = remember(state.dados.disciplinas, termoPesquisa) {
        if (termoPesquisa.isBlank()) state.dados.disciplinas
        else state.dados.disciplinas.filter {
            it.nomeDisciplina.contains(termoPesquisa, ignoreCase = true)
        }
    }

    val pesquisaAtiva = termoPesquisa.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xl)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            ) {
                Text(
                    "Início",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.offline) {
                    Spacer(Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Exibindo dados salvos · sem conexão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (disciplinasFiltradas.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (pesquisaAtiva) Icons.Filled.Search else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        if (pesquisaAtiva) "Nenhuma disciplina encontrada" else "Nenhuma disciplina por aqui ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        if (pesquisaAtiva) "Tente pesquisar com outro nome de disciplina."
                        else "Assim que as notas forem lançadas, as disciplinas aparecem aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(disciplinasFiltradas, key = { it.disciplinaId }) { disciplina ->
                DisciplinaCard(
                    disciplina = disciplina,
                    onClick = { onAbrirDetalhes(disciplina) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs + 2.dp)
                        .animateItem()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Conteúdo: Notificações
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificacoesContent(
    notificacoes: List<NotificacaoNota>,
    carregando: Boolean,
    onToggleLida: (NotificacaoNota) -> Unit,
    onDeletar: (NotificacaoNota) -> Unit,
    mostrarMenu: Boolean,
    onMostrarMenuChange: (Boolean) -> Unit,
    onMarcarTodasLidas: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xl)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notificações",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    Surface(
                        onClick = { onMostrarMenuChange(true) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = "Mais opções",
                                tint = if (notificacoes.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = mostrarMenu,
                        onDismissRequest = { onMostrarMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Marcar todas como lidas") },
                            onClick = {
                                onMostrarMenuChange(false)
                                onMarcarTodasLidas()
                            }
                        )
                    }
                }
            }
        }

        when {
            carregando -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            }
            notificacoes.isEmpty() -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> items(notificacoes, key = { it.id }) { notificacao ->
                NotificacaoItem(
                    notificacao = notificacao,
                    onClick = { onToggleLida(notificacao) },
                    onDeletar = { onDeletar(notificacao) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal - 4.dp, vertical = Spacing.xs)
                        .animateItem()
                )
            }
        }
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
            if (value == SwipeToDismissBoxValue.EndToStart) { onDeletar(); true } else false
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
                Icon(Icons.Filled.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (notificacao.tipo) {
                            TipoNotificacao.NOTA_LANCADA -> Icons.AutoMirrored.Filled.Assignment
                            TipoNotificacao.NOTA_ALTERADA -> Icons.Filled.Edit
                            TipoNotificacao.NOTA_DELETADA -> Icons.Filled.Delete
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(Spacing.sm + 4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            val quem = notificacao.professorNome.ifBlank { null }
                            if (quem != null) {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(quem) }
                                append(" ")
                            } else {
                                append("Um professor ")
                            }
                            append(
                                when (notificacao.tipo) {
                                    TipoNotificacao.NOTA_LANCADA -> "lançou nota"
                                    TipoNotificacao.NOTA_ALTERADA -> "atualizou a nota"
                                    TipoNotificacao.NOTA_DELETADA -> "removeu a nota"
                                }
                            )
                            append(" de ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(notificacao.disciplina) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(if (notificacao.lida) 0.6f else 1f)
                    )
                    Spacer(Modifier.height(2.dp))
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

// ---------------------------------------------------------------------------
// Conteúdo: Detalhes da disciplina
// ---------------------------------------------------------------------------
@Composable
private fun DetalhesContent(
    disciplinaNome: String,
    professor: String,
    alunoNome: String,
    alunoNumero: Int,
    turmaNome: String,
    notas: Map<String, String>
) {
    data class LinhaTrimestre(
        val trimestre: String, val acs1: String, val acs2: String, val acs3: String,
        val macs: String, val at: String, val mt: String
    )

    val linhas = remember(notas) {
        listOf(
            LinhaTrimestre("1º", notas["campo_0"] ?: "-", notas["campo_1"] ?: "-", notas["campo_2"] ?: "-", notas["campo_3"] ?: "-", notas["campo_4"] ?: "-", notas["campo_5"] ?: "-"),
            LinhaTrimestre("2º", notas["campo_6"] ?: "-", notas["campo_7"] ?: "-", notas["campo_8"] ?: "-", notas["campo_9"] ?: "-", notas["campo_10"] ?: "-", notas["campo_11"] ?: "-"),
            LinhaTrimestre("3º", notas["campo_12"] ?: "-", notas["campo_13"] ?: "-", notas["campo_14"] ?: "-", notas["campo_15"] ?: "-", notas["campo_16"] ?: "-", notas["campo_17"] ?: "-")
        )
    }
    val mediaFinal = remember(notas) {
        val mt1 = notas["campo_5"]?.replace(",", ".")?.toFloatOrNull()
        val mt2 = notas["campo_11"]?.replace(",", ".")?.toFloatOrNull()
        val mt3 = notas["campo_17"]?.replace(",", ".")?.toFloatOrNull()
        val validas = listOfNotNull(mt1, mt2, mt3)
        if (validas.isEmpty()) null else (validas.sum() / validas.size).roundToInt()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Column {
                Text(disciplinaNome, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.xs))
                Text("Prof. $professor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("$alunoNome (Nº $alunoNumero) · $turmaNome", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Column {
                        Row(
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)).padding(vertical = Spacing.sm)
                        ) {
                            TitulosColunas.forEachIndexed { index, titulo ->
                                val largura = if (index == 0) LarguraColunaTrimestre else LarguraColunaDado
                                Box(modifier = Modifier.width(largura), contentAlignment = Alignment.Center) {
                                    Text(titulo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (index != TitulosColunas.lastIndex) {
                                    VerticalDivider(modifier = Modifier.height(20.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                    }
                    linhas.forEachIndexed { rowIndex, linha ->
                        val corFundo = if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        val valores = listOf(linha.trimestre, linha.acs1, linha.acs2, linha.acs3, linha.macs, linha.at, linha.mt)
                        Column {
                            Row(modifier = Modifier.background(corFundo).padding(vertical = Spacing.sm + 2.dp)) {
                                valores.forEachIndexed { index, valor ->
                                    val largura = if (index == 0) LarguraColunaTrimestre else LarguraColunaDado
                                    Box(modifier = Modifier.width(largura), contentAlignment = Alignment.Center) {
                                        if (index == 6) {
                                            val notaFloat = valor.replace(",", ".").toFloatOrNull()
                                            val cor = when {
                                                notaFloat == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                                notaFloat < 10f -> CapulanaRed
                                                else -> CapulanaGreen
                                            }
                                            Surface(shape = RoundedCornerShape(8.dp), color = cor.copy(alpha = 0.12f)) {
                                                Text(
                                                    valor,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = cor,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                                                )
                                            }
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
                                    if (index != TitulosColunas.lastIndex) {
                                        VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
        item {
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
                    Text("Média final", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Surface(shape = RoundedCornerShape(12.dp), color = cor.copy(alpha = 0.15f)) {
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
    }
}

// ---------------------------------------------------------------------------
// Componentes compartilhados
// ---------------------------------------------------------------------------
@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun ErroState(mensagem: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.md))
        Text(mensagem, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PesquisarDisciplinasField(
    valor: String,
    onValorChange: (String) -> Unit
) {
    TextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = {
            Text(
                text = "Pesquisar disciplinas",
                maxLines = 1
            )
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(
                    onClick = { onValorChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,

            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun DisciplinaCard(disciplina: DisciplinaComNotas, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val visual = remember(disciplina.nomeDisciplina) { visualParaDisciplina(disciplina.nomeDisciplina) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = visual.corFundo, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = visual.icone, contentDescription = null, tint = visual.corIcone, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Text(disciplina.nomeDisciplina, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuHamburguerCabecalho(
    fotoUrl: String?,
    alunoNome: String,
    alunoNumero: Int,
    turmaNome: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(48.dp)
        ) {
            if (fotoUrl != null) {
                AsyncImage(model = fotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(Spacing.sm + 4.dp))
        Column {
            Text(
                alunoNome,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "$turmaNome · Nº $alunoNumero",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PerfilAvatarButton(fotoUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.size(40.dp)) {
        if (fotoUrl != null) {
            AsyncImage(model = fotoUrl, contentDescription = "Conta", modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = "Conta", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun InicioBottomBar(
    abaAtiva: Aba,
    naoLidas: Int,
    onIrInicio: () -> Unit,
    onIrNotificacoes: () -> Unit,
    onNovaConsulta: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onIrInicio,
                shape = CircleShape,
                color = if (abaAtiva == Aba.INICIO || abaAtiva == Aba.DETALHES)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    Color.Transparent,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Início",
                        tint = if (abaAtiva == Aba.INICIO || abaAtiva == Aba.DETALHES)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = onIrNotificacoes,
                shape = CircleShape,
                color = if (abaAtiva == Aba.NOTIFICACOES)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    Color.Transparent,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    BadgedBox(
                        badge = {
                            if (naoLidas > 0) Badge { Text(if (naoLidas > 99) "99+" else naoLidas.toString()) }
                        }
                    ) {
                        Icon(
                            imageVector = if (naoLidas > 0) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                            contentDescription = "Notificações",
                            tint = if (abaAtiva == Aba.NOTIFICACOES)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onNovaConsulta, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Nova consulta")
            }
        }
    }
}

private fun formatarDataHora(timestamp: Long): String {
    val diferenca = System.currentTimeMillis() - timestamp
    return when {
        diferenca < 60_000 -> "agora"
        diferenca < 3_600_000 -> "${diferenca / 60_000}min"
        diferenca < 86_400_000 -> "${diferenca / 3_600_000}h"
        diferenca < 604_800_000 -> "${diferenca / 86_400_000}d"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}