let barbeiroAtual = null;
let servicosDisponiveis = [];
let todosAgendamentos = [];
let chartInstance = null;

const modal = document.getElementById("add-modal");
const listaHojeEl = document.getElementById("appointments-list");
const totalAgendamentosEl = document.getElementById("total-agendamentos");
const clientesComparecidosEl = document.getElementById("clientes-comparecidos");
const rendaAtualEl = document.getElementById("renda-atual");
const saudacaoEl = document.getElementById("saudacao");
const profileNameEl = document.getElementById("profile-name");

// ===== Logout =====
document.getElementById("btn-logout").addEventListener("click", (e) => {
  e.preventDefault();
  localStorage.removeItem("token");
  window.location.href = "login.html";
});

// ===== Identificar quem está logado a partir do token =====
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

async function identificarBarbeiroLogado() {
  const token = localStorage.getItem("token");
  if (!token) return;

  const payload = decodeJwtPayload(token);
  if (!payload || !payload.sub) return;

  const resp = await apiFetch("/api/usuarios");
  if (!resp) return;

  const usuarios = await resp.json();
  barbeiroAtual = usuarios.find((u) => u.email === payload.sub) || null;

  if (barbeiroAtual) {
    profileNameEl.textContent = barbeiroAtual.nome;
    saudacaoEl.textContent = `Olá, ${barbeiroAtual.nome}!`;
  }
}

// ===== Helpers de data =====
function ehHoje(dataHoraStr) {
  const data = new Date(dataHoraStr);
  const agora = new Date();
  return (
    data.getFullYear() === agora.getFullYear() &&
    data.getMonth() === agora.getMonth() &&
    data.getDate() === agora.getDate()
  );
}

function meusAgendamentos(lista) {
  if (!barbeiroAtual) return lista;
  return lista.filter((a) => a.barbeiro && a.barbeiro.id === barbeiroAtual.id);
}

// ===== Cards de resumo =====
function atualizarCards() {
  const hojeDoBarbeiro = meusAgendamentos(todosAgendamentos).filter((a) =>
    ehHoje(a.dataHora),
  );

  totalAgendamentosEl.textContent = hojeDoBarbeiro.length;

  const comparecidos = hojeDoBarbeiro.filter((a) => a.status === "CONCLUIDO");
  clientesComparecidosEl.textContent = comparecidos.length;

  const renda = comparecidos.reduce(
    (soma, a) => soma + (a.servico?.valor || 0),
    0,
  );
  rendaAtualEl.textContent = renda.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

// ===== Lista "Agendamentos de hoje" =====
function renderizarListaHoje() {
  const hojeDoBarbeiro = meusAgendamentos(todosAgendamentos)
    .filter((a) => ehHoje(a.dataHora))
    .sort((a, b) => new Date(a.dataHora) - new Date(b.dataHora));

  if (hojeDoBarbeiro.length === 0) {
    listaHojeEl.innerHTML = `<p style="text-align:center;color:#999;font-size:13.5px;">Nenhum agendamento para hoje.</p>`;
    return;
  }

  listaHojeEl.innerHTML = hojeDoBarbeiro
    .map((a) => {
      const hora = new Date(a.dataHora).toLocaleTimeString("pt-BR", {
        hour: "2-digit",
        minute: "2-digit",
      });
      return `
      <div class="appointment-item">
        <span>${a.clienteNome}</span>
        <span class="appointment-time">${hora}h</span>
      </div>
    `;
    })
    .join("");
}

// ===== Gráfico anual =====
function renderizarGraficoAnual() {
  const anoAtual = new Date().getFullYear();
  const contagemPorMes = new Array(12).fill(0);

  meusAgendamentos(todosAgendamentos).forEach((a) => {
    const data = new Date(a.dataHora);
    if (data.getFullYear() === anoAtual) {
      contagemPorMes[data.getMonth()]++;
    }
  });

  const meses = [
    "Jan",
    "Fev",
    "Mar",
    "Abr",
    "Mai",
    "Jun",
    "Jul",
    "Ago",
    "Set",
    "Out",
    "Nov",
    "Dez",
  ];
  const ctx = document.getElementById("annualChart").getContext("2d");

  if (chartInstance) chartInstance.destroy();

  chartInstance = new Chart(ctx, {
    type: "line",
    data: {
      labels: meses,
      datasets: [
        {
          data: contagemPorMes,
          borderColor: "#1a1a1a",
          backgroundColor: "#ffc107",
          borderWidth: 2,
          pointBackgroundColor: "#ffc107",
          pointBorderColor: "#1a1a1a",
          pointRadius: 4,
          tension: 0.3,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { enabled: true },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1, color: "#888" },
          grid: { color: "#f0f0f0" },
        },
        x: {
          ticks: { color: "#666" },
          grid: { display: false },
        },
      },
    },
  });
}

// ===== Modal: novo agendamento rápido =====
function popularSelectServicos() {
  const select = document.getElementById("client-service");
  select.innerHTML = servicosDisponiveis
    .map((s) => `<option value="${s.id}">${s.nome}</option>`)
    .join("");
}

document.getElementById("open-modal-btn").addEventListener("click", () => {
  modal.style.display = "flex";
});

document.getElementById("close-modal-btn").addEventListener("click", () => {
  modal.style.display = "none";
});

document
  .getElementById("save-appointment-btn")
  .addEventListener("click", async () => {
    const nomeInput = document.getElementById("client-name");
    const horaInput = document.getElementById("client-time");
    const servicoSelect = document.getElementById("client-service");

    if (!nomeInput.value.trim() || !horaInput.value || !servicoSelect.value) {
      alert("Por favor, preencha nome, horário e serviço.");
      return;
    }

    if (!barbeiroAtual) {
      alert(
        "Não foi possível identificar o barbeiro logado. Faça login novamente.",
      );
      return;
    }

    const hoje = new Date();
    const dataHoje = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}-${String(hoje.getDate()).padStart(2, "0")}`;
    const dataHora = `${dataHoje}T${horaInput.value}:00`;

    const resp = await apiFetch("/api/agendamentos", {
      method: "POST",
      body: JSON.stringify({
        barbeiro: { id: barbeiroAtual.id },
        servico: { id: Number(servicoSelect.value) },
        clienteNome: nomeInput.value.trim(),
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

    nomeInput.value = "";
    horaInput.value = "";
    modal.style.display = "none";

    const respAgendamentos = await apiFetch("/api/agendamentos");
    if (respAgendamentos) {
      todosAgendamentos = await respAgendamentos.json();
      atualizarCards();
      renderizarListaHoje();
      renderizarGraficoAnual();
    }
  });

// ===== Inicialização =====
async function carregarDados() {
  await identificarBarbeiroLogado();

  const [respServicos, respAgendamentos] = await Promise.all([
    apiFetch("/api/servicos"),
    apiFetch("/api/agendamentos"),
  ]);

  if (!respServicos || !respAgendamentos) return;

  servicosDisponiveis = await respServicos.json();
  todosAgendamentos = await respAgendamentos.json();

  popularSelectServicos();
  atualizarCards();
  renderizarListaHoje();
  renderizarGraficoAnual();
}

carregarDados();
