package com.imobiliario.aluno.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.imobiliario.aluno.ui.notificacoes.NotificacaoNota
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Fonte da verdade das notificações: coleção top-level `notificacoesAluno`
 * (não mais subcoleção de users/{uid}) — mais leve para carregar e mostrar,
 * já que não depende de abrir o documento pai do usuário, e permite índices
 * e queries dedicados só para notificação.
 *
 * Cada doc carrega o campo `uid` do encarregado dono da notificação; a
 * query filtra por uid + codigoAluno.
 *
 * Sem cópia local em Room — sobrevive a reinstalação/troca de aparelho e
 * continua visível mesmo lida; só some se o usuário deletar (swipe
 * individual).
 *
 * Offline: a persistência local do Firestore é ativada uma única vez em
 * MeuFilhoApplication.onCreate(), antes de qualquer uso do Firestore no
 * app — por isso não é configurada aqui de novo. O listener em
 * observarPorAluno entrega os dados já sincronizados mesmo sem internet,
 * e marcarComoLidas/deletar enfileiram a escrita localmente e sincronizam
 * sozinhos quando a conexão voltar.
 */
class NotificacaoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun colecao() = firestore.collection("notificacoesAluno")

    /**
     * Observa em tempo real as notificações do aluno informado, mais
     * recentes primeiro. Emite lista vazia se não houver usuário logado.
     */
    fun observarPorAluno(codigoAluno: String): Flow<List<NotificacaoNota>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = colecao()
            .whereEqualTo("uid", uid)
            .whereEqualTo("codigoAluno", codigoAluno)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    // Falha de listener não deve derrubar a tela; apenas não atualiza.
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(NotificacaoNota::class.java) ?: emptyList()
                trySend(lista)
            }

        awaitClose { registration.remove() }
    }

    suspend fun marcarComoLida(notificacaoId: String) {
        // update() enfileira a escrita localmente se estiver offline e
        // sincroniza sozinho quando a conexão voltar — não precisa de
        // tratamento especial aqui.
        colecao().document(notificacaoId).update("lida", true).await()
    }

    suspend fun deletar(notificacaoId: String) {
        colecao().document(notificacaoId).delete().await()
    }

    /**
     * Marca como lidas os ids informados (não lidos daquele aluno).
     * Recebe os ids já conhecidos pela tela — que vêm do listener em tempo
     * real, portanto disponíveis mesmo offline — em vez de fazer uma nova
     * leitura de rede só para descobrir quais estão não lidos.
     */
    suspend fun marcarComoLidas(notificacaoIds: List<String>) {
        if (notificacaoIds.isEmpty()) return

        val batch = firestore.batch()
        for (id in notificacaoIds) {
            batch.update(colecao().document(id), "lida", true)
        }
        batch.commit().await()
        // commit() também enfileira offline e sincroniza depois sozinho.
    }
}
