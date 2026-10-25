const listaBarbeirosEl = document.getElementById("lista-barbeiros");
const barbeirosVazioEl = document.getElementById("barbeiros-vazio");
const modalBarbeiroEl = document.getElementById("modal-barbeiro");
const formBarbeiroEl = document.getElementById("form-barbeiro");
const modalTituloEl = document.getElementById("modal-barbeiro-titulo");
const nomeBarbeiroEl = document.getElementById("barbeiro-nome");
const telefoneBarbeiroEl = document.getElementById("barbeiro-telefone");
const cpfBarbeiroEl = document.getElementById("barbeiro-cpf");
const erroBarbeiroEl = document.getElementById("barbeiro-form-erro");
const btnSalvarBarbeiroEl = document.getElementById("btn-salvar-barbeiro");
const profileNameEl = document.getElementById("profile-name");
let barbeiros = [];
let barbeiroEmEdicao = null;

function renderizarBarbeiros(barbeiros) {
  listaBarbeirosEl.innerHTML = barbeiros
    .map((barbeiro) => `
      <tr>
        <td>${escaparHtml(barbeiro.nome)}</td>
        <td>${escaparHtml(formatarTelefone(barbeiro.telefone))}</td>
        <td>${escaparHtml(formatarCpf(barbeiro.cpf))}</td>
        <td class="servico-acoes">
          <button type="button" class="btn-icone-servico" data-editar-barbeiro="${barbeiro.id}" aria-label="Editar barbeiro" title="Editar">
            <i class="fa-solid fa-pen"></i>
          </button>
          <button type="button" class="btn-icone-servico btn-excluir-barbeiro" data-excluir-barbeiro="${barbeiro.id}" aria-label="Excluir barbeiro" title="Excluir">
            <i class="fa-solid fa-trash-can"></i>
          </button>
        </td>
      </tr>
    `)
    .join("");
  barbeirosVazioEl.hidden = barbeiros.length > 0;
  listaBarbeirosEl.querySelectorAll("[data-editar-barbeiro]").forEach((botao) => {
    botao.addEventListener("click", () => abrirModal(botao.dataset.editarBarbeiro));
  });
  listaBarbeirosEl.querySelectorAll("[data-excluir-barbeiro]").forEach((botao) => {
    botao.addEventListener("click", () => excluirBarbeiro(botao.dataset.excluirBarbeiro));
  });
}

async function carregarBarbeiros() {
  const resposta = await apiFetch("/api/barbeiros");
  if (!resposta) return;
  if (!resposta.ok) {
    barbeirosVazioEl.textContent = "Não foi possível carregar os barbeiros.";
    barbeirosVazioEl.hidden = false;
    return;
  }
  barbeiros = await resposta.json();
  renderizarBarbeiros(barbeiros);
}

async function carregarPerfil() {
  const resposta = await apiFetch("/api/usuarios/meus-dados");
  if (!resposta || !resposta.ok) return;
  const usuario = await resposta.json();
  profileNameEl.textContent = usuario.nomeBarbearia || usuario.nome;
}

function abrirModal(id = null) {
  barbeiroEmEdicao = id
    ? barbeiros.find((barbeiro) => String(barbeiro.id) === String(id))
    : null;
  formBarbeiroEl.reset();
  modalTituloEl.textContent = barbeiroEmEdicao ? "Editar barbeiro" : "Adicionar barbeiro";
  nomeBarbeiroEl.value = barbeiroEmEdicao?.nome || "";
  telefoneBarbeiroEl.value = formatarTelefone(barbeiroEmEdicao?.telefone);
  cpfBarbeiroEl.value = formatarCpf(barbeiroEmEdicao?.cpf);
  erroBarbeiroEl.textContent = "";
  modalBarbeiroEl.style.display = "flex";
  nomeBarbeiroEl.focus();
}

function fecharModal() {
  modalBarbeiroEl.style.display = "none";
  barbeiroEmEdicao = null;
}

async function excluirBarbeiro(id) {
  const barbeiro = barbeiros.find((item) => String(item.id) === String(id));
  if (
    !barbeiro ||
    !confirm(
      `Excluir "${barbeiro.nome}"? Os agendamentos vinculados serão removidos; os registros financeiros serão mantidos.`,
    )
  ) return;

  try {
    const resposta = await apiFetch(`/api/barbeiros/${id}`, { method: "DELETE" });
    if (!resposta || !resposta.ok) {
      const erro = resposta ? await resposta.json().catch(() => ({})) : {};
      throw new Error(erro.erro || "Não foi possível excluir o barbeiro.");
    }
    await carregarBarbeiros();
  } catch (error) {
    alert(error.message || "Não foi possível excluir o barbeiro.");
  }
}

function formatarTelefone(valor) {
  const digitos = String(valor || "").replace(/\D/g, "");
  if (digitos.length === 11) {
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`;
  }
  if (digitos.length === 10) {
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`;
  }
  return valor || "";
}

function formatarCpf(valor) {
  const digitos = String(valor || "").replace(/\D/g, "");
  return digitos.length === 11
    ? `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`
    : valor || "";
}

formBarbeiroEl.addEventListener("submit", async (event) => {
  event.preventDefault();
  erroBarbeiroEl.textContent = "";
  btnSalvarBarbeiroEl.disabled = true;
  btnSalvarBarbeiroEl.textContent = "Salvando...";

  try {
    const resposta = await apiFetch(
      barbeiroEmEdicao ? `/api/barbeiros/${barbeiroEmEdicao.id}` : "/api/barbeiros",
      {
        method: barbeiroEmEdicao ? "PUT" : "POST",
        body: JSON.stringify({
          nome: nomeBarbeiroEl.value.trim(),
          telefone: telefoneBarbeiroEl.value.trim(),
          cpf: cpfBarbeiroEl.value.trim(),
        }),
      },
    );
    if (!resposta || !resposta.ok) {
      const erro = resposta ? await resposta.json().catch(() => ({})) : {};
      const mensagem = resposta?.status === 403
        ? erro.erro || "Sua conta não tem permissão para gerenciar barbeiros."
        : erro.erro || "Não foi possível salvar o barbeiro.";
      throw new Error(mensagem);
    }
    fecharModal();
    await carregarBarbeiros();
  } catch (error) {
    erroBarbeiroEl.textContent = error.message;
  } finally {
    btnSalvarBarbeiroEl.disabled = false;
    btnSalvarBarbeiroEl.textContent = "Salvar";
  }
});

document.getElementById("btn-adicionar-barbeiro").addEventListener("click", () => abrirModal());
document.getElementById("btn-cancelar-barbeiro").addEventListener("click", fecharModal);
modalBarbeiroEl.addEventListener("click", (event) => {
  if (event.target === modalBarbeiroEl) fecharModal();
});
document.getElementById("btn-logout").addEventListener("click", (event) => {
  event.preventDefault();
  localStorage.removeItem("token");
  window.location.href = "login.html";
});

Promise.all([carregarPerfil(), carregarBarbeiros()]);

telefoneBarbeiroEl.addEventListener("input", () => {
  const digitos = telefoneBarbeiroEl.value.replace(/\D/g, "").slice(0, 11);
  if (digitos.length > 6) {
    const corte = digitos.length > 10 ? 7 : 6;
    telefoneBarbeiroEl.value =
      `(${digitos.slice(0, 2)}) ${digitos.slice(2, corte)}-${digitos.slice(corte)}`;
  } else if (digitos.length > 2) {
    telefoneBarbeiroEl.value = `(${digitos.slice(0, 2)}) ${digitos.slice(2)}`;
  } else {
    telefoneBarbeiroEl.value = digitos;
  }
});

cpfBarbeiroEl.addEventListener("input", () => {
  const digitos = cpfBarbeiroEl.value.replace(/\D/g, "").slice(0, 11);
  cpfBarbeiroEl.value = digitos
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
});
