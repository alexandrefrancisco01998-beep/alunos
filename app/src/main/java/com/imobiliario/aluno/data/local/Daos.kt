package com.imobiliario.aluno.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilAlunoDao {

    @Query("SELECT * FROM perfil_aluno WHERE ativo = 1 LIMIT 1")
    suspend fun getPerfilAtivo(): PerfilAluno?

    /** Mesma consulta que [getPerfilAtivo], como Flow — usado pela Home para
     *  atualizar a tela instantaneamente quando o perfil ativo muda, sem
     *  precisar recarregar a tela manualmente. */
    @Query("SELECT * FROM perfil_aluno WHERE ativo = 1 LIMIT 1")
    fun observarPerfilAtivo(): Flow<PerfilAluno?>

    @Query("SELECT * FROM perfil_aluno WHERE codigoAluno = :codigo LIMIT 1")
    suspend fun getPerfilPorCodigo(codigo: String): PerfilAluno?

    /**
     * Todos os alunos já consultados neste aparelho, do mais recente para
     * o mais antigo — usado pelo seletor de alunos (trocar entre os já
     * salvos). Nenhum perfil é apagado ao adicionar um novo, então essa
     * lista só cresce com o tempo.
     */
    @Query("SELECT * FROM perfil_aluno ORDER BY dataUltimaAtualizacao DESC")
    fun listarTodosPerfis(): Flow<List<PerfilAluno>>

    @Query("UPDATE perfil_aluno SET ativo = 0")
    suspend fun desativarTodosPerfis()

    /**
     * Troca qual aluno está ativo (visível na Home) sem apagar nem
     * desativar dados de mais ninguém de forma destrutiva — os dois
     * passos rodam como uma única transação, então nunca existe um
     * instante em que dois alunos (ou nenhum) estejam marcados como
     * ativos ao mesmo tempo.
     */
    @Transaction
    suspend fun ativarPerfil(codigo: String) {
        desativarTodosPerfis()
        marcarComoAtivo(codigo)
    }

    @Query("UPDATE perfil_aluno SET ativo = 1 WHERE codigoAluno = :codigo")
    suspend fun marcarComoAtivo(codigo: String)

    /**
     * Apaga TODOS os perfis salvos localmente (nome, número, turma).
     * Diferente de [desativarTodosPerfis] — que só marca `ativo = 0` e
     * mantém os dados no Room para uma eventual "nova consulta" rápida —
     * este método remove os registros de fato. Usar apenas no logout
     * completo (`sairDaConta`), nunca em "nova consulta", pois nesse
     * fluxo o usuário continua na mesma conta.
     */
    @Query("DELETE FROM perfil_aluno")
    suspend fun apagarTodosPerfis()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirPerfil(perfil: PerfilAluno)
}

/**
 * DAO do cache de disciplinas/notas (ver [DisciplinaCache]). Permite que
 * a tela de Início leia o último resultado salvo instantaneamente, sem
 * depender do backend estar disponível a cada abertura do app.
 */
@Dao
interface DisciplinaDao {

    @Query("SELECT * FROM disciplinas_cache WHERE codigo_aluno = :codigo ORDER BY disciplina_id")
    suspend fun listarPorAluno(codigo: String): List<DisciplinaCache>

    @Query("DELETE FROM disciplinas_cache WHERE codigo_aluno = :codigo")
    suspend fun apagarPorAluno(codigo: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodas(disciplinas: List<DisciplinaCache>)

    /** Apaga todo o cache de disciplinas de todos os alunos (usado no logout). */
    @Query("DELETE FROM disciplinas_cache")
    suspend fun apagarTodas()

    /**
     * Substitui o cache de um aluno pelo resultado mais recente do
     * backend, como uma operação atômica — evita uma janela em que a
     * tela ficaria sem nenhuma disciplina salva entre o apagar e o
     * inserir.
     */
    @Transaction
    suspend fun substituir(codigo: String, disciplinas: List<DisciplinaCache>) {
        apagarPorAluno(codigo)
        inserirTodas(disciplinas)
    }
}

// NotificacaoDao foi removido: notificações não têm mais cópia local em
// Room (ver nota em Entities.kt). Tudo passa a vir do Firestore através
// de NotificacaoRepository.
