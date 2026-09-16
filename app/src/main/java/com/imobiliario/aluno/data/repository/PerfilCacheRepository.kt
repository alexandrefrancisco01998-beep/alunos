package com.imobiliario.aluno.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.imobiliario.aluno.data.model.DadosConsultaAluno
import com.imobiliario.aluno.data.model.DisciplinaComNotas
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Perfil salvo localmente (cache), equivalente ao antigo `PerfilAluno` do Room. */
data class PerfilSalvo(
    val codigoAluno: String,
    val nomeAluno: String,
    val numeroAluno: Int,
    val turmaNome: String,
    val classeNome: String = "",
    val dataUltimaAtualizacao: Long = System.currentTimeMillis(),
    val ativo: Boolean = true
)

/**
 * Cache de perfis e disciplinas/notas do aluno, guardado no Firebase
 * Realtime Database em vez de Room/SQLite.
 *
 * Path: `cache_alunos/{uid}/{codigoAluno}` — cada usuário (encarregado)
 * só enxerga o próprio cache, já que a leitura/escrita é sempre
 * escopada pelo uid do Firebase Auth atual.
 *
 * A persistência offline nativa do SDK do Realtime Database
 * (`FirebaseDatabase.setPersistenceEnabled(true)`, ativada em
 * [com.imobiliario.aluno.MeuFilhoApplication]) é o que garante que este
 * cache continue disponível sem rede — os `ValueEventListener` abaixo
 * disparam imediatamente com os dados do disco local antes de
 * sincronizar com o servidor, exatamente como um `SELECT` local do
 * Room fazia antes.
 *
 * `disciplinas` fica embutido dentro do próprio nó do perfil (não é uma
 * tabela separada como `disciplinas_cache` era no Room) porque no RTDB
 * não há JOIN — tudo que é lido junto deve ficar salvo junto.
 */
class PerfilCacheRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private fun raiz(uid: String) = database.getReference("cache_alunos").child(uid)

    /** Observa a lista de perfis salvos, do mais recente para o mais antigo. */
    fun observarTodosPerfis(uid: String): Flow<List<PerfilSalvo>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = raiz(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val perfis = snapshot.children.mapNotNull { it.paraPerfilSalvo() }
                    .sortedByDescending { it.dataUltimaAtualizacao }
                trySend(perfis)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Observa o perfil atualmente ativo (ou null se nenhum). */
    fun observarPerfilAtivo(uid: String): Flow<PerfilSalvo?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = raiz(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ativo = snapshot.children
                    .mapNotNull { it.paraPerfilSalvo() }
                    .firstOrNull { it.ativo }
                trySend(ativo)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getPerfil(uid: String, codigoAluno: String): PerfilSalvo? {
        if (uid.isBlank()) return null
        val snapshot = raiz(uid).child(codigoAluno).get().await()
        return snapshot.paraPerfilSalvo()
    }

    suspend fun getDisciplinas(uid: String, codigoAluno: String): List<DisciplinaComNotas> {
        if (uid.isBlank()) return emptyList()
        val snapshot = raiz(uid).child(codigoAluno).child("disciplinas").get().await()
        return snapshot.paraDisciplinas()
    }

    /**
     * Salva/atualiza o perfil e (opcionalmente) as disciplinas de um
     * aluno, sem apagar nem desativar nenhum outro perfil já salvo —
     * equivalente ao antigo `inserirPerfil` do Room.
     */
    suspend fun salvarPerfil(
        uid: String,
        perfil: PerfilSalvo,
        disciplinas: List<DisciplinaComNotas>? = null
    ) {
        if (uid.isBlank()) return
        val dados = mutableMapOf<String, Any?>(
            "codigoAluno" to perfil.codigoAluno,
            "nomeAluno" to perfil.nomeAluno,
            "numeroAluno" to perfil.numeroAluno,
            "turmaNome" to perfil.turmaNome,
            "classeNome" to perfil.classeNome,
            "dataUltimaAtualizacao" to perfil.dataUltimaAtualizacao,
            "ativo" to perfil.ativo
        )
        if (disciplinas != null) {
            dados["disciplinas"] = disciplinas.map { it.paraMapa() }
        }
        raiz(uid).child(perfil.codigoAluno).updateChildren(dados).await()
    }

    suspend fun substituirDisciplinas(
        uid: String,
        codigoAluno: String,
        disciplinas: List<DisciplinaComNotas>
    ) {
        if (uid.isBlank()) return
        raiz(uid).child(codigoAluno).child("disciplinas")
            .setValue(disciplinas.map { it.paraMapa() })
            .await()
    }

    /**
     * Troca qual aluno está ativo — desativa todos e ativa só o
     * indicado, como uma única atualização multi-path (equivalente à
     * transação `ativarPerfil` do Room DAO).
     */
    suspend fun ativarPerfil(uid: String, codigoAluno: String) {
        if (uid.isBlank()) return
        val snapshot = raiz(uid).get().await()
        val atualizacoes = mutableMapOf<String, Any?>()
        for (filho in snapshot.children) {
            val codigo = filho.key ?: continue
            atualizacoes["$codigo/ativo"] = codigo == codigoAluno
        }
        if (atualizacoes.isEmpty()) return
        raiz(uid).updateChildren(atualizacoes).await()
    }

    /** Apaga todo o cache local (perfis + disciplinas) deste usuário — usado no logout. */
    suspend fun apagarTudo(uid: String) {
        if (uid.isBlank()) return
        raiz(uid).removeValue().await()
    }

    private fun DataSnapshot.paraPerfilSalvo(): PerfilSalvo? {
        val codigo = child("codigoAluno").getValue(String::class.java)
            ?: key?.takeIf { exists() }
            ?: return null
        return PerfilSalvo(
            codigoAluno = codigo,
            nomeAluno = child("nomeAluno").getValue(String::class.java) ?: "",
            numeroAluno = child("numeroAluno").getValue(Int::class.java) ?: 0,
            turmaNome = child("turmaNome").getValue(String::class.java) ?: "",
            classeNome = child("classeNome").getValue(String::class.java) ?: "",
            dataUltimaAtualizacao = child("dataUltimaAtualizacao").getValue(Long::class.java)
                ?: System.currentTimeMillis(),
            ativo = child("ativo").getValue(Boolean::class.java) ?: false
        )
    }

    private fun DataSnapshot.paraDisciplinas(): List<DisciplinaComNotas> =
        children.mapIndexedNotNull { index, item -> item.paraDisciplina(index) }

    private fun DataSnapshot.paraDisciplina(indiceFallback: Int): DisciplinaComNotas {
        val notas = mutableMapOf<String, String>()
        for (notaSnapshot in child("notas").children) {
            val campo = notaSnapshot.key ?: continue
            val valor = notaSnapshot.getValue(String::class.java)
                ?: notaSnapshot.value?.toString()
                ?: continue
            notas[campo] = valor
        }
        return DisciplinaComNotas(
            disciplinaId = child("disciplinaId").getValue(Int::class.java) ?: indiceFallback,
            codigoUnicoDisciplina = child("codigoUnicoDisciplina").getValue(String::class.java) ?: "",
            nomeDisciplina = child("nomeDisciplina").getValue(String::class.java) ?: "",
            professor = child("professor").getValue(String::class.java) ?: "—",
            notas = notas
        )
    }

    private fun DisciplinaComNotas.paraMapa(): Map<String, Any?> = mapOf(
        "disciplinaId" to disciplinaId,
        "codigoUnicoDisciplina" to codigoUnicoDisciplina,
        "nomeDisciplina" to nomeDisciplina,
        "professor" to professor,
        "notas" to notas
    )
}

fun DadosConsultaAluno.paraPerfilSalvo(codigoAluno: String, ativo: Boolean): PerfilSalvo =
    PerfilSalvo(
        codigoAluno = codigoAluno,
        nomeAluno = alunoNome,
        numeroAluno = alunoNumero,
        turmaNome = turmaNome,
        classeNome = classeNome,
        dataUltimaAtualizacao = System.currentTimeMillis(),
        ativo = ativo
    )
