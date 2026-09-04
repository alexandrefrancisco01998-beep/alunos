package com.imobiliario.aluno.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_aluno")
data class PerfilAluno(
    @PrimaryKey val codigoAluno: String,
    val nomeAluno: String,
    val numeroAluno: Int,
    val turmaNome: String,
    val classeNome: String = "",
    val turmaId: Int = 0,
    val dataUltimaAtualizacao: Long = System.currentTimeMillis(),
    val ativo: Boolean = true
)

/**
 * Cache local das disciplinas/notas do último resultado bem-sucedido do
 * backend, por aluno. Permite que a tela de Início mostre algo
 * instantaneamente ao abrir o app (sem esperar rede) e continue
 * funcionando em modo offline com dados de verdade — antes desta
 * entidade só o nome/número/turma do aluno ficavam salvos, e o modo
 * offline sempre exibia a lista de disciplinas vazia.
 *
 * `notasJson` guarda o mapa de notas serializado como um objeto JSON
 * simples (chave -> valor, todos String), evitando depender de uma
 * lib de serialização nova só para isso.
 */
@Entity(
    tableName = "disciplinas_cache",
    indices = [Index(value = ["codigo_aluno"])]
)
data class DisciplinaCache(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "codigo_aluno") val codigoAluno: String,
    @ColumnInfo(name = "disciplina_id") val disciplinaId: Int,
    @ColumnInfo(name = "nome_disciplina") val nomeDisciplina: String,
    val professor: String,
    @ColumnInfo(name = "notas_json") val notasJson: String
)

// Notificações NÃO têm mais cópia local em Room — a fonte única da
// verdade é o Firestore (coleção `notificacoesAluno`, ver
// com.imobiliario.aluno.ui.notificacoes.NotificacaoNota e
// data.repository.NotificacaoRepository). A entidade/tabela local que
// existia aqui foi removida porque nada na tela chegava a lê-la: o
// serviço de push e o PerfilViewModel gravavam neste Room, enquanto a
// UI sempre leu do Firestore — duas fontes desencontradas que só
// geravam confusão (contador de não lidas incoerente com a lista).
