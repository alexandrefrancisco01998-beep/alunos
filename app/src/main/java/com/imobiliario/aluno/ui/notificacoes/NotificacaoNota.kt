package com.imobiliario.aluno.ui.notificacoes


import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


/**
 * Notificação de nota. Fonte da verdade é o Firestore, na coleção
 * top-level notificacoesAluno/{id} — não é mais subcoleção de users/{uid}
 * (mais leve para carregar/mostrar, sem depender do doc do usuário) e não
 * existe cópia local em Room. Continua visível mesmo depois de lida; só
 * some se o usuário deletar.
 *
 * O construtor sem-args e os vars são exigidos pelo Firestore para
 * desserialização automática via toObject().
 */
data class NotificacaoNota(
    @DocumentId
    val id: String = "",

    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("codigoAluno") @set:PropertyName("codigoAluno")
    var codigoAluno: String = "",

    @get:PropertyName("codigoTurma") @set:PropertyName("codigoTurma")
    var codigoTurma: String = "",

    @get:PropertyName("turmaNome") @set:PropertyName("turmaNome")
    var turmaNome: String = "",

    @get:PropertyName("disciplina") @set:PropertyName("disciplina")
    var disciplina: String = "",

    @get:PropertyName("professorNome") @set:PropertyName("professorNome")
    var professorNome: String = "",

    @get:PropertyName("tipo") @set:PropertyName("tipo")
    var tipo: TipoNotificacao = TipoNotificacao.NOTA_ALTERADA,

    @get:PropertyName("valorNota") @set:PropertyName("valorNota")
    var valorNota: String = "",

    @get:PropertyName("lida") @set:PropertyName("lida")
    var lida: Boolean = false,

    @ServerTimestamp
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Date? = null
) {
    /** Epoch millis para reaproveitar a UI existente (formatarDataHora espera Long). */
    val timestampMillis: Long
        get() = timestamp?.time ?: 0L
}
