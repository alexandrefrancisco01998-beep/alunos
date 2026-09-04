package com.imobiliario.aluno.ui.notificacoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imobiliario.aluno.data.repository.NotificacaoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificacoesViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Instanciado aqui dentro, não como parâmetro do construtor: o
    // ViewModelProvider padrão (usado por viewModel() no Compose) só sabe
    // construir AndroidViewModel com um único parâmetro Application — um
    // segundo parâmetro no construtor faz a reflection da factory falhar
    // com "Cannot create an instance of class ...", mesmo tendo valor
    // default. Se no futuro for preciso injetar um repository de teste,
    // use uma ViewModelProvider.Factory customizada em vez de construtor.
    private val repository: NotificacaoRepository = NotificacaoRepository()

    private var codigoAluno: String = ""
    private var jobObservacao: Job? = null

    private val _notificacoes = MutableStateFlow<List<NotificacaoNota>>(emptyList())
    val notificacoes: StateFlow<List<NotificacaoNota>> = _notificacoes.asStateFlow()

    private val _carregando = MutableStateFlow(true)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    fun iniciar(codigo: String) {
        if (codigoAluno == codigo && jobObservacao?.isActive == true) return
        codigoAluno = codigo
        _carregando.value = true

        // Cancela o listener do aluno anterior antes de assinar o novo,
        // senão ficaríamos recebendo notificações de dois alunos ao trocar.
        jobObservacao?.cancel()
        jobObservacao = viewModelScope.launch {
            repository.observarPorAluno(codigo).collectLatest { lista ->
                _notificacoes.value = lista
                _carregando.value = false
            }
        }
    }

    // As três funções abaixo disparam a escrita e não esperam o retorno na
    // UI: cada launch roda numa coroutine própria, então mesmo se o
    // dispositivo estiver offline e a Task ficar pendente até reconectar,
    // a tela não trava — o Firestore já reflete a mudança localmente e de
    // forma otimista no snapshot do listener antes de confirmar no
    // servidor, então o usuário vê o efeito na hora de qualquer forma.

    fun toggleLida(notificacao: NotificacaoNota) {
        // Nunca "destoggle": a intenção é só marcar como lida ao tocar.
        if (notificacao.lida) return
        viewModelScope.launch {
            repository.marcarComoLida(notificacao.id)
        }
    }

    fun deletar(notificacao: NotificacaoNota) {
        viewModelScope.launch {
            repository.deletar(notificacao.id)
        }
    }

    fun marcarTodasComoLidas() {
        val idsNaoLidos = _notificacoes.value.filter { !it.lida }.map { it.id }
        if (idsNaoLidos.isEmpty()) return
        viewModelScope.launch {
            repository.marcarComoLidas(idsNaoLidos)
        }
    }
}
