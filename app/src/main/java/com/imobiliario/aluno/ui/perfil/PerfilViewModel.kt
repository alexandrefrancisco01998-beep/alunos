package com.imobiliario.aluno.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imobiliario.aluno.data.local.AppDatabase
import com.imobiliario.aluno.data.local.PerfilAluno
import com.imobiliario.aluno.data.local.paraCache
import com.imobiliario.aluno.data.local.paraDisciplinas
import com.imobiliario.aluno.data.model.DadosConsultaAluno
import com.imobiliario.aluno.data.repository.AlunoRepository
import com.imobiliario.aluno.data.repository.AuthRepository
import com.imobiliario.aluno.data.repository.ConsultaErro
import com.imobiliario.aluno.data.repository.ConsultaResult
import com.imobiliario.aluno.data.repository.NotificacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PerfilUiState {
    data object Carregando : PerfilUiState
    data class Sucesso(
        val perfil: PerfilAluno,
        val dados: DadosConsultaAluno,
        val offline: Boolean
    ) : PerfilUiState
    data class Erro(val mensagem: String) : PerfilUiState
}

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AlunoRepository()
    private val authRepository = AuthRepository(application)
    private val notificacaoRepository = NotificacaoRepository()

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Carregando)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    private var codigoAluno: String = ""

    /**
     * Todos os alunos já consultados neste aparelho, do mais recente para
     * o mais antigo — alimenta o [SeletorAlunoSheet]. Como nenhum perfil
     * é apagado ao adicionar outro (ver [PerfilAlunoDao.ativarPerfil]),
     * esta lista reflete de verdade tudo que já foi salvo.
     */
    val perfisSalvos: StateFlow<List<PerfilAluno>> by lazy {
        database.perfilAlunoDao().listarTodosPerfis()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /**
     * Perfil atualmente ativo, como Flow — permite que a Home troque os
     * dados exibidos instantaneamente assim que o usuário escolhe outro
     * aluno no seletor, sem precisar navegar ou recarregar a tela.
     */
    val perfilAtivo: StateFlow<PerfilAluno?> by lazy {
        database.perfilAlunoDao().observarPerfilAtivo()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    /**
     * Contagem de não lidas derivada da MESMA fonte que a tela de
     * Notificações usa (Firestore, via [NotificacaoRepository]) — antes
     * vinha de uma contagem separada em Room que nunca era atualizada
     * pelo mesmo caminho que populava a lista, então o número do badge
     * podia ficar preso ou incoerente com o que a lista realmente
     * mostrava. Reage à troca de aluno através de [perfilAtivo].
     */
    val notificacoesNaoLidas: StateFlow<Int> by lazy {
        perfilAtivo
            .flatMapLatest { perfil ->
                val codigo = perfil?.codigoAluno
                if (codigo.isNullOrBlank()) {
                    flowOf(0)
                } else {
                    notificacaoRepository.observarPorAluno(codigo).map { lista ->
                        lista.count { !it.lida }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    /**
     * URL da foto de perfil do usuário autenticado, quando existir.
     * Vem direto do FirebaseUser: para login via Google, o Firebase já
     * preenche `photoUrl` com a foto pública da conta Google usada no
     * login (a mesma exibida no seletor de contas). Para login por
     * e-mail/senha, `photoUrl` é sempre null — nesse caso a TopBar cai
     * de volta no ícone genérico de conta.
     */
    val fotoPerfilUrl: String? get() = authRepository.usuarioAtual?.photoUrl?.toString()

    /**
     * Carrega a tela em modo "cache-first": mostra o que já está salvo
     * localmente (perfil + disciplinas da última consulta bem-sucedida)
     * imediatamente, sem esperar rede, e só depois atualiza em segundo
     * plano consultando o backend. Assim o app não depende de bater no
     * backend toda vez que a tela abre — funciona com os últimos dados
     * salvos e atualiza quando há conexão.
     */
    fun carregar(codigo: String) {
        if (codigoAluno == codigo && _uiState.value !is PerfilUiState.Erro) return
        codigoAluno = codigo
        viewModelScope.launch {
            val perfil = database.perfilAlunoDao().getPerfilPorCodigo(codigo)
            if (perfil == null) {
                _uiState.value = PerfilUiState.Erro("Perfil não encontrado.")
                return@launch
            }

            // 1) Mostra imediatamente o que já está salvo (se houver).
            val disciplinasSalvas = database.disciplinaDao().listarPorAluno(codigo).paraDisciplinas()
            if (disciplinasSalvas.isNotEmpty()) {
                _uiState.value = PerfilUiState.Sucesso(
                    perfil = perfil,
                    dados = DadosConsultaAluno(
                        alunoNome = perfil.nomeAluno,
                        alunoNumero = perfil.numeroAluno,
                        turmaNome = perfil.turmaNome,
                        classeNome = perfil.classeNome,
                        disciplinas = disciplinasSalvas
                    ),
                    offline = true
                )
            }

            // 2) Atualiza em segundo plano a partir do backend.
            when (val resultado = repository.consultarPorCodigo(codigo)) {
                is ConsultaResult.Sucesso -> {
                    _uiState.value = PerfilUiState.Sucesso(perfil, resultado.dados, offline = false)
                    // Notificações de nota alterada não são mais geradas no
                    // cliente comparando cache local: quem cria a
                    // notificação (no Firestore, `notificacoesAluno`) é a
                    // Cloud Function `lancarNotasIndividuais` no momento em
                    // que o professor lança a nota — fonte única, sem
                    // depender deste dispositivo já ter aberto o app antes.
                    database.disciplinaDao().substituir(codigo, resultado.dados.disciplinas.paraCache(codigo))
                }
                is ConsultaResult.Erro -> {
                    if (resultado.tipo == ConsultaErro.NAO_AUTENTICADO) {
                        // Sessão do Google expirou/foi revogada. Se já há dados
                        // salvos na tela, mantém o que está sendo exibido
                        // (o usuário ainda consegue ver as notas conhecidas);
                        // só bloqueia com erro se não houver nada salvo.
                        if (disciplinasSalvas.isEmpty()) {
                            _uiState.value = PerfilUiState.Erro(
                                "Sua sessão expirou. Faça login novamente para atualizar as notas."
                            )
                        }
                        return@launch
                    }

                    // Demais falhas (rede, servidor): se não havia nada em
                    // cache, mostra ao menos o perfil salvo com lista vazia
                    // em modo offline. Se já havia cache, o estado já
                    // definido no passo 1 continua valendo.
                    if (disciplinasSalvas.isEmpty()) {
                        val dadosOffline = DadosConsultaAluno(
                            alunoNome = perfil.nomeAluno,
                            alunoNumero = perfil.numeroAluno,
                            turmaNome = perfil.turmaNome,
                            classeNome = perfil.classeNome,
                            disciplinas = emptyList()
                        )
                        _uiState.value = PerfilUiState.Sucesso(perfil, dadosOffline, offline = true)
                    }
                }
            }
        }
    }

    /**
     * Troca o aluno ativo para um já salvo anteriormente — usado pelo
     * [SeletorAlunoSheet]. Diferente do antigo fluxo de "nova consulta",
     * aqui NADA é desativado ou apagado de forma solta: `ativarPerfil`
     * troca o ativo em uma única transação, e como [uiState] é recarregado
     * a partir do novo `codigo`, a Home reflete a troca imediatamente.
     */
    fun ativarPerfil(codigo: String) {
        viewModelScope.launch {
            database.perfilAlunoDao().ativarPerfil(codigo)
            carregar(codigo)
        }
    }

    /**
     * Encerra a sessão (Google ou e-mail/senha) e apaga todo o cache local
     * sensível: perfis de alunos consultados (nome, número, turma) e o
     * cache de disciplinas/notas. Diferente de [ativarPerfil], aqui a
     * conta muda — não é seguro deixar dados do aluno anterior acessíveis
     * para quem entrar em seguida no mesmo aparelho.
     *
     * Notificações não têm mais cópia local (ver [NotificacaoRepository]),
     * então não há nada a apagar aqui — elas ficam no Firestore, associadas
     * ao uid do encarregado, e somem da tela sozinhas quando a sessão troca
     * (a query já filtra por uid).
     */
    fun sairDaConta(aoConcluir: () -> Unit) {
        viewModelScope.launch {
            database.perfilAlunoDao().apagarTodosPerfis()
            database.disciplinaDao().apagarTodas()
            authRepository.sair()
            aoConcluir()
        }
    }
}
