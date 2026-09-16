package com.imobiliario.aluno.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imobiliario.aluno.data.model.DadosConsultaAluno
import com.imobiliario.aluno.data.model.DisciplinaComNotas
import com.imobiliario.aluno.data.repository.AlunoRepository
import com.imobiliario.aluno.data.repository.AuthRepository
import com.imobiliario.aluno.data.repository.ConsultaErro
import com.imobiliario.aluno.data.repository.ConsultaResult
import com.imobiliario.aluno.data.repository.NotificacaoRepository
import com.imobiliario.aluno.data.repository.DisciplinaTempoReal
import com.imobiliario.aluno.data.repository.NotasTempoRealRepository
import com.imobiliario.aluno.data.repository.PerfilCacheRepository
import com.imobiliario.aluno.data.repository.PerfilSalvo
import com.imobiliario.aluno.data.repository.paraPerfilSalvo
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
        val perfil: PerfilSalvo,
        val dados: DadosConsultaAluno,
        val offline: Boolean
    ) : PerfilUiState
    data class Erro(val mensagem: String) : PerfilUiState
}

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = PerfilCacheRepository()
    private val repository = AlunoRepository()
    private val authRepository = AuthRepository(application)
    private val notificacaoRepository = NotificacaoRepository()
    private val notasTempoRealRepository = NotasTempoRealRepository()

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Carregando)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /**
     * Última leitura do RTDB `notas_tempo_real` para o aluno atual.
     * Guardada aqui (não só aplicada direto ao uiState) porque o
     * listener liga logo no início de [carregar], antes do estado
     * virar [PerfilUiState.Sucesso] — sem isto, uma nota lançada pelo
     * professor nesse intervalo (ou enquanto o estado está em
     * [PerfilUiState.Carregando]/[PerfilUiState.Erro]) era descartada
     * silenciosamente e só aparecia numa próxima consulta manual.
     */
    private var ultimaLeituraTempoReal: Map<String, DisciplinaTempoReal> = emptyMap()

    private var codigoAluno: String = ""

    private val uid: String get() = authRepository.usuarioAtual?.uid ?: ""

    /**
     * Todos os alunos já consultados neste aparelho, do mais recente para
     * o mais antigo — alimenta a lista de alunos no drawer do
     * [PerfilScreen]. Como nenhum perfil é apagado ao adicionar outro
     * (ver [PerfilCacheRepository.ativarPerfil]), esta lista reflete de
     * verdade tudo que já foi salvo. Vem do Realtime Database (cache
     * local via persistência offline do SDK) em vez de Room.
     */
    val perfisSalvos: StateFlow<List<PerfilSalvo>> by lazy {
        cache.observarTodosPerfis(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /**
     * Perfil atualmente ativo, como Flow — permite que a Home troque os
     * dados exibidos instantaneamente assim que o usuário escolhe outro
     * aluno no seletor, sem precisar navegar ou recarregar a tela.
     */
    val perfilAtivo: StateFlow<PerfilSalvo?> by lazy {
        cache.observarPerfilAtivo(uid)
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

    /** Nome de exibição da conta logada (Google → displayName; e-mail → parte antes do @). */
    val nomeUsuario: String
        get() {
            val user = authRepository.usuarioAtual ?: return ""
            return user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: ""
        }

    /** E-mail da conta logada, ou string vazia quando não disponível. */
    val emailUsuario: String
        get() = authRepository.usuarioAtual?.email ?: ""

    /**
     * Aplica a leitura mais recente do RTDB `notas_tempo_real` ao estado
     * atual, atualizando disciplinas já exibidas e ACRESCENTANDO as que
     * ainda não estavam na lista (ex.: professor lançou nota numa
     * disciplina nova para este aluno).
     *
     * [atualizacoes] é sempre a leitura completa e mais recente (snapshot
     * do nó inteiro do aluno, não um delta), então dá para reconstruir a
     * lista de disciplinas inteira a partir dela sem perder nada.
     *
     * Se o estado ainda não for [PerfilUiState.Sucesso] (ex.: chegou
     * enquanto a tela está em [PerfilUiState.Carregando]), a leitura já
     * fica salva em [ultimaLeituraTempoReal] por [onNotasAtualizadas] e
     * será reaplicada assim que o estado virar Sucesso — ver [carregar].
     */
    private fun mesclarNotasTempoReal(
        atualizacoes: Map<String, DisciplinaTempoReal>
    ) {
        val estado = _uiState.value as? PerfilUiState.Sucesso ?: return

        val restantes = atualizacoes.toMutableMap()

        // 1) Atualiza disciplinas já exibidas, consumindo do mapa restante.
        val disciplinasExistentesAtualizadas = estado.dados.disciplinas.map { disciplina ->
            val codigo = disciplina.codigoUnicoDisciplina
                .replace("-", "")
                .uppercase()

            val novaLeitura = restantes.remove(codigo) ?: return@map disciplina

            disciplina.copy(
                nomeDisciplina = novaLeitura.nomeDisciplina.ifBlank { disciplina.nomeDisciplina },
                professor = novaLeitura.professor.ifBlank { disciplina.professor },
                notas = disciplina.notas + novaLeitura.notas
            )
        }

        // 2) O que sobrou em `restantes` são disciplinas que o RTDB tem
        //    mas que ainda não existiam na tela — cria uma entrada nova
        //    para cada uma em vez de descartar.
        val disciplinasNovas = restantes.values.map { novaLeitura ->
            DisciplinaComNotas(
                disciplinaId = novaLeitura.codigoDisciplina.hashCode(),
                codigoUnicoDisciplina = novaLeitura.codigoDisciplina,
                nomeDisciplina = novaLeitura.nomeDisciplina,
                professor = novaLeitura.professor,
                notas = novaLeitura.notas
            )
        }

        val disciplinasAtualizadas = disciplinasExistentesAtualizadas + disciplinasNovas

        val novosDados = estado.dados.copy(
            disciplinas = disciplinasAtualizadas
        )

        _uiState.value = estado.copy(
            dados = novosDados,
            offline = false
        )

        viewModelScope.launch {
            cache.substituirDisciplinas(uid, codigoAluno, disciplinasAtualizadas)
        }
    }

    /**
     * Carrega a tela em modo "cache-first": mostra o que já está salvo
     * localmente (perfil + disciplinas da última consulta bem-sucedida)
     * imediatamente, sem esperar rede, e só depois atualiza em segundo
     * plano consultando o backend. O cache agora vem do Realtime
     * Database (persistência offline do SDK) em vez de Room — o app não
     * depende de bater no backend toda vez que a tela abre.
     */
    fun carregar(codigo: String) {
        if (codigoAluno == codigo && _uiState.value !is PerfilUiState.Erro) return
        codigoAluno = codigo
        // Troca de aluno: descarta leitura pendente do aluno anterior para
        // não misturar disciplinas de um código com o de outro.
        ultimaLeituraTempoReal = emptyMap()

        val uidAtual = uid

        notasTempoRealRepository.observar(
            uid = uidAtual,
            codigoAluno = codigo,
            onNotasAtualizadas = { atualizacoes ->
                // Guarda sempre a leitura mais recente, mesmo que o
                // estado ainda não seja Sucesso (ver comentário em
                // ultimaLeituraTempoReal) — e tenta aplicar de imediato
                // caso já seja.
                ultimaLeituraTempoReal = atualizacoes
                mesclarNotasTempoReal(atualizacoes)
            }
        )

        viewModelScope.launch {
            val perfil = cache.getPerfil(uidAtual, codigo)
            if (perfil == null) {
                _uiState.value = PerfilUiState.Erro("Perfil não encontrado.")
                return@launch
            }

            // 1) Mostra imediatamente o que já está salvo (se houver).
            val disciplinasSalvas = cache.getDisciplinas(uidAtual, codigo)
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
                // O estado acabou de virar Sucesso — se o listener do RTDB
                // já tinha entregue uma leitura antes disso (perdida até
                // agora), aplica-a já, sem esperar a próxima notificação
                // do listener.
                if (ultimaLeituraTempoReal.isNotEmpty()) {
                    mesclarNotasTempoReal(ultimaLeituraTempoReal)
                }
            }

            // 2) Atualiza em segundo plano a partir do backend.
            when (val resultado = repository.consultarPorCodigo(codigo)) {
                is ConsultaResult.Sucesso -> {
                    val dados = resultado.dados
                    // Atualiza o perfil local com os dados mais recentes do
                    // backend (nome, número, turma, classeNome podem ter mudado)
                    // e renova dataUltimaAtualizacao para refletir a sincronização.
                    // Notificações de nota alterada não são geradas no cliente:
                    // quem as cria é a Cloud Function `lancarNotasIndividuais`
                    // no momento em que o professor lança a nota.
                    cache.salvarPerfil(
                        uidAtual,
                        dados.paraPerfilSalvo(codigo, ativo = perfil.ativo),
                        dados.disciplinas
                    )
                    _uiState.value = PerfilUiState.Sucesso(perfil, dados, offline = false)
                    // Idem ao passo 1: reaplica a leitura pendente do RTDB
                    // (se houver) agora que o estado voltou a ser Sucesso —
                    // isto pega tanto a leitura antiga quanto qualquer nota
                    // lançada pelo professor durante a consulta ao backend.
                    if (ultimaLeituraTempoReal.isNotEmpty()) {
                        mesclarNotasTempoReal(ultimaLeituraTempoReal)
                    }
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
     * Troca o aluno ativo para um já salvo anteriormente — usado pela
     * lista de alunos no drawer do [PerfilScreen]. Diferente do antigo
     * fluxo de "nova consulta", aqui NADA é desativado ou apagado de
     * forma solta: [PerfilCacheRepository.ativarPerfil] troca o ativo em
     * uma única atualização multi-path, e como [uiState] é recarregado
     * a partir do novo `codigo`, a Home reflete a troca imediatamente.
     */
    fun ativarPerfil(codigo: String) {
        viewModelScope.launch {
            cache.ativarPerfil(uid, codigo)
            carregar(codigo)
        }
    }

    override fun onCleared() {
        notasTempoRealRepository.parar()
        super.onCleared()
    }

    /**
     * Encerra a sessão (Google ou e-mail/senha) e apaga todo o cache local
     * sensível: perfis de alunos consultados (nome, número, turma) e o
     * cache de disciplinas/notas, agora guardado no Realtime Database sob
     * o uid do usuário. Diferente de [ativarPerfil], aqui a conta muda —
     * não é seguro deixar dados do aluno anterior acessíveis para quem
     * entrar em seguida no mesmo aparelho.
     *
     * Notificações não têm mais cópia local (ver [NotificacaoRepository]),
     * então não há nada a apagar aqui — elas ficam no Firestore, associadas
     * ao uid do encarregado, e somem da tela sozinhas quando a sessão troca
     * (a query já filtra por uid).
     */
    fun sairDaConta(aoConcluir: () -> Unit) {
        val uidAtual = uid
        viewModelScope.launch {
            cache.apagarTudo(uidAtual)
            authRepository.sair()
            aoConcluir()
        }
    }
}
