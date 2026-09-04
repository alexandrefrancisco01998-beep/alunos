package com.imobiliario.aluno.ui.notificacoes

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class TipoNotificacao {
    NOTA_LANCADA,
    NOTA_ALTERADA,
    NOTA_DELETADA
}

/**
 * Notificação de nota. Fonte da verdade é o Firestore, em
 * users/{uid}/notificacoesAluno/{id} — não existe mais cópia local em Room.
 * Continua visível mesmo depois de lida; só some se o usuário deletar.
 *
 * O construtor sem-args e os vars são exigidos pelo Firestore para
 * desserialização automática via toObject().
 */