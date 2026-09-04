
import * as logger from "firebase-functions/logger";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { admin } from "../../shared/admin";

const REGION = "us-central1";

interface FilhoVinculado {
  codigoAluno: string;
  alunoNome: string;
  alunoNumero: number;
  turmaNome: string;
  vinculadoEm: string | null;
}

// ─────────────────────────────────────────────────────────────────
//  Lista os filhos já vinculados ao encarregado logado.
//  Usada na tela inicial de "Consultar Notas".
// ─────────────────────────────────────────────────────────────────

export const listarFilhosVinculados = onCall(
  { region: REGION },
  async (request): Promise<{ filhos: FilhoVinculado[] }> => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "É necessário fazer login para ver seus vínculos.");
    }

    const uid = request.auth.uid;
    const db  = admin.firestore();

    const snap = await db
      .collection("alunos_vinculados")
      .doc(uid)
      .collection("filhos")
      .orderBy("vinculadoEm", "desc")
      .get();

    const filhos: FilhoVinculado[] = snap.docs.map((doc) => {
      const data      = doc.data();
      const timestamp = data.vinculadoEm as FirebaseFirestore.Timestamp | undefined;
      return {
        codigoAluno:  (data.codigoAluno  as string) ?? doc.id,
        alunoNome:    (data.alunoNome    as string) ?? "",
        alunoNumero:  (data.alunoNumero  as number) ?? 0,
        turmaNome:    (data.turmaNome    as string) ?? "",
        vinculadoEm:  timestamp ? timestamp.toDate().toISOString() : null,
      };
    });

    return { filhos };
  }
);

// ─────────────────────────────────────────────────────────────────
//  Remove um vínculo (encarregado desvincula um aluno da lista).
// ─────────────────────────────────────────────────────────────────

export const removerVinculoFilho = onCall(
  { region: REGION },
  async (request): Promise<{ sucesso: boolean }> => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "É necessário fazer login.");
    }

    const uid         = request.auth.uid;
    const codigoAluno = (request.data?.codigoAluno as string) ?? "";
    const codigoLimpo = codigoAluno.replace(/-/g, "").replace(/\s/g, "").toUpperCase();

    if (codigoLimpo.length !== 6) {
      throw new HttpsError("invalid-argument", "Código inválido.");
    }

    const db = admin.firestore();

    await db
      .collection("alunos_vinculados")
      .doc(uid)
      .collection("filhos")
      .doc(codigoLimpo)
      .delete();

    logger.info("Vínculo removido", { uid, codigoLimpo });
    return { sucesso: true };
  }
);
