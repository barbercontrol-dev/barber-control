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
const novoAgendamentoDiaEl = document.getElementById("novo-agendamento-dia");
const agendaDiaTituloEl = document.getElementById("agenda-dia-titulo");
const agendaListaEl = document.getElementById("agenda-lista");
const servicoSelect = document.getElementById("servico");
const barbeiroSelect = document.getElementById("barbeiro");
const horarioSelect = document.getElementById("horario");
const formNovoAgendamento = document.getElementById("form-novo-agendamento");
const erroAgendamentoEl = document.getElementById("erro-agendamento");

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

  barbeiroSelect.innerHTML = barbeirosDisponiveis
    .map((b) => `<option value="${b.id}">${b.nome}</option>`)
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

  const diasComAgendamento = new Set(
    agendamentosDoMes.map((a) => new Date(a.dataHora).getDate()),
  );

  for (let dia = 1; dia <= totalDias; dia++) {
    const cell = document.createElement("div");
    cell.className = "day-cell";
    cell.textContent = dia;

    if (diasComAgendamento.has(dia)) cell.classList.add("has-agendamento");
    if (dia === diaSelecionado) cell.classList.add("selected");

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
  novoAgendamentoDiaEl.textContent = `Dia ${dataFormatada}`;
  agendaDiaTituloEl.textContent = `Agenda do dia ${String(diaSelecionado).padStart(2, "0")}/${String(mesAtual + 1).padStart(2, "0")}`;

  const doDia = agendamentosDoMes
    .filter((a) => new Date(a.dataHora).getDate() === diaSelecionado)
    .sort((a, b) => new Date(a.dataHora) - new Date(b.dataHora));

  if (doDia.length === 0) {
    agendaListaEl.innerHTML = `<p class="agenda-empty">Nenhum horário agendado para este dia.</p>`;
    return;
  }

  agendaListaEl.innerHTML = doDia
    .map((a) => {
      const hora = new Date(a.dataHora).toLocaleTimeString("pt-BR", {
        hour: "2-digit",
        minute: "2-digit",
      });
      return `
      <div class="agenda-item">
        <div><span class="horario">${hora}</span>${a.clienteNome}</div>
        <span class="status">${a.status}</span>
      </div>
    `;
    })
    .join("");
}

formNovoAgendamento.addEventListener("submit", async (e) => {
  e.preventDefault();
  erroAgendamentoEl.classList.remove("visible");

  const clienteNome = document.getElementById("cliente-nome").value.trim();
  const servicoId = servicoSelect.value;
  const barbeiroId = barbeiroSelect.value;
  const horario = horarioSelect.value;

  const dataHora = `${anoAtual}-${String(mesAtual + 1).padStart(2, "0")}-${String(diaSelecionado).padStart(2, "0")}T${horario}:00`;

  const resp = await apiFetch("/api/agendamentos", {
    method: "POST",
    body: JSON.stringify({
      barbeiro: { id: Number(barbeiroId) },
      servico: { id: Number(servicoId) },
      clienteNome,
      dataHora,
    }),
  });

  if (!resp) return;

  if (resp.status === 409 || resp.status === 400) {
    const erro = await resp.json();
    erroAgendamentoEl.textContent =
      erro.erro || "Não foi possível criar o agendamento.";
    erroAgendamentoEl.classList.add("visible");
    return;
  }

  if (!resp.ok) {
    erroAgendamentoEl.textContent =
      "Erro ao criar agendamento. Tente novamente.";
    erroAgendamentoEl.classList.add("visible");
    return;
  }

  formNovoAgendamento.reset();
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
});

async function iniciar() {
  gerarHorarios();
  await carregarServicosEBarbeiros();
  await carregarAgendamentosDoMes();
  renderizarCalendario();
  atualizarPainelDoDia();
}

iniciar();
