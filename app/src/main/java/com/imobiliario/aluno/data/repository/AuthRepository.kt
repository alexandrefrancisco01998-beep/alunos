package com.imobiliario.aluno.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.imobiliario.aluno.R
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Resultado de uma tentativa de login: sucesso, cancelamento pelo usuário, ou falha tipada. */
sealed interface AuthResult {
    data object Sucesso : AuthResult
    data object Cancelado : AuthResult
    data class Erro(val tipo: AuthErro, val mensagem: String) : AuthResult
}

enum class AuthErro {
    SEM_REDE,
    NENHUMA_CONTA,
    CREDENCIAL_INVALIDA,
    FALHA_SERVIDOR,
    EMAIL_INVALIDO,
    SENHA_FRACA,
    EMAIL_JA_CADASTRADO,
    CREDENCIAIS_ERRADAS
}

/**
 * Repositório de autenticação Google — usa Credential Manager (API moderna,
 * substitui o antigo GoogleSignInClient), o mesmo padrão já usado no app
 * do Professor/Encarregado (com.ap.pautaa).
 *
 * Não conhece Activity, Toast ou qualquer coisa de UI de verdade — mas
 * entrarComGoogle() EXIGE um Context de Activity (não o Application),
 * porque o Credential Manager precisa abrir o seletor de contas por cima
 * de uma tela visível. Passar o Application Context aqui falha em runtime
 * com "Failed to launch the selector UI" — por isso o Context não fica
 * guardado no construtor, é pedido a cada chamada, vindo de quem tem
 * acesso à Activity atual (o Composable, via LocalContext.current).
 *
 * Estratégia recomendada pelo Google: tenta primeiro SILENCIOSAMENTE
 * (só contas já autorizadas neste app antes — sem abrir seletor nenhum).
 * Se não houver nenhuma, cai para o seletor completo (todas as contas
 * do aparelho). Isso evita mostrar UI de escolha de conta toda vez que
 * o app já sabe qual conta o usuário usa.
 *
 * SEGURANÇA: nenhuma credencial, token de ID, e-mail ou exceção com
 * dados do usuário é logada aqui, em nenhum build. As mensagens
 * expostas em [AuthResult.Erro] são strings fixas e amigáveis — nunca
 * o conteúdo bruto da exceção do SDK — para não vazar detalhes internos
 * do Firebase/Google em Toast/Snackbar de erro.
 */
class AuthRepository(
    applicationContext: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val credentialManager: CredentialManager = CredentialManager.create(applicationContext)
) {

    val usuarioAtual get() = auth.currentUser

    /**
     * @param activityContext Context da Activity atual (ex: LocalContext.current
     * dentro de um Composable). Não passe o Application Context aqui — o
     * seletor de contas do Google precisa de um Context com janela associada.
     */
    suspend fun entrarComGoogle(activityContext: Context): AuthResult {
        // 1ª tentativa: silenciosa, só contas já autorizadas — sem seletor visível.
        val resultadoSilencioso = tentarObterCredencial(activityContext, apenasContasAutorizadas = true)
        if (resultadoSilencioso != null) return resultadoSilencioso

        // 2ª tentativa: seletor completo, mostra todas as contas do aparelho.
        return tentarObterCredencial(activityContext, apenasContasAutorizadas = false)
            ?: AuthResult.Erro(AuthErro.NENHUMA_CONTA, "Nenhuma conta Google disponível neste aparelho.")
    }

    /**
     * Retorna null apenas quando a tentativa silenciosa não encontrou
     * nenhuma conta autorizada (NoCredentialException) — nesse caso quem
     * chamou deve tentar de novo com apenasContasAutorizadas = false.
     * Qualquer outro desfecho (sucesso, cancelamento, erro real) é final.
     */
    private suspend fun tentarObterCredencial(
        activityContext: Context,
        apenasContasAutorizadas: Boolean
    ): AuthResult? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(apenasContasAutorizadas)
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(apenasContasAutorizadas)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(request = request, context = activityContext)
            autenticarNoFirebase(result.credential)
        } catch (e: NoCredentialException) {
            if (apenasContasAutorizadas) null // sinaliza: tenta o seletor completo em seguida
            else AuthResult.Erro(AuthErro.NENHUMA_CONTA, "Nenhuma conta Google disponível neste aparelho.")
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelado
        } catch (e: GetCredentialException) {
            // Mensagem fixa e amigável — não propaga e.message (pode conter
            // detalhes internos do provedor de credenciais) para a UI.
            AuthResult.Erro(AuthErro.CREDENCIAL_INVALIDA, "Falha ao obter credencial do Google.")
        }
    }

    private suspend fun autenticarNoFirebase(credential: androidx.credentials.Credential): AuthResult {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return AuthResult.Erro(AuthErro.CREDENCIAL_INVALIDA, "Credencial não suportada.")
        }

        val idToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            return AuthResult.Erro(AuthErro.CREDENCIAL_INVALIDA, "Token do Google inválido.")
        }

        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            AuthResult.Sucesso
        } catch (e: FirebaseNetworkException) {
            AuthResult.Erro(AuthErro.SEM_REDE, "Sem conexão com a internet.")
        } catch (e: IOException) {
            AuthResult.Erro(AuthErro.SEM_REDE, "Sem conexão com a internet.")
        } catch (e: Exception) {
            AuthResult.Erro(AuthErro.FALHA_SERVIDOR, "Falha ao autenticar com o Firebase.")
        }
    }

    /** Login com e-mail e senha numa conta já existente. */
    suspend fun entrarComEmail(email: String, senha: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, senha).await()
            AuthResult.Sucesso
        } catch (e: Exception) {
            e.paraAuthResultErro()
        }
    }

    /** Cria uma conta nova com e-mail e senha. */
    suspend fun cadastrarComEmail(email: String, senha: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email, senha).await()
            AuthResult.Sucesso
        } catch (e: Exception) {
            e.paraAuthResultErro()
        }
    }

    /**
     * Mapeia a exceção para uma mensagem fixa por tipo — nunca repassa
     * `message` de exceções desconhecidas para a UI, pois esse texto pode
     * ecoar o e-mail informado ou detalhes internos do Firebase.
     */
    private fun Exception.paraAuthResultErro(): AuthResult.Erro {
        return when (this) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException,
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                AuthResult.Erro(AuthErro.CREDENCIAIS_ERRADAS, "E-mail ou senha incorretos.")
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                AuthResult.Erro(AuthErro.EMAIL_JA_CADASTRADO, "Já existe uma conta com este e-mail.")
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                AuthResult.Erro(AuthErro.SENHA_FRACA, "A senha deve ter pelo menos 6 caracteres.")
            is FirebaseNetworkException, is IOException ->
                AuthResult.Erro(AuthErro.SEM_REDE, "Sem conexão com a internet.")
            else ->
                AuthResult.Erro(AuthErro.FALHA_SERVIDOR, "Falha ao autenticar.")
        }
    }

    /**
     * Encerra a sessão do Firebase. Isso invalida os tokens locais, mas
     * NÃO limpa dados em cache (Room) nem revoga a conta Google
     * selecionada no Credential Manager — quem chama este método é
     * responsável por também limpar o cache local sensível (ver
     * `PerfilViewModel.sairDaConta`).
     */
    fun sair() {
        auth.signOut()
    }
}


