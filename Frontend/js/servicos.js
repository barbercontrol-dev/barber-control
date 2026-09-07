const listaServicosEl = document.getElementById("lista-servicos");
const servicosVazioEl = document.getElementById("servicos-vazio");
const buscaServicoEl = document.getElementById("busca-servico");
const modalServicoEl = document.getElementById("modal-servico");
const formServicoEl = document.getElementById("form-servico");
const modalTituloEl = document.getElementById("modal-servico-titulo");
const nomeServicoEl = document.getElementById("servico-nome");
const valorServicoEl = document.getElementById("servico-valor");
const duracaoServicoEl = document.getElementById("servico-duracao");
const erroServicoEl = document.getElementById("servico-form-erro");
const btnSalvarServicoEl = document.getElementById("btn-salvar-servico");
const profileNameEl = document.getElementById("profile-name");

let servicos = [];
let servicoEmEdicao = null;

function escaparHtml(valor) {
  const div = document.createElement("div");
  div.textContent = valor ?? "";
  return div.innerHTML;
}

function formatarValor(valor) {
  return Number(valor || 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

function formatarDuracao(minutos) {
  const horas = Math.floor(minutos / 60);
  const restante = minutos % 60;
  if (!horas) return `${minutos} min`;
  return restante ? `${horas}h ${restante}min` : `${horas}h`;
}

function renderizarServicos() {
  const termo = buscaServicoEl.value.trim().toLowerCase();
  const filtrados = servicos.filter((servico) =>
    (servico.nome || "").toLowerCase().includes(termo),
  );

  listaServicosEl.innerHTML = filtrados
    .map(
      (servico) => `
        <tr>
          <td>S${String(servico.id).padStart(3, "0")}</td>
          <td class="servico-nome-tabela">${escaparHtml(servico.nome)}</td>
          <td>${formatarValor(servico.valor)}</td>
          <td>${formatarDuracao(servico.duracaoMinutos)}</td>
          <td class="servico-acoes">
            <button type="button" class="btn-icone-servico" data-editar-servico="${servico.id}" aria-label="Editar ${escaparHtml(servico.nome)}" title="Editar">
              <i class="fa-solid fa-pen"></i>
            </button>
            <button type="button" class="btn-icone-servico btn-excluir-servico" data-excluir-servico="${servico.id}" aria-label="Excluir ${escaparHtml(servico.nome)}" title="Excluir">
              <i class="fa-solid fa-trash-can"></i>
            </button>
          </td>
        </tr>
      `,
    )
    .join("");

  servicosVazioEl.hidden = filtrados.length > 0;
  listaServicosEl.querySelectorAll("[data-editar-servico]").forEach((botao) => {
    botao.addEventListener("click", () =>
      abrirModalServico(botao.dataset.editarServico),
    );
  });
  listaServicosEl
    .querySelectorAll("[data-excluir-servico]")
    .forEach((botao) => {
      botao.addEventListener("click", () =>
        excluirServico(botao.dataset.excluirServico),
      );
    });
}

function abrirModalServico(id = null) {
  servicoEmEdicao = id
    ? servicos.find((servico) => String(servico.id) === String(id))
    : null;
  modalTituloEl.textContent = servicoEmEdicao
    ? "Editar serviço"
    : "Adicionar serviço";
  nomeServicoEl.value = servicoEmEdicao?.nome || "";
  valorServicoEl.value = servicoEmEdicao?.valor ?? "";
  duracaoServicoEl.value = servicoEmEdicao?.duracaoMinutos ?? "";
  erroServicoEl.textContent = "";
  modalServicoEl.style.display = "flex";
  nomeServicoEl.focus();
}

function fecharModalServico() {
  modalServicoEl.style.display = "none";
  servicoEmEdicao = null;
}

async function carregarServicos() {
  const resposta = await apiFetch("/api/servicos");
  if (!resposta) return;
  if (!resposta.ok) {
    servicosVazioEl.textContent = "Não foi possível carregar os serviços.";
    servicosVazioEl.hidden = false;
    return;
  }
  servicos = await resposta.json();
  renderizarServicos();
}

async function excluirServico(id) {
  const servico = servicos.find((item) => String(item.id) === String(id));
  if (!servico || !confirm(`Excluir o serviço "${servico.nome}"?`)) return;

  const resposta = await apiFetch(`/api/servicos/${id}`, { method: "DELETE" });
  if (!resposta || !resposta.ok) {
    alert("Não foi possível excluir o serviço.");
    return;
  }
  await carregarServicos();
}

formServicoEl.addEventListener("submit", async (event) => {
  event.preventDefault();
  erroServicoEl.textContent = "";
  btnSalvarServicoEl.disabled = true;
  btnSalvarServicoEl.textContent = "Salvando...";

  const payload = {
    nome: nomeServicoEl.value.trim(),
    valor: Number(valorServicoEl.value),
    duracaoMinutos: Number(duracaoServicoEl.value),
  };

  try {
    const endpoint = servicoEmEdicao
      ? `/api/servicos/${servicoEmEdicao.id}`
      : "/api/servicos";
    const resposta = await apiFetch(endpoint, {
      method: servicoEmEdicao ? "PUT" : "POST",
      body: JSON.stringify(payload),
    });

    if (!resposta || !resposta.ok) {
      throw new Error("Não foi possível salvar o serviço.");
    }

    fecharModalServico();
    await carregarServicos();
  } catch (error) {
    erroServicoEl.textContent = error.message;
  } finally {
    btnSalvarServicoEl.disabled = false;
    btnSalvarServicoEl.textContent = "Salvar";
  }
});

document
  .getElementById("btn-adicionar-servico")
  .addEventListener("click", () => abrirModalServico());
document
  .getElementById("btn-cancelar-servico")
  .addEventListener("click", fecharModalServico);
modalServicoEl.addEventListener("click", (event) => {
  if (event.target === modalServicoEl) fecharModalServico();
});
buscaServicoEl.addEventListener("input", renderizarServicos);
document.getElementById("btn-logout").addEventListener("click", (event) => {
  event.preventDefault();
  localStorage.removeItem("token");
  window.location.href = "login.html";
});

async function carregarPerfil() {
  const resposta = await apiFetch("/api/usuarios");
  if (!resposta || !resposta.ok) return;
  const usuarios = await resposta.json();
  const token = localStorage.getItem("token");
  const payload = token ? JSON.parse(atob(token.split(".")[1])) : null;
  const usuario = usuarios.find((item) => item.email === payload?.sub);
  if (usuario) profileNameEl.textContent = usuario.nome;
}

Promise.all([carregarPerfil(), carregarServicos()]);
