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
const servicosOpcoesEl = document.getElementById("servicos-opcoes");
const horarioSelect = document.getElementById("horario");
const formNovoAgendamento = document.getElementById("form-novo-agendamento");
const profileNameEl = document.getElementById("profile-name");
const barbeiroAgendaEl = document.getElementById("barbeiro-agenda");
const modalEdicaoEl = document.getElementById("modal-edicao");
const formEdicaoEl = document.getElementById("form-edicao-agendamento");
const edicaoClienteNomeEl = document.getElementById("edicao-cliente-nome");
const edicaoServicosOpcoesEl = document.getElementById(
  "edicao-servicos-opcoes",
);
const edicaoDataHoraEl = document.getElementById("edicao-data-hora");
let agendamentoEmEdicao = null;
const modalConclusaoEl = document.getElementById("modal-conclusao");
const formConclusaoEl = document.getElementById("form-conclusao");
const conclusaoFormaPagamentoEl = document.getElementById(
  "conclusao-forma-pagamento",
);
const conclusaoFeedbackEl = document.getElementById("conclusao-feedback");
const botaoConfirmarConclusaoEl = document.getElementById(
  "btn-confirmar-conclusao",
);
let alteracaoStatusPendente = null;

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
    profileNameEl.textContent =
      usuarioLogado.nomeBarbearia || usuarioLogado.nome;
  }
}

function formatarData(dia, mes, ano) {
  const dd = String(dia).padStart(2, "0");
  const mm = String(mes + 1).padStart(2, "0");
  return `${dd}/${mm}/${ano}`;
}

// Mostra o telefone (só dígitos no banco) no formato (61) 99999-9999
function formatarTelefone(valor) {
  const d = String(valor || "").replace(/\D/g, "");
  if (d.length === 11)
    return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10)
    return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return String(valor || "");
}

function ehDiaAnterior(dia, mes, ano) {
  const dataSelecionada = new Date(ano, mes, dia);
  const inicioDeHoje = new Date();
  inicioDeHoje.setHours(0, 0, 0, 0);
  return dataSelecionada < inicioDeHoje;
}

function atualizarDisponibilidadeDoFormulario() {
  const diaAnterior = ehDiaAnterior(diaSelecionado, mesAtual, anoAtual);
  const todasAgendas = barbeiroAgendaEl.value === "todos";
  const controles = formNovoAgendamento.querySelectorAll(
    "input, select, button",
  );

  controles.forEach((controle) => {
    controle.disabled = diaAnterior || todasAgendas;
  });

  formNovoAgendamento.classList.toggle("dia-anterior", diaAnterior);
  formNovoAgendamento.classList.toggle("todas-agendas", todasAgendas);
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

function renderizarOpcoesServicos(container, name, selecionados = []) {
  const idsSelecionados = new Set(selecionados.map(String));
  container.innerHTML = servicosDisponiveis
    .map(
      (servico) => `
    <label class="servico-opcao">
      <input type="checkbox" name="${name}" value="${servico.id}" ${idsSelecionados.has(String(servico.id)) ? "checked" : ""} />
      <span>${escaparHtml(servico.nome)}</span>
    </label>
  `,
    )
    .join("");
}

function servicosDoAgendamento(agendamento) {
  if (agendamento.servicos?.length) return agendamento.servicos;
  return agendamento.servico ? [agendamento.servico] : [];
}

function servicosSelecionados(container, name) {
  return Array.from(
    container.querySelectorAll(`input[name="${name}"]:checked`),
  ).map((checkbox) => Number(checkbox.value));
}

async function carregarServicosEBarbeiros() {
  const respBarbeiros = await apiFetch("/api/barbeiros");
  if (!respBarbeiros || !respBarbeiros.ok) {
    barbeiroAgendaEl.innerHTML =
      '<option value="">Não foi possível carregar os barbeiros</option>';
    return;
  }
  barbeirosDisponiveis = await respBarbeiros.json();
  barbeiroAgendaEl.innerHTML = [
    '<option value="todos">Todas as agendas</option>',
    ...barbeirosDisponiveis.map(
      (barbeiro) =>
        `<option value="${barbeiro.id}">${escaparHtml(barbeiro.nome)}</option>`,
    ),
  ].join("");
  if (!barbeirosDisponiveis.length) {
    barbeiroAgendaEl.innerHTML =
      '<option value="todos">Todas as agendas</option>';
  }

  const respServicos = await apiFetch("/api/servicos");

  if (!respServicos || !respServicos.ok) return;

  servicosDisponiveis = await respServicos.json();

  renderizarOpcoesServicos(servicosOpcoesEl, "novo-servico");
}

async function carregarAgendamentosDoMes() {
  const query =
    barbeiroAgendaEl.value === "todos"
      ? ""
      : `?barbeiroId=${encodeURIComponent(barbeiroAgendaEl.value)}`;
  const resp = await apiFetch(`/api/agendamentos${query}`);
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

    const diaAnterior = ehDiaAnterior(dia, mesAtual, anoAtual);
    if (diaAnterior) cell.classList.add("past");

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
  atualizarDisponibilidadeDoFormulario();
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
      const servico =
        servicosDoAgendamento(a)
          .map((item) => escaparHtml(item.nome))
          .join(", ") || "Serviço não informado";
      const barbeiro =
        barbeiroAgendaEl.value === "todos" && a.barbeiro
          ? `<div class="agenda-item-detalhes">Barbeiro: ${escaparHtml(a.barbeiro.nome)}</div>`
          : "";
      const telefone = a.clienteTelefone
        ? ` • ${escaparHtml(formatarTelefone(a.clienteTelefone))}`
        : "";
      return `
        <div class="agenda-item">
          <div class="agenda-item-conteudo">
            <div><span class="horario">${hora}</span><span class="agenda-item-nome">${escaparHtml(a.clienteNome)}</span></div>
            ${barbeiro}
            <div class="agenda-item-detalhes">${escaparHtml(servico)}${telefone}</div>
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
    select.addEventListener("change", () => {
      const agendamento = agendamentosDoMes.find(
        (item) => String(item.id) === String(select.dataset.statusId),
      );
      const statusAnterior = agendamento?.status || "AGENDADO";
      if (select.value === "CONCLUIDO" && statusAnterior !== "CONCLUIDO") {
        abrirModalConclusao(select, statusAnterior);
        return;
      }
      alterarStatus(
        select.dataset.statusId,
        select.value,
        statusAnterior,
        select,
      );
    });
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

function preencherOpcoesEdicao(selecionados) {
  renderizarOpcoesServicos(
    edicaoServicosOpcoesEl,
    "edicao-servico",
    selecionados,
  );
}

function abrirEdicao(id) {
  agendamentoEmEdicao = agendamentosDoMes.find(
    (a) => String(a.id) === String(id),
  );
  if (!agendamentoEmEdicao) return;

  preencherOpcoesEdicao(
    servicosDoAgendamento(agendamentoEmEdicao).map((servico) => servico.id),
  );
  edicaoClienteNomeEl.value = agendamentoEmEdicao.clienteNome || "";
  edicaoDataHoraEl.value = agendamentoEmEdicao.dataHora.slice(0, 16);
  modalEdicaoEl.style.display = "flex";
}

function fecharEdicao() {
  modalEdicaoEl.style.display = "none";
  agendamentoEmEdicao = null;
}

function abrirModalConclusao(select, statusAnterior) {
  alteracaoStatusPendente = {
    id: select.dataset.statusId,
    select,
    statusAnterior,
  };
  conclusaoFeedbackEl.textContent = "";
  conclusaoFormaPagamentoEl.value = "";
  modalConclusaoEl.style.display = "flex";
  conclusaoFormaPagamentoEl.focus();
}

function fecharModalConclusao(restaurarStatus = true) {
  if (restaurarStatus && alteracaoStatusPendente) {
    alteracaoStatusPendente.select.value =
      alteracaoStatusPendente.statusAnterior;
  }
  modalConclusaoEl.style.display = "none";
  alteracaoStatusPendente = null;
  formConclusaoEl.reset();
  conclusaoFeedbackEl.textContent = "";
}

async function alterarStatus(
  id,
  status,
  statusAnterior,
  select,
  formaPagamento = null,
) {
  const resp = await apiFetch(`/api/agendamentos/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, formaPagamento }),
  });
  if (!resp || !resp.ok) {
    if (select) select.value = statusAnterior;
    let mensagem = "Não foi possível alterar o status.";
    if (resp) {
      try {
        const erro = await resp.json();
        mensagem = erro.erro || mensagem;
      } catch {
        mensagem = "Não foi possível alterar o status.";
      }
    }
    alert(mensagem);
    return;
  }
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
}

formConclusaoEl.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!alteracaoStatusPendente || !formConclusaoEl.reportValidity()) return;

  botaoConfirmarConclusaoEl.disabled = true;
  botaoConfirmarConclusaoEl.textContent = "Registrando...";
  try {
    const pendente = alteracaoStatusPendente;
    await alterarStatus(
      pendente.id,
      "CONCLUIDO",
      pendente.statusAnterior,
      pendente.select,
      conclusaoFormaPagamentoEl.value,
    );
    if (
      agendamentosDoMes.find(
        (item) =>
          String(item.id) === String(pendente.id) &&
          item.status === "CONCLUIDO",
      )
    ) {
      fecharModalConclusao(false);
    }
  } finally {
    botaoConfirmarConclusaoEl.disabled = false;
    botaoConfirmarConclusaoEl.textContent = "Concluir atendimento";
  }
});

document
  .getElementById("btn-cancelar-conclusao")
  .addEventListener("click", () => {
    fecharModalConclusao();
  });
modalConclusaoEl.addEventListener("click", (event) => {
  if (event.target === modalConclusaoEl) fecharModalConclusao();
});

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

  const dataEdicao = new Date(edicaoDataHoraEl.value);
  const servicoIds = servicosSelecionados(
    edicaoServicosOpcoesEl,
    "edicao-servico",
  );
  if (!servicoIds.length) {
    alert("Selecione ao menos um serviço.");
    return;
  }
  if (
    ehDiaAnterior(
      dataEdicao.getDate(),
      dataEdicao.getMonth(),
      dataEdicao.getFullYear(),
    )
  ) {
    alert("Não é possível agendar em um dia que já passou.");
    return;
  }

  const resp = await apiFetch(`/api/agendamentos/${agendamentoEmEdicao.id}`, {
    method: "PUT",
    body: JSON.stringify({
      servico: { id: servicoIds[0] },
      servicos: servicoIds.map((id) => ({ id })),
      clienteNome: edicaoClienteNomeEl.value.trim(),
      // manda o telefone de volta, senão o backend o apaga ao editar
      clienteTelefone: agendamentoEmEdicao.clienteTelefone,
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
  const servicoIds = servicosSelecionados(servicosOpcoesEl, "novo-servico");
  const horario = horarioSelect.value;
  const barbeiroId = barbeiroAgendaEl.value;
  if (!servicoIds.length) {
    alert("Selecione ao menos um serviço.");
    return;
  }
  if (!barbeiroId || barbeiroId === "todos") {
    alert("Selecione um barbeiro específico para criar um agendamento.");
    return;
  }

  const dataHora = `${anoAtual}-${String(mesAtual + 1).padStart(2, "0")}-${String(diaSelecionado).padStart(2, "0")}T${horario}:00`;

  if (ehDiaAnterior(diaSelecionado, mesAtual, anoAtual)) {
    alert("Não é possível agendar em um dia que já passou.");
    return;
  }

  const resp = await apiFetch("/api/agendamentos", {
    method: "POST",
    body: JSON.stringify({
      barbeiro: { id: Number(barbeiroId) },
      servico: { id: servicoIds[0] },
      servicos: servicoIds.map((id) => ({ id })),
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

barbeiroAgendaEl.addEventListener("change", async () => {
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
});

iniciar();
