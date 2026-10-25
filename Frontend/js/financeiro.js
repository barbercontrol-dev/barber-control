const tipoFinanceiro = document.body.dataset.financeiroTipo;
const simboloMoeda = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});
const coresGraficos = ["#ffc107", "#1f1f1f", "#27ae60", "#4f7cac", "#e07a5f", "#8e6cbb", "#5a9b8a"];

let operacoes = [];
let atendimentosPendentes = [];
let servicosDisponiveis = [];
let graficoTendencia = null;
let graficoDistribuicao = null;

const dataHoje = new Date();
const dataLocal = (data) =>
  `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, "0")}-${String(data.getDate()).padStart(2, "0")}`;
const inicioMes = new Date(dataHoje.getFullYear(), dataHoje.getMonth(), 1);

const periodoInicioEl = document.getElementById("periodo-inicio");
const periodoFimEl = document.getElementById("periodo-fim");
const dataRegistroEl = document.getElementById("registro-data");
const listaOperacoesEl = document.getElementById("lista-operacoes");
const statusEl = document.getElementById("financeiro-status");
const formRegistroEl = document.getElementById("form-registro");
const feedbackEl = document.getElementById("form-feedback");
const botaoSalvarEl = document.getElementById("btn-salvar");
const perfilEl = document.getElementById("profile-name");

periodoInicioEl.value = dataLocal(inicioMes);
periodoFimEl.value = dataLocal(dataHoje);
dataRegistroEl.value = dataLocal(dataHoje);

function formatarMoeda(valor) {
  return simboloMoeda.format(Number(valor) || 0);
}

function formatarData(valor) {
  const data = new Date(`${valor}T12:00:00`);
  return data.toLocaleDateString("pt-BR");
}

function formatarFormaPagamento(valor) {
  return {
    DINHEIRO: "Dinheiro",
    PIX: "Pix",
    DEBITO: "Débito",
    CREDITO: "Crédito",
  }[valor] || valor;
}

function mostrarStatus(mensagem, erro = false) {
  statusEl.textContent = mensagem;
  statusEl.classList.toggle("is-error", erro);
  statusEl.classList.toggle("is-visible", Boolean(mensagem));
}

async function mensagemErroDaResposta(resposta, mensagemPadrao) {
  try {
    const corpo = await resposta.json();
    return corpo.erro || corpo.message || mensagemPadrao;
  } catch {
    return mensagemPadrao;
  }
}

function escapar(valor) {
  const elemento = document.createElement("span");
  elemento.textContent = valor ?? "";
  return elemento.innerHTML;
}

function configurarLogout() {
  document.getElementById("btn-logout").addEventListener("click", (evento) => {
    evento.preventDefault();
    localStorage.removeItem("token");
    window.location.href = "login.html";
  });
}

async function carregarPerfil() {
  const resposta = await apiFetch("/api/usuarios/meus-dados");
  if (!resposta) return;
  if (!resposta.ok) {
    throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível carregar o perfil."));
  }

  const usuario = await resposta.json();
  perfilEl.textContent = usuario.nomeBarbearia || usuario.nome;
}

async function carregarBarbeiros() {
  const barbeiroEl = document.getElementById("registro-barbeiro");
  if (!barbeiroEl) return;

  const resposta = await apiFetch("/api/barbeiros");
  if (!resposta) return;
  if (!resposta.ok) {
    throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível carregar os barbeiros."));
  }

  const barbeiros = await resposta.json();
  if (barbeiros.length === 0) {
    barbeiroEl.innerHTML = '<option value="">Cadastre um barbeiro primeiro</option>';
    barbeiroEl.disabled = true;
    return;
  }

  barbeiroEl.innerHTML = `
    ${barbeiros.map((barbeiro) => `
      <option value="${barbeiro.id}">${escapar(barbeiro.nome)}</option>
    `).join("")}
  `;
  barbeiroEl.disabled = false;
}

async function carregarServicos() {
  if (tipoFinanceiro !== "ENTRADA") return;

  const resposta = await apiFetch("/api/servicos");
  if (!resposta) return;
  if (!resposta.ok) {
    throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível carregar os serviços."));
  }

  servicosDisponiveis = await resposta.json();
  const listaEl = document.getElementById("lista-servicos-financeiro");
  if (servicosDisponiveis.length === 0) {
    listaEl.innerHTML = '<p class="financeiro-vazio">Cadastre serviços antes de registrar um atendimento.</p>';
    return;
  }

  listaEl.innerHTML = servicosDisponiveis.map((servico) => `
    <label class="servico-opcao">
      <input type="checkbox" name="servicoIds" value="${servico.id}" data-valor="${Number(servico.valor) || 0}" />
      <span class="servico-opcao-nome">${escapar(servico.nome)}</span>
      <span class="servico-opcao-valor">${formatarMoeda(servico.valor)}</span>
    </label>
  `).join("");

  listaEl.addEventListener("change", atualizarTotalPreview);
}

function atualizarTotalPreview() {
  if (tipoFinanceiro !== "ENTRADA") return;

  const total = Array.from(document.querySelectorAll('input[name="servicoIds"]:checked'))
    .reduce((soma, checkbox) => soma + Number(checkbox.dataset.valor || 0), 0);
  document.getElementById("total-preview").textContent = formatarMoeda(total);
}

function parseDataLocal(valor) {
  const [ano, mes, dia] = valor.split("-").map(Number);
  return new Date(ano, mes - 1, dia, 12);
}

function criarChavesPeriodo() {
  const inicioValor = periodoInicioEl.value;
  const fimValor = periodoFimEl.value;
  if (!inicioValor || !fimValor || inicioValor > fimValor) {
    throw new Error("Selecione um período válido: a data inicial deve ser anterior à final.");
  }

  const inicio = parseDataLocal(inicioValor);
  const fim = parseDataLocal(fimValor);
  const dias = Math.floor((Date.UTC(fim.getFullYear(), fim.getMonth(), fim.getDate())
    - Date.UTC(inicio.getFullYear(), inicio.getMonth(), inicio.getDate())) / 86400000) + 1;
  const agruparPorDia = dias <= 31;
  const chaves = [];

  if (agruparPorDia) {
    for (let indice = 0; indice < dias; indice += 1) {
      const data = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate() + indice);
      chaves.push({
        id: dataLocal(data),
        label: data.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" }),
      });
    }
  } else {
    const cursor = new Date(inicio.getFullYear(), inicio.getMonth(), 1, 12);
    const ultimoMes = new Date(fim.getFullYear(), fim.getMonth(), 1, 12);
    while (cursor <= ultimoMes) {
      chaves.push({
        id: `${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, "0")}`,
        label: cursor.toLocaleDateString("pt-BR", { month: "short", year: "2-digit" }),
      });
      cursor.setMonth(cursor.getMonth() + 1);
    }
  }

  return { chaves, agruparPorDia };
}

function chaveOperacao(operacao, agruparPorDia) {
  return agruparPorDia ? operacao.data : operacao.data.slice(0, 7);
}

function movimentosParaIndicadores() {
  if (tipoFinanceiro !== "ENTRADA") return operacoes;
  return [
    ...operacoes,
    ...atendimentosPendentes.map((atendimento) => ({
      data: atendimento.data,
      valor: atendimento.valor,
      formaPagamento: "PENDENTE",
    })),
  ];
}

function atualizarIndicadores() {
  const movimentos = movimentosParaIndicadores();
  const total = movimentos.reduce((soma, operacao) => soma + Number(operacao.valor || 0), 0);
  document.getElementById("kpi-total").textContent = formatarMoeda(total);
  document.getElementById("kpi-quantidade").textContent = movimentos.length.toLocaleString("pt-BR");

  if (tipoFinanceiro === "ENTRADA") {
    const media = movimentos.length ? total / movimentos.length : 0;
    document.getElementById("kpi-media").textContent = formatarMoeda(media);
    return;
  }

  const maiorGasto = operacoes.reduce(
    (maior, operacao) => Number(operacao.valor) > Number(maior.valor || 0) ? operacao : maior,
    { valor: 0 },
  );
  document.getElementById("kpi-destaque").textContent = formatarMoeda(maiorGasto.valor);
  document.getElementById("kpi-destaque-legenda").textContent =
    maiorGasto.descricao || "No período selecionado";
}

function renderizarGraficoTendencia() {
  const { chaves, agruparPorDia } = criarChavesPeriodo();
  const valoresPorChave = new Map(chaves.map(({ id }) => [id, 0]));
  movimentosParaIndicadores().forEach((operacao) => {
    const chave = chaveOperacao(operacao, agruparPorDia);
    valoresPorChave.set(chave, (valoresPorChave.get(chave) || 0) + Number(operacao.valor || 0));
  });

  if (graficoTendencia) graficoTendencia.destroy();
  graficoTendencia = new Chart(document.getElementById("grafico-tendencia"), {
    type: "bar",
    data: {
      labels: chaves.map(({ label }) => label),
      datasets: [{
        data: chaves.map(({ id }) => valoresPorChave.get(id) || 0),
        backgroundColor: tipoFinanceiro === "ENTRADA" ? "rgba(39, 174, 96, 0.72)" : "rgba(224, 122, 95, 0.72)",
        borderColor: tipoFinanceiro === "ENTRADA" ? "#27ae60" : "#e07a5f",
        borderWidth: 1,
        borderRadius: 6,
        maxBarThickness: 36,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (contexto) => formatarMoeda(contexto.raw) } },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { color: "#777", callback: (valor) => formatarMoeda(valor) },
          grid: { color: "#f0f0f0" },
        },
        x: { ticks: { color: "#777", maxRotation: 0, autoSkip: true }, grid: { display: false } },
      },
    },
  });
}

function renderizarGraficoDistribuicao() {
  const agrupamentos = new Map();
  movimentosParaIndicadores().forEach((operacao) => {
    const chave = operacao.formaPagamento === "PENDENTE"
      ? "Pagamento pendente"
      : formatarFormaPagamento(operacao.formaPagamento || "Não informado");
    agrupamentos.set(chave, (agrupamentos.get(chave) || 0) + Number(operacao.valor || 0));
  });

  const dados = Array.from(agrupamentos.entries()).filter(([, valor]) => valor > 0);
  const total = dados.reduce((soma, [, valor]) => soma + valor, 0);
  if (graficoDistribuicao) graficoDistribuicao.destroy();
  graficoDistribuicao = new Chart(document.getElementById("grafico-distribuicao"), {
    type: "doughnut",
    data: {
      labels: dados.length ? dados.map(([forma]) => forma) : ["Sem movimentação"],
      datasets: [{
        data: dados.length ? dados.map(([, valor]) => valor) : [1],
        backgroundColor: dados.length ? coresGraficos : ["#e5e5e5"],
        borderColor: "#fff",
        borderWidth: dados.length ? 3 : 0,
        hoverOffset: 5,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: "64%",
      plugins: {
        legend: {
          position: "bottom",
          labels: {
            usePointStyle: true,
            boxWidth: 8,
            padding: 16,
            filter: (item) => item.text !== "Sem movimentação",
          },
        },
        tooltip: {
          filter: () => dados.length > 0,
          callbacks: {
            label: (contexto) => {
              const percentual = total ? (Number(contexto.raw) / total) * 100 : 0;
              return `${contexto.label}: ${formatarMoeda(contexto.raw)} (${percentual.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%)`;
            },
          },
        },
      },
    },
  });
}

function renderizarOperacoes() {
  if (operacoes.length === 0) {
    listaOperacoesEl.innerHTML = `
      <div class="financeiro-empty-state">
        <strong>Nenhuma operação neste período</strong>
        <p>Altere o período ou registre uma nova operação.</p>
      </div>
    `;
    return;
  }

  listaOperacoesEl.innerHTML = operacoes.slice(0, 8).map((operacao) => {
    const titulo = tipoFinanceiro === "ENTRADA" ? operacao.nomeCliente : operacao.descricao;
    const detalhe = tipoFinanceiro === "ENTRADA"
      ? (operacao.servicos || []).map((servico) => servico.nome).join(", ")
      : operacao.categoria;
    return `
      <article class="operacao-item">
        <div class="operacao-detalhes">
          <strong>${escapar(titulo)}</strong>
          <span>${escapar(detalhe)} · ${formatarFormaPagamento(operacao.formaPagamento)}</span>
          <small>${formatarData(operacao.data)}</small>
        </div>
        <strong class="operacao-valor">${formatarMoeda(operacao.valor)}</strong>
      </article>
    `;
  }).join("");
}

async function buscarAtendimentosPendentes() {
  if (tipoFinanceiro !== "ENTRADA") return [];
  const painel = document.getElementById("atendimentos-pendentes");
  const lista = document.getElementById("lista-atendimentos-pendentes");
  const parametros = new URLSearchParams({
    inicio: periodoInicioEl.value,
    fim: periodoFimEl.value,
  });
  const resposta = await apiFetch(`/api/financeiro/atendimentos-pendentes?${parametros}`);
  if (!resposta) return [];
  if (!resposta.ok) {
    throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível verificar atendimentos concluídos."));
  }

  atendimentosPendentes = await resposta.json();
  painel.hidden = atendimentosPendentes.length === 0;
  lista.innerHTML = atendimentosPendentes.map((atendimento) => `
      <article class="atendimento-pendente">
        <div class="atendimento-pendente-detalhes">
          <strong>${escapar(atendimento.nomeCliente)}</strong>
          <span>${escapar(atendimento.servicoNome)} · ${formatarData(atendimento.data)}</span>
        </div>
        <strong class="atendimento-pendente-valor">${formatarMoeda(atendimento.valor)}</strong>
        <label class="sr-only" for="pagamento-pendente-${atendimento.id}">Forma de pagamento para ${escapar(atendimento.nomeCliente)}</label>
        <select id="pagamento-pendente-${atendimento.id}" data-pagamento-pendente="${atendimento.id}">
          <option value="">Forma de pagamento</option>
          <option value="DINHEIRO">Dinheiro</option>
          <option value="PIX">Pix</option>
          <option value="DEBITO">Débito</option>
          <option value="CREDITO">Crédito</option>
        </select>
        <button class="financeiro-btn financeiro-btn-primary" type="button" data-registrar-pendente="${atendimento.id}">
          Registrar entrada
        </button>
      </article>
    `).join("");
  return atendimentosPendentes;
}

async function registrarAtendimentoPendente(id, botao) {
  const pagamentoEl = document.getElementById(`pagamento-pendente-${id}`);
  if (!pagamentoEl.value) {
    pagamentoEl.focus();
    mostrarStatus("Selecione a forma de pagamento do atendimento concluído.", true);
    return;
  }

  botao.disabled = true;
  try {
    const resposta = await apiFetch(`/api/agendamentos/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({
        status: "CONCLUIDO",
        formaPagamento: pagamentoEl.value,
      }),
    });
    if (!resposta) return;
    if (!resposta.ok) {
      throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível registrar a entrada."));
    }
    mostrarStatus("Atendimento incluído nas entradas.");
    await carregarOperacoes();
    await carregarAtendimentosPendentes();
  } catch (erro) {
    mostrarStatus(erro.message || "Não foi possível registrar a entrada.", true);
  } finally {
    botao.disabled = false;
  }
}

async function carregarOperacoes() {
  mostrarStatus("");
  try {
    const { chaves } = criarChavesPeriodo();
    if (chaves.length === 0) throw new Error("O período selecionado não contém datas válidas.");
    const parametros = new URLSearchParams({
      tipo: tipoFinanceiro,
      inicio: periodoInicioEl.value,
      fim: periodoFimEl.value,
    });
    const resposta = await apiFetch(`/api/financeiro?${parametros}`);
    if (!resposta) return;
    if (!resposta.ok) {
      throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível carregar as operações."));
    }

    operacoes = await resposta.json();
    atendimentosPendentes = await buscarAtendimentosPendentes();
    atualizarIndicadores();
    renderizarGraficoTendencia();
    renderizarGraficoDistribuicao();
    renderizarOperacoes();
  } catch (erro) {
    mostrarStatus(erro.message || "Não foi possível carregar as operações.", true);
    listaOperacoesEl.innerHTML = '<p class="financeiro-vazio">Não foi possível carregar as operações.</p>';
  }
}

async function registrarOperacao(evento) {
  evento.preventDefault();
  feedbackEl.textContent = "";
  if (!formRegistroEl.checkValidity()) {
    formRegistroEl.reportValidity();
    return;
  }

  const dados = new FormData(formRegistroEl);
  const payload = tipoFinanceiro === "ENTRADA"
    ? {
      tipo: tipoFinanceiro,
      data: dataRegistroEl.value,
      nomeCliente: dados.get("nomeCliente").trim(),
      servicoIds: Array.from(document.querySelectorAll('input[name="servicoIds"]:checked'))
        .map((checkbox) => Number(checkbox.value)),
      formaPagamento: document.getElementById("forma-pagamento").value,
    }
    : {
      tipo: tipoFinanceiro,
      data: dataRegistroEl.value,
      descricao: document.getElementById("descricao").value.trim(),
      categoria: document.getElementById("categoria").value,
      valor: Number(document.getElementById("valor").value),
      formaPagamento: document.getElementById("forma-pagamento").value,
    };

  if (tipoFinanceiro === "ENTRADA" && payload.servicoIds.length === 0) {
    feedbackEl.textContent = "Selecione ao menos um serviço.";
    return;
  }

  botaoSalvarEl.disabled = true;
  botaoSalvarEl.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Salvando...';
  try {
    const resposta = await apiFetch("/api/financeiro", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    if (!resposta) return;
    if (!resposta.ok) {
      throw new Error(await mensagemErroDaResposta(resposta, "Não foi possível registrar a operação."));
    }

    formRegistroEl.reset();
    dataRegistroEl.value = dataLocal(new Date());
    if (tipoFinanceiro === "ENTRADA") {
      document.getElementById("total-preview").textContent = formatarMoeda(0);
    }
    await carregarOperacoes();
    mostrarStatus(tipoFinanceiro === "ENTRADA" ? "Atendimento registrado com sucesso." : "Despesa registrada com sucesso.");
  } catch (erro) {
    feedbackEl.textContent = erro.message || "Não foi possível registrar a operação.";
  } finally {
    botaoSalvarEl.disabled = false;
    botaoSalvarEl.innerHTML = `<i class="fa-solid fa-plus"></i> Registrar ${tipoFinanceiro === "ENTRADA" ? "entrada" : "saída"}`;
  }
}

document.getElementById("form-periodo").addEventListener("submit", (evento) => {
  evento.preventDefault();
  carregarOperacoes();
});
formRegistroEl.addEventListener("submit", registrarOperacao);
document.getElementById("lista-atendimentos-pendentes")?.addEventListener("click", (evento) => {
  const botao = evento.target.closest("[data-registrar-pendente]");
  if (botao) registrarAtendimentoPendente(botao.dataset.registrarPendente, botao);
});
configurarLogout();

Promise.all([carregarPerfil(), carregarBarbeiros(), carregarServicos()])
  .then(carregarOperacoes)
  .catch((erro) => mostrarStatus(erro.message || "Não foi possível carregar os dados financeiros.", true));
