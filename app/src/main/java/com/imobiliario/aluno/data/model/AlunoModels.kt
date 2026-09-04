package com.imobiliario.aluno.data.model

data class DisciplinaComNotas(
    val disciplinaId: Int,
    val nomeDisciplina: String,
    val professor: String,
    val notas: Map<String, String>
) {
    /** Média simples das 3 notas trimestrais (MT), na escala 0–20. */
    val mediaFinal: Float?
        get() {
            val medias = listOfNotNull(
                notas["campo_5"]?.replace(",", ".")?.toFloatOrNull(),
                notas["campo_11"]?.replace(",", ".")?.toFloatOrNull(),
                notas["campo_17"]?.replace(",", ".")?.toFloatOrNull()
            )
            return if (medias.isNotEmpty()) medias.sum() / medias.size else null
        }

    val aprovado: Boolean
        get() = (mediaFinal ?: 0f) >= 10f
}

data class DadosConsultaAluno(
    val alunoNome: String,
    val alunoNumero: Int,
    val turmaNome: String,
    val classeNome: String,
    val disciplinas: List<DisciplinaComNotas>
)
