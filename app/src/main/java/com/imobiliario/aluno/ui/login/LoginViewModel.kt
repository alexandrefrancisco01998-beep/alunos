package com.imobiliario.aluno.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imobiliario.aluno.data.repository.AuthRepository
import com.imobiliario.aluno.data.repository.AuthResult
import com.imobiliario.aluno.data.repository.FcmTokenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val verificandoSessao: Boolean = true,
    val jaAutenticado: Boolean = false,

    // ---- Google ----
    val entrandoComGoogle: Boolean = false,
    val erroGoogle: String? = null,

    // ---- Email/senha ----
    val modoCadastro: Boolean = false, // false = entrar, true = criar conta
    val email: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val carregandoEmail: Boolean = false,
    val erroEmail: String? = null,

    // ---- Evento de sucesso (qualquer um dos dois métodos) ----
    val autenticadoAgora: Boolean = false
) {
    val botaoEmailHabilitado: Boolean
        get() {
            if (carregandoEmail) return false
            if (email.isBlank() || senha.isBlank()) return false
            if (modoCadastro && senha != confirmarSenha) return false
            return true
        }
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val fcmTokenRepository = FcmTokenRepository()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        verificarSessao()
    }

    private fun verificarSessao() {
        val jaLogado = authRepository.usuarioAtual != null
        _uiState.value = _uiState.value.copy(
            verificandoSessao = false,
            jaAutenticado = jaLogado
        )
        if (jaLogado) {
            viewModelScope.launch { fcmTokenRepository.registrarTokenAtual() }
        }
    }

    // ---------------- Google ----------------

    fun entrarComGoogle(activityContext: android.content.Context) {
        if (_uiState.value.entrandoComGoogle) return
        _uiState.value = _uiState.value.copy(entrandoComGoogle = true, erroGoogle = null)

        viewModelScope.launch {
            when (val resultado = authRepository.entrarComGoogle(activityContext)) {
                is AuthResult.Sucesso -> {
                    _uiState.value = _uiState.value.copy(
                        entrandoComGoogle = false,
                        autenticadoAgora = true
                    )
                    viewModelScope.launch { fcmTokenRepository.registrarTokenAtual() }
                }
                is AuthResult.Cancelado -> {
                    _uiState.value = _uiState.value.copy(entrandoComGoogle = false)
                }
                is AuthResult.Erro -> {
                    _uiState.value = _uiState.value.copy(
                        entrandoComGoogle = false,
                        erroGoogle = resultado.mensagem
                    )
                }
            }
        }
    }

    // ---------------- Email/senha ----------------

    fun alternarModoCadastro() {
        _uiState.value = _uiState.value.copy(
            modoCadastro = !_uiState.value.modoCadastro,
            erroEmail = null,
            confirmarSenha = ""
        )
    }

    fun onEmailChange(valor: String) {
        _uiState.value = _uiState.value.copy(email = valor, erroEmail = null)
    }

    fun onSenhaChange(valor: String) {
        _uiState.value = _uiState.value.copy(senha = valor, erroEmail = null)
    }

    fun onConfirmarSenhaChange(valor: String) {
        _uiState.value = _uiState.value.copy(confirmarSenha = valor, erroEmail = null)
    }

    fun enviarFormularioEmail() {
        val estado = _uiState.value
        if (!estado.botaoEmailHabilitado) return

        _uiState.value = estado.copy(carregandoEmail = true, erroEmail = null)

        viewModelScope.launch {
            val resultado = if (estado.modoCadastro) {
                authRepository.cadastrarComEmail(estado.email.trim(), estado.senha)
            } else {
                authRepository.entrarComEmail(estado.email.trim(), estado.senha)
            }

            when (resultado) {
                is AuthResult.Sucesso -> {
                    _uiState.value = _uiState.value.copy(
                        carregandoEmail = false,
                        autenticadoAgora = true
                    )
                    viewModelScope.launch { fcmTokenRepository.registrarTokenAtual() }
                }
                is AuthResult.Cancelado -> {
                    _uiState.value = _uiState.value.copy(carregandoEmail = false)
                }
                is AuthResult.Erro -> {
                    _uiState.value = _uiState.value.copy(
                        carregandoEmail = false,
                        erroEmail = resultado.mensagem
                    )
                }
            }
        }
    }

    fun consumirEventoAutenticado() {
        _uiState.value = _uiState.value.copy(autenticadoAgora = false)
    }
}
