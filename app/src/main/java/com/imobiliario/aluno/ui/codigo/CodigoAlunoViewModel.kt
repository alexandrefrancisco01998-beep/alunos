package com.imobiliario.aluno.ui.codigo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.imobiliario.aluno.data.repository.AlunoRepository
import com.imobiliario.aluno.data.repository.ConsultaErro
import com.imobiliario.aluno.data.repository.ConsultaResult
import com.imobiliario.aluno.data.repository.PerfilCacheRepository
import com.imobiliario.aluno.data.repository.paraPerfilSalvo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CodigoAlunoUiState(
    val codigo: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    // true quando vale a pena tentar de novo direto (rede/servidor);
    // false quando o usuário precisa corrigir algo (código errado).
    val erroTemporario: Boolean = false,
    val consultaSucesso: String? = null
) {
    val codigoLimpo get() = codigo.replace("-", "")
    val botaoHabilitado get() = codigoLimpo.length == 10 && !carregando
}

/**
 * ViewModel do formulário de código do aluno — hoje usado apenas dentro
 * do bottomsheet [com.imobiliario.aluno.ui.perfil.AdicionarAlunoSheet],
 * nunca mais como tela cheia própria. O cache local (perfil + eventual
 * ativação automática) vem do Realtime Database via
 * [PerfilCacheRepository], substituindo o antigo Room.
 */
class CodigoAlunoViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = PerfilCacheRepository()
    private val alunoRepository = AlunoRepository()

    private val _uiState = MutableStateFlow(CodigoAlunoUiState())
    val uiState: StateFlow<CodigoAlunoUiState> = _uiState.asStateFlow()

    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun onCodigoChange(novoValor: String) {
        val limpo = novoValor.replace("-", "").replace(" ", "").uppercase().take(10)
        val formatado = buildString {
            limpo.forEachIndexed { index, c ->
                if (index != 0 && index % 3 == 0) append("-")
                append(c)
            }
        }
        _uiState.value = _uiState.value.copy(codigo = formatado, erro = null)
    }

    /**
     * Consulta o código e, em caso de sucesso, salva o aluno e o torna o
     * ativo — usando [PerfilCacheRepository.ativarPerfil], que troca o
     * ativo numa única atualização multi-path. Nenhum perfil já salvo é
     * apagado ou perdido: quem já existia continua no cache, só deixa de
     * ser o "ativo" no momento em que este novo aluno passa a ser
     * exibido na Home. Isso vale tanto para o primeiro aluno quanto para
     * "adicionar outro aluno".
     */
    fun consultar() {
        val estado = _uiState.value
        if (!estado.botaoHabilitado) return

        _uiState.value = estado.copy(carregando = true, erro = null)

        viewModelScope.launch {
            when (val resultado = alunoRepository.consultarPorCodigo(estado.codigo)) {
                is ConsultaResult.Sucesso -> {
                    val dados = resultado.dados
                    val uidAtual = uid
                    cache.salvarPerfil(
                        uidAtual,
                        dados.paraPerfilSalvo(estado.codigoLimpo, ativo = false),
                        dados.disciplinas
                    )
                    cache.ativarPerfil(uidAtual, estado.codigoLimpo)
                    _uiState.value = _uiState.value.copy(
                        carregando = false,
                        consultaSucesso = estado.codigoLimpo
                    )
                }
                is ConsultaResult.Erro -> {
                    _uiState.value = _uiState.value.copy(
                        carregando = false,
                        erro = resultado.mensagem,
                        erroTemporario = resultado.tipo == ConsultaErro.SEM_REDE ||
                                resultado.tipo == ConsultaErro.FALHA_SERVIDOR
                    )
                }
            }
        }
    }

    fun consumirEventoConsulta() {
        _uiState.value = _uiState.value.copy(consultaSucesso = null)
    }
}
