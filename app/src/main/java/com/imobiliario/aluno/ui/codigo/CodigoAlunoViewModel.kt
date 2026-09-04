package com.imobiliario.aluno.ui.codigo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imobiliario.aluno.data.local.AppDatabase
import com.imobiliario.aluno.data.local.PerfilAluno
import com.imobiliario.aluno.data.repository.AlunoRepository
import com.imobiliario.aluno.data.repository.ConsultaErro
import com.imobiliario.aluno.data.repository.ConsultaResult
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
    val perfilSalvoEncontrado: String? = null,
    val consultaSucesso: String? = null
) {
    val codigoLimpo get() = codigo.replace("-", "")
    val botaoHabilitado get() = codigoLimpo.length == 6 && !carregando
}

class CodigoAlunoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val alunoRepository = AlunoRepository()

    private val _uiState = MutableStateFlow(CodigoAlunoUiState())
    val uiState: StateFlow<CodigoAlunoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val perfil = database.perfilAlunoDao().getPerfilAtivo()
            _uiState.value = _uiState.value.copy(perfilSalvoEncontrado = perfil?.codigoAluno)
        }
    }

    fun onCodigoChange(novoValor: String) {
        val limpo = novoValor.replace("-", "").replace(" ", "").uppercase().take(6)
        val formatado = if (limpo.length > 3) {
            "${limpo.substring(0, 3)}-${limpo.substring(3)}"
        } else limpo
        _uiState.value = _uiState.value.copy(codigo = formatado, erro = null)
    }

    /**
     * Consulta o código e, em caso de sucesso, salva o aluno e o torna o
     * ativo — usando [PerfilAlunoDao.ativarPerfil], que troca o ativo numa
     * única transação. Nenhum perfil já salvo é apagado ou perdido: quem
     * já existia continua no banco, só deixa de ser o "ativo" no momento
     * em que este novo aluno passa a ser exibido na Home. Isso vale tanto
     * para o primeiro login quanto para "adicionar outro aluno".
     */
    fun consultar() {
        val estado = _uiState.value
        if (!estado.botaoHabilitado) return

        _uiState.value = estado.copy(carregando = true, erro = null)

        viewModelScope.launch {
            when (val resultado = alunoRepository.consultarPorCodigo(estado.codigo)) {
                is ConsultaResult.Sucesso -> {
                    val dados = resultado.dados
                    database.perfilAlunoDao().inserirPerfil(
                        PerfilAluno(
                            codigoAluno = estado.codigoLimpo,
                            nomeAluno = dados.alunoNome,
                            numeroAluno = dados.alunoNumero,
                            turmaNome = dados.turmaNome,
                            classeNome = dados.classeNome,
                            ativo = false
                        )
                    )
                    database.perfilAlunoDao().ativarPerfil(estado.codigoLimpo)
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
