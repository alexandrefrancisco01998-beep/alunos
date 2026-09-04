package com.imobiliario.aluno.data.local

import com.imobiliario.aluno.data.model.DisciplinaComNotas
import org.json.JSONObject

/**
 * Conversões entre o modelo de rede ([DisciplinaComNotas]) e a entidade
 * de cache local ([DisciplinaCache]). Mantidas num arquivo à parte para
 * não acoplar `data.model` (usado pelo repositório de rede) a `data.local`
 * (Room).
 */

fun List<DisciplinaComNotas>.paraCache(codigoAluno: String): List<DisciplinaCache> =
    map { disciplina ->
        DisciplinaCache(
            codigoAluno = codigoAluno,
            disciplinaId = disciplina.disciplinaId,
            nomeDisciplina = disciplina.nomeDisciplina,
            professor = disciplina.professor,
            notasJson = JSONObject(disciplina.notas as Map<*, *>).toString()
        )
    }

fun List<DisciplinaCache>.paraDisciplinas(): List<DisciplinaComNotas> =
    map { cache ->
        val json = runCatching { JSONObject(cache.notasJson) }.getOrNull()
        val notas: Map<String, String> = json?.keys()?.asSequence()
            ?.associateWith { chave -> json.optString(chave) }
            ?: emptyMap()

        DisciplinaComNotas(
            disciplinaId = cache.disciplinaId,
            nomeDisciplina = cache.nomeDisciplina,
            professor = cache.professor,
            notas = notas
        )
    }
