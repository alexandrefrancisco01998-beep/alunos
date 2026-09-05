package com.imobiliario.aluno.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.imobiliario.aluno.BuildConfig
import com.imobiliario.aluno.data.model.DadosConsultaAluno
import com.imobiliario.aluno.data.model.DisciplinaComNotas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException

/** Resultado de uma consulta: sucesso com dados, ou falha tipada com mensagem amigável. */
sealed interface ConsultaResult {
    data class Sucesso(val dados: DadosConsultaAluno) : ConsultaResult
    data class Erro(val tipo: ConsultaErro, val mensagem: String) : ConsultaResult
}

enum class ConsultaErro {
    NAO_AUTENTICADO,
    CODIGO_INVALIDO,
    NAO_ENCONTRADO,
    LIMITE_EXCEDIDO,
    SEM_REDE,
    FALHA_SERVIDOR
}

/**
 * Repositório de consulta de notas por código do aluno.
 *
 * Chama a Cloud Function `consultarNotasPorCodigo` (App Check + rate
 * limit + exige login Google no backend) em vez de ler o Realtime
 * Database direto — a leitura pública sem login não é mais permitida.
 *
 * Por isso é obrigatório haver um usuário Firebase autenticado
 * (ver AuthRepository/Google Sign-In) antes de chamar consultarPorCodigo.
 *
 * Falhas de REDE (sem internet, timeout, DNS) são tentadas de novo
 * automaticamente com backoff exponencial — falhas de NEGÓCIO (código
 * inválido, aluno não encontrado, rate limit) nunca são, porque tentar
 * de novo não muda o resultado e ainda gastaria saldo do rate limit.
 *
 * SEGURANÇA:
 * - A validação de formato do código (6 caracteres) acontece ANTES de
 *   qualquer chamada de rede — evita gastar saldo de rate limit da
 *   Cloud Function com entradas obviamente inválidas.
 * - A autorização por aluno/turma é responsabilidade da Cloud Function
 *   (App Check + regras do backend) — este repositório nunca lê
 *   Firestore/RTDB diretamente, então não há risco de expor dados de
 *   outro aluno por regra de leitura aberta no cliente.
 * - Nenhum log aqui inclui o código do aluno, nome do aluno ou qualquer
 *   dado pessoal — apenas metadados estruturais (tipo inesperado, índice
 *   do item) úteis para depurar problemas de parsing. Os logs de aviso
 *   (Log.w) só são emitidos em build de debug (`BuildConfig.DEBUG`),
 *   nunca em release.
 */
class AlunoRepository(
    private val functions: FirebaseFunctions = Firebase.functions("us-central1"),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "AlunoRepository"
        private const val CODIGO_ALUNO_LENGTH = 6
        private const val MAX_TENTATIVAS_REDE = 3
        private const val BACKOFF_INICIAL_MS = 500L
    }

    suspend fun consultarPorCodigo(codigoAlunoBruto: String): ConsultaResult {
        if (auth.currentUser == null) {
            return ConsultaResult.Erro(
                ConsultaErro.NAO_AUTENTICADO,
                "Faça login com sua conta Google para consultar as notas."
            )
        }

        val codigo = codigoAlunoBruto.replace("-", "").replace(" ", "").uppercase()

        if (codigo.length != CODIGO_ALUNO_LENGTH) {
            return ConsultaResult.Erro(
                ConsultaErro.CODIGO_INVALIDO,
                "O código deve ter 6 caracteres no formato XXX-XXX."
            )
        }

        return withContext(Dispatchers.IO) {
            consultarComRetry(codigo)
        }
    }

    private suspend fun consultarComRetry(codigo: String): ConsultaResult {
        var backoffMs = BACKOFF_INICIAL_MS

        repeat(MAX_TENTATIVAS_REDE) { tentativa ->
            val resultado = executarConsulta(codigo)

            val deveTentarDeNovo = resultado is ConsultaResult.Erro &&
                resultado.tipo == ConsultaErro.SEM_REDE &&
                tentativa < MAX_TENTATIVAS_REDE - 1

            if (!deveTentarDeNovo) return resultado

            delay(backoffMs)
            backoffMs *= 2
        }

        // Inatingível na prática — o repeat sempre retorna dentro do loop.
        return executarConsulta(codigo)
    }

    private suspend fun executarConsulta(codigo: String): ConsultaResult {
        return try {
            val payload = hashMapOf("codigoAluno" to codigo)
            val resultado = functions
                .getHttpsCallable("consultarNotasPorCodigo")
                .call(payload)
                .await()

            val resposta = resultado.getData() as? Map<*, *>
                ?: return ConsultaResult.Erro(ConsultaErro.FALHA_SERVIDOR, "Resposta inesperada do servidor.")

            ConsultaResult.Sucesso(resposta.paraDadosConsultaAluno())
        } catch (e: Exception) {
            e.paraConsultaResultErro()
        }
    }

    private fun logAvisoDebug(mensagem: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, mensagem)
        }
    }

    private fun Map<*, *>.paraDadosConsultaAluno(): DadosConsultaAluno {
        val disciplinasRaw = this["disciplinas"]
        val disciplinasLista = disciplinasRaw as? List<*> ?: run {
            logAvisoDebug("Campo 'disciplinas' veio com tipo inesperado: ${disciplinasRaw?.javaClass}")
            emptyList<Any?>()
        }

        val disciplinas = disciplinasLista.mapIndexedNotNull { index, item ->
            val discMap = item as? Map<*, *>
            if (discMap == null) {
                logAvisoDebug("Item de 'disciplinas' no índice $index não é um Map: ${item?.javaClass}")
                return@mapIndexedNotNull null
            }
            discMap.paraDisciplina(index)
        }

        if (disciplinasRaw != null && disciplinas.isEmpty() && disciplinasLista.isNotEmpty()) {
            logAvisoDebug("Nenhuma disciplina foi parseada com sucesso apesar de ${disciplinasLista.size} itens recebidos.")
        }

        return DadosConsultaAluno(
            alunoNome = this["alunoNome"] as? String ?: "",
            alunoNumero = (this["alunoNumero"] as? Number)?.toInt() ?: 0,
            turmaNome = this["turmaNome"] as? String ?: "",
            classeNome = this["classeNome"] as? String ?: "",
            disciplinas = disciplinas
        )
    }

    private fun Map<*, *>.paraDisciplina(index: Int): DisciplinaComNotas {
        val notasRaw = this["notas"]
        val notasMap = notasRaw as? Map<*, *> ?: run {
            logAvisoDebug("Campo 'notas' da disciplina $index veio com tipo inesperado: ${notasRaw?.javaClass}")
            emptyMap<Any?, Any?>()
        }

        // Converte chave e valor individualmente em vez de castar o Map inteiro:
        // o SDK do Firebase Functions desserializa JSON aninhado como
        // Map<String, Any>, nunca Map<String, String> — um cast direto
        // (as? Map<String, String>) não falha em runtime por apagamento de
        // tipo genérico, mas também não converte os valores, então os dados
        // somem silenciosamente na hora de exibir.
        val notas: Map<String, String> = notasMap.entries
            .mapNotNull { (chave, valor) ->
                val chaveStr = chave as? String ?: return@mapNotNull null
                val valorStr = valor?.toString() ?: return@mapNotNull null
                chaveStr to valorStr
            }
            .toMap()

        return DisciplinaComNotas(
            disciplinaId = index,
            nomeDisciplina = this["nomeDisciplina"] as? String ?: "",
            professor = this["professor"] as? String ?: "—",
            notas = notas
        )
    }

    /**
     * Mapeia a exceção para um erro tipado com mensagem fixa por caso.
     * Só a mensagem devolvida pelo próprio backend (FirebaseFunctionsException.message)
     * é exibida ao usuário para os casos de negócio (não encontrado, limite
     * excedido, argumento inválido) — nunca o código do aluno é incluído
     * na mensagem, pois ele nunca é ecoado pela Cloud Function nem
     * concatenado aqui.
     */
    private fun Exception.paraConsultaResultErro(): ConsultaResult.Erro {
        if (this is FirebaseFunctionsException) {
            return when (code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    ConsultaResult.Erro(ConsultaErro.NAO_AUTENTICADO, "É necessário fazer login para continuar.")
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    ConsultaResult.Erro(
                        ConsultaErro.LIMITE_EXCEDIDO,
                        message ?: "Muitas tentativas. Aguarde antes de tentar novamente."
                    )
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    ConsultaResult.Erro(
                        ConsultaErro.NAO_ENCONTRADO,
                        message ?: "Nenhum aluno encontrado com este código."
                    )
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    ConsultaResult.Erro(ConsultaErro.CODIGO_INVALIDO, message ?: "Dados inválidos.")
                FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    ConsultaResult.Erro(ConsultaErro.SEM_REDE, "Sem conexão com o servidor. Tentando de novo…")
                else ->
                    ConsultaResult.Erro(ConsultaErro.FALHA_SERVIDOR, message ?: "Erro desconhecido no servidor.")
            }
        }
        if (this is FirebaseNetworkException || this is IOException) {
            return ConsultaResult.Erro(ConsultaErro.SEM_REDE, "Sem conexão com a internet.")
        }
        return ConsultaResult.Erro(ConsultaErro.FALHA_SERVIDOR, "Erro ao consultar dados.")
    }
}
