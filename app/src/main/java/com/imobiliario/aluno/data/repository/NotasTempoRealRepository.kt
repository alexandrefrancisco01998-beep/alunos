package com.imobiliario.aluno.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotasTempoRealRepository {

    private val database = FirebaseDatabase.getInstance()

    private var listener: ValueEventListener? = null
    private var referenciaAtual: DatabaseReference? = null

    fun observar(
        uid: String,
        codigoAluno: String,
        onNotasAtualizadas: (Map<String, Map<String, String>>) -> Unit,
        onErro: (String) -> Unit = {}
    ) {
        parar()

        if (uid.isBlank() || codigoAluno.isBlank()) return

        val codigoLimpo = codigoAluno
            .replace("-", "")
            .uppercase()

        val ref = database
            .getReference("notas_tempo_real")
            .child(uid)
            .child(codigoLimpo)

        referenciaAtual = ref

        listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val resultado = mutableMapOf<String, Map<String, String>>()

                for (disciplinaSnapshot in snapshot.children) {
                    val codigoDisciplina = disciplinaSnapshot.key ?: continue

                    val notasSnapshot =
                        disciplinaSnapshot.child("notas")

                    val notas = mutableMapOf<String, String>()

                    for (notaSnapshot in notasSnapshot.children) {
                        val campo = notaSnapshot.key ?: continue
                        val valor = notaSnapshot.getValue(String::class.java)
                            ?: notaSnapshot.value?.toString()
                            ?: continue

                        notas[campo] = valor
                    }

                    resultado[codigoDisciplina] = notas
                }

                onNotasAtualizadas(resultado)
            }

            override fun onCancelled(error: DatabaseError) {
                onErro(error.message)
            }
        }

        ref.addValueEventListener(listener!!)
    }

    fun parar() {
        val atual = listener
        val ref = referenciaAtual

        if (atual != null && ref != null) {
            ref.removeEventListener(atual)
        }

        listener = null
        referenciaAtual = null
    }
}
