import * as logger from "firebase-functions/logger";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { admin } from "../../shared/admin";
import { enviarNotificacaoParaUsuario } from "../notificacoes/enviarPush";

const REGION           = "us-central1";
const LIMITE_TENTATIVAS = 10;
const JANELA_MINUTOS    = 60;

interface ConsultaRequest {
  codigoAluno: string;
}

interface DisciplinaComNotasResponse {
  nomeDisciplina: string;
  professor: string;
  notas: Record<string, string>;
}

interface ConsultaResponse {
  alunoNome: string;
  alunoNumero: number;
  turmaNome: string;
  disciplinas: DisciplinaComNotasResponse[];
}

function normalizarCodigo(codigo: string): string {
  return (codigo || "").replace(/-/g, "").replace(/\s/g, "").toUpperCase();
}

async function verificarRateLimit(db: FirebaseFirestore.Firestore, uid: string): Promise<void> {
  const ref = db.collection("rate_limits").doc(uid);

  await db.runTransaction(async (tx) => {
    const snap   = await tx.get(ref);
    const agora  = Date.now();
    const janelaMs = JANELA_MINUTOS * 60 * 1000;

    if (!snap.exists) {
      tx.set(ref, { tentativas: 1, inicioJanela: agora });
      return;
    }

    const dados       = snap.data()!;
    const inicioJanela = dados.inicioJanela as number;
    const tentativas   = dados.tentativas  as number;

    if (agora - inicioJanela > janelaMs) {
      tx.set(ref, { tentativas: 1, inicioJanela: agora });
      return;
    }

    if (tentativas >= LIMITE_TENTATIVAS) {
      throw new HttpsError(
        "resource-exhausted",
        `Muitas tentativas. Tente novamente em ${JANELA_MINUTOS} minutos.`
      );
    }

    tx.update(ref, { tentativas: tentativas + 1 });
  });
}

export const consultarNotasPorCodigo = onCall(
  { region: REGION },
  async (request): Promise<ConsultaResponse> => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "É necessário fazer login com sua conta Google para consultar notas."
      );
    }

    const uid        = request.auth.uid;
    const data       = request.data as ConsultaRequest;
    const codigoLimpo = normalizarCodigo(data?.codigoAluno);

    if (codigoLimpo.length !== 6) {
      throw new HttpsError(
        "invalid-argument",
        "O código do aluno deve ter 6 caracteres no formato XXX-XXX."
      );
    }

    const db = admin.firestore();
    await verificarRateLimit(db, uid);

    const indexSnap = await db.collection("alunos_codigo").doc(codigoLimpo).get();
    if (!indexSnap.exists) {
      logger.warn("Tentativa de consulta com código inexistente", { uid, codigoLimpo });
      throw new HttpsError("not-found", "Nenhum aluno encontrado com o código informado.");
    }

    const indexData    = indexSnap.data()!;
    const alunoNome    = (indexData.alunoNome  as string) ?? "";
    const alunoNumero  = (indexData.alunoNumero as number) ?? 0;
    const turmaNome    = (indexData.turmaNome  as string) ?? "";
    const codigoTurma  = (indexData.codigoTurma as string) ?? "";

    if (!alunoNome || !codigoTurma) {
      throw new HttpsError("data-loss", "Os dados do aluno estão incompletos no sistema.");
    }

    const codigoTurmaLimpo  = codigoTurma.replace(/-/g, "");
    const backupCodigoRef   = db.collection("backups_codigo").doc(codigoTurmaLimpo);
    const backupCodigoSnap  = await backupCodigoRef.get();
    const professorUid      = (backupCodigoSnap.data()?.userId as string) ?? null;

    const disciplinasSnap = await backupCodigoRef.collection("disciplinas").get();
    if (disciplinasSnap.empty) {
      throw new HttpsError("not-found", "As notas desta turma não estão disponíveis no momento.");
    }

    const disciplinasComNotas: DisciplinaComNotasResponse[] = [];

    for (const discDoc of disciplinasSnap.docs) {
      const discData       = discDoc.data();
      const nomeDisciplina = (discData.nome as string) ?? "";

      const alunosSnap = await discDoc.ref.collection("alunos").get();
      const alunoDoc   = alunosSnap.docs.find((doc) => {
        const codigoSalvo = (doc.data().codigoUnicoAluno as string) ?? "";
        return normalizarCodigo(codigoSalvo) === codigoLimpo;
      });

      if (!alunoDoc) continue;

      const alunoData = alunoDoc.data();
      const notasRaw  = (alunoData.notas as Record<string, unknown>) ?? {};
      const notas: Record<string, string> = {};

      for (const [campo, valor] of Object.entries(notasRaw)) {
        if (valor !== null && valor !== undefined && String(valor).length > 0) {
          notas[campo] = String(valor);
        }
      }

      disciplinasComNotas.push({
        nomeDisciplina,
        professor: (discData.professorNome as string) ?? "Não informado",
        notas,
      });
    }

    if (disciplinasComNotas.length === 0) {
      throw new HttpsError("not-found", "Este aluno não foi encontrado nas disciplinas da turma.");
    }

    // Salva (ou actualiza) o vínculo permanente do encarregado
    let primeiraVinculacao = false;
    try {
      const vinculoRef = db
        .collection("alunos_vinculados")
        .doc(uid)
        .collection("filhos")
        .doc(codigoLimpo);

      const vinculoExistenteSnap = await vinculoRef.get();
      primeiraVinculacao = !vinculoExistenteSnap.exists;

      await vinculoRef.set(
        {
          alunoNome,
          alunoNumero,
          turmaNome,
          codigoTurma,
          codigoAluno: codigoLimpo,
          vinculadoEm: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    } catch (err) {
      logger.error("Falha ao salvar vínculo (não bloqueante)", { uid, codigoLimpo, err });
    }

    // Notifica o professor apenas na primeira vinculação deste encarregado
    if (professorUid && primeiraVinculacao) {
      await enviarNotificacaoParaUsuario(
        professorUid,
        "Novo encarregado vinculado",
        `${alunoNome} (Nº ${alunoNumero}) foi consultado pela primeira vez por um encarregado.`,
        { tipo: "primeira_consulta", turmaNome, alunoNome }
      );
    }

    logger.info("Consulta de notas realizada", { uid, codigoLimpo, turmaNome });

    return { alunoNome, alunoNumero, turmaNome, disciplinas: disciplinasComNotas };
  }
);
