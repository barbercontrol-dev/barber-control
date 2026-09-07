const MESES = [
  "Janeiro",
  "Fevereiro",
  "Março",
  "Abril",
  "Maio",
  "Junho",
  "Julho",
  "Agosto",
  "Setembro",
  "Outubro",
  "Novembro",
  "Dezembro",
];
const DIAS_SEMANA = ["DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB"];

const hoje = new Date();
let anoAtual = hoje.getFullYear();
let mesAtual = hoje.getMonth(); // 0-indexado
let diaSelecionado = hoje.getDate();

let agendamentosDoMes = [];
let servicosDisponiveis = [];
let barbeirosDisponiveis = [];

const calendarioEl = document.getElementById("calendario");
const mesTituloEl = document.getElementById("mes-titulo");
const totalMesEl = document.getElementById("total-mes");
const diaSelecionadoLabelEl = document.getElementById("dia-selecionado-label");
const agendaDiaTituloEl = document.getElementById("agenda-dia-titulo");
const agendaListaEl = document.getElementById("agenda-lista");
const servicoSelect = document.getElementById("servico");
const horarioSelect = document.getElementById("horario");
const formNovoAgendamento = document.getElementById("form-novo-agendamento");
const profileNameEl = document.getElementById("profile-name");
const modalEdicaoEl = document.getElementById("modal-edicao");
const formEdicaoEl = document.getElementById("form-edicao-agendamento");
const edicaoClienteNomeEl = document.getElementById("edicao-cliente-nome");
const edicaoServicoEl = document.getElementById("edicao-servico");
const edicaoBarbeiroEl = document.getElementById("edicao-barbeiro");
const edicaoDataHoraEl = document.getElementById("edicao-data-hora");
let agendamentoEmEdicao = null;

// ===== Identificar quem está logado, para mostrar o nome na sidebar =====
function decodeJwtPayload(token) {
  try {
    const payloadBase64 = token.split(".")[1];
    const payloadJson = atob(
      payloadBase64.replace(/-/g, "+").replace(/_/g, "/"),
    );
    return JSON.parse(payloadJson);
  } catch (e) {
    return null;
  }
}

async function identificarUsuarioLogado() {
  const token = localStorage.getItem("token");
  if (!token) return;

  const payload = decodeJwtPayload(token);
  if (!payload || !payload.sub) return;

  const resp = await apiFetch("/api/usuarios");
  if (!resp) return;

  const usuarios = await resp.json();
  const usuarioLogado = usuarios.find((u) => u.email === payload.sub);

  if (usuarioLogado && profileNameEl) {
    profileNameEl.textContent = usuarioLogado.nome;
  }
}

function formatarData(dia, mes, ano) {
  const dd = String(dia).padStart(2, "0");
  const mm = String(mes + 1).padStart(2, "0");
  return `${dd}/${mm}/${ano}`;
}

function gerarHorarios() {
  horarioSelect.innerHTML = "";
  for (let h = 8; h <= 19; h++) {
    for (const m of ["00", "30"]) {
      const valor = `${String(h).padStart(2, "0")}:${m}`;
      const opt = document.createElement("option");
      opt.value = valor;
      opt.textContent = valor;
      horarioSelect.appendChild(opt);
    }
  }
}

async function carregarServicosEBarbeiros() {
  const [respServicos, respBarbeiros] = await Promise.all([
    apiFetch("/api/servicos"),
    apiFetch("/api/usuarios"),
  ]);

  if (!respServicos || !respBarbeiros) return;

  servicosDisponiveis = await respServicos.json();
  barbeirosDisponiveis = await respBarbeiros.json();

  servicoSelect.innerHTML = servicosDisponiveis
    .map((s) => `<option value="${s.id}">${s.nome}</option>`)
    .join("");
}

async function carregarAgendamentosDoMes() {
  const resp = await apiFetch("/api/agendamentos");
  if (!resp) return;

  const todos = await resp.json();

  agendamentosDoMes = todos.filter((a) => {
    const data = new Date(a.dataHora);
    return data.getFullYear() === anoAtual && data.getMonth() === mesAtual;
  });

  totalMesEl.textContent = `${agendamentosDoMes.length} agendamento${agendamentosDoMes.length === 1 ? "" : "s"} no mês`;
}

function renderizarCalendario() {
  mesTituloEl.textContent = `${MESES[mesAtual]} ${anoAtual}`;
  calendarioEl.innerHTML = "";

  DIAS_SEMANA.forEach((d) => {
    const el = document.createElement("div");
    el.className = "day-name";
    el.textContent = d;
    calendarioEl.appendChild(el);
  });

  const primeiroDiaSemana = new Date(anoAtual, mesAtual, 1).getDay();
  const totalDias = new Date(anoAtual, mesAtual + 1, 0).getDate();

  for (let i = 0; i < primeiroDiaSemana; i++) {
    const vazio = document.createElement("div");
    vazio.className = "day-cell empty";
    calendarioEl.appendChild(vazio);
  }

  const contagemPorDia = new Map();
  agendamentosDoMes.forEach((a) => {
    const dia = new Date(a.dataHora).getDate();
    contagemPorDia.set(dia, (contagemPorDia.get(dia) || 0) + 1);
  });

  for (let dia = 1; dia <= totalDias; dia++) {
    const cell = document.createElement("div");
    cell.className = "day-cell";
    cell.textContent = dia;

    if (dia === diaSelecionado) cell.classList.add("selected");

    const quantidade = contagemPorDia.get(dia);
    if (quantidade) {
      const badge = document.createElement("span");
      badge.className = "day-badge";
      badge.textContent = quantidade;
      cell.appendChild(badge);
    }

    cell.addEventListener("click", () => selecionarDia(dia));
    calendarioEl.appendChild(cell);
  }
}

function selecionarDia(dia) {
  diaSelecionado = dia;
  renderizarCalendario();
  atualizarPainelDoDia();
}

function atualizarPainelDoDia() {
  const dataFormatada = formatarData(diaSelecionado, mesAtual, anoAtual);
  diaSelecionadoLabelEl.textContent = `Dia ${dataFormatada}`;
  agendaDiaTituloEl.textContent = `Agenda do dia ${String(diaSelecionado).padStart(2, "0")}/${String(mesAtual + 1).padStart(2, "0")}`;

  const doDia = agendamentosDoMes
    .filter((a) => new Date(a.dataHora).getDate() === diaSelecionado)
    .sort((a, b) => new Date(a.dataHora) - new Date(b.dataHora));

  if (doDia.length === 0) {
    agendaListaEl.innerHTML = `<p class="agenda-vazia">Nenhum horário agendado para este dia.</p>`;
    return;
  }

  agendaListaEl.innerHTML = doDia
    .map((a) => {
      const hora = new Date(a.dataHora).toLocaleTimeString("pt-BR", {
        hour: "2-digit",
        minute: "2-digit",
      });
      const status = a.status || "AGENDADO";
      const servico = a.servico ? a.servico.nome : "Serviço não informado";
      return `
        <div class="agenda-item">
          <div class="agenda-item-conteudo">
            <div><span class="horario">${hora}</span><span class="agenda-item-nome">${a.clienteNome}</span></div>
            <div class="agenda-item-detalhes">${servico}</div>
          </div>
          <div class="agenda-item-acoes">
            <select class="status-select" data-status-id="${a.id}" aria-label="Status do agendamento">
              ${["AGENDADO", "CONCLUIDO", "CANCELADO"].map((opcao) => `<option value="${opcao}" ${status === opcao ? "selected" : ""}>${opcao}</option>`).join("")}
            </select>
            <button type="button" class="btn-acao-agendamento" data-editar-id="${a.id}">Editar</button>
            <button type="button" class="btn-acao-agendamento btn-excluir-agendamento" data-excluir-id="${a.id}">Excluir</button>
          </div>
        </div>
      `;
    })
    .join("");

  agendaListaEl.querySelectorAll("[data-status-id]").forEach((select) => {
    select.addEventListener("change", () =>
      alterarStatus(select.dataset.statusId, select.value),
    );
  });
  agendaListaEl.querySelectorAll("[data-editar-id]").forEach((botao) => {
    botao.addEventListener("click", () => abrirEdicao(botao.dataset.editarId));
  });
  agendaListaEl.querySelectorAll("[data-excluir-id]").forEach((botao) => {
    botao.addEventListener("click", () =>
      excluirAgendamento(botao.dataset.excluirId),
    );
  });
}

function preencherOpcoesEdicao() {
  edicaoServicoEl.innerHTML = servicosDisponiveis
    .map((s) => `<option value="${s.id}">${s.nome}</option>`)
    .join("");
  edicaoBarbeiroEl.innerHTML = barbeirosDisponiveis
    .map((b) => `<option value="${b.id}">${b.nome}</option>`)
    .join("");
}

function abrirEdicao(id) {
  agendamentoEmEdicao = agendamentosDoMes.find(
    (a) => String(a.id) === String(id),
  );
  if (!agendamentoEmEdicao) return;

  preencherOpcoesEdicao();
  edicaoClienteNomeEl.value = agendamentoEmEdicao.clienteNome || "";
  edicaoServicoEl.value = agendamentoEmEdicao.servico?.id || "";
  edicaoBarbeiroEl.value = agendamentoEmEdicao.barbeiro?.id || "";
  edicaoDataHoraEl.value = agendamentoEmEdicao.dataHora.slice(0, 16);
  modalEdicaoEl.style.display = "flex";
}

function fecharEdicao() {
  modalEdicaoEl.style.display = "none";
  agendamentoEmEdicao = null;
}

async function alterarStatus(id, status) {
  const resp = await apiFetch(`/api/agendamentos/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
  if (!resp || !resp.ok) {
    alert("Não foi possível alterar o status.");
    return;
  }
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
}

async function excluirAgendamento(id) {
  if (!confirm("Deseja realmente excluir este agendamento?")) return;

  const resp = await apiFetch(`/api/agendamentos/${id}`, { method: "DELETE" });
  if (!resp || !resp.ok) {
    alert("Não foi possível excluir o agendamento.");
    return;
  }
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
}

formEdicaoEl.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!agendamentoEmEdicao) return;

  const resp = await apiFetch(`/api/agendamentos/${agendamentoEmEdicao.id}`, {
    method: "PUT",
    body: JSON.stringify({
      barbeiro: { id: Number(edicaoBarbeiroEl.value) },
      servico: { id: Number(edicaoServicoEl.value) },
      clienteNome: edicaoClienteNomeEl.value.trim(),
      dataHora: edicaoDataHoraEl.value + ":00",
    }),
  });

  if (!resp || !resp.ok) {
    const erro = resp?.status === 409 ? await resp.json() : null;
    alert(erro?.erro || "Não foi possível atualizar o agendamento.");
    return;
  }

  fecharEdicao();
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
});

document
  .getElementById("btn-cancelar-edicao")
  .addEventListener("click", fecharEdicao);
modalEdicaoEl.addEventListener("click", (event) => {
  if (event.target === modalEdicaoEl) fecharEdicao();
});

formNovoAgendamento.addEventListener("submit", async (e) => {
  e.preventDefault();

  const clienteNome = document.getElementById("cliente-nome").value.trim();
  const servicoId = servicoSelect.value;
  const horario = horarioSelect.value;

  const dataHora = `${anoAtual}-${String(mesAtual + 1).padStart(2, "0")}-${String(diaSelecionado).padStart(2, "0")}T${horario}:00`;

  const resp = await apiFetch("/api/agendamentos", {
    method: "POST",
    body: JSON.stringify({
      servico: { id: Number(servicoId) },
      clienteNome,
      dataHora,
    }),
  });

  if (!resp) return;

  if (resp.status === 409 || resp.status === 400) {
    const erro = await resp.json();
    alert(erro.erro || "Não foi possível criar o agendamento.");
    return;
  }

  if (!resp.ok) {
    alert("Erro ao criar agendamento. Tente novamente.");
    return;
  }

  formNovoAgendamento.reset();
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
});

async function iniciar() {
  gerarHorarios();
  await identificarUsuarioLogado();
  await carregarServicosEBarbeiros();
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
}

iniciar();
