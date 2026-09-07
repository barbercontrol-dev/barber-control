const form = document.getElementById("form-cadastro");
const erroEl = document.getElementById("form-erro");
const btnCadastrar = document.getElementById("btn-cadastrar");

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const nome = document.getElementById("nome").value.trim();
  const email = document.getElementById("email").value.trim();
  const senha = document.getElementById("senha").value;
  const repetirSenha = document.getElementById("repetir-senha").value;

  erroEl.textContent = "";
  erroEl.classList.remove("visivel");

  if (senha !== repetirSenha) {
    erroEl.textContent = "As senhas não coincidem.";
    erroEl.classList.add("visivel");
    return;
  }

  btnCadastrar.disabled = true;
  btnCadastrar.textContent = "Cadastrando...";

  try {
    await apiRegistrar(nome, email, senha);
    window.location.href = "login.html";
  } catch (error) {
    erroEl.textContent =
      "Não foi possível criar a conta. Verifique os dados e tente novamente.";
    erroEl.classList.add("visivel");
  } finally {
    btnCadastrar.disabled = false;
    btnCadastrar.textContent = "Cadastrar";
  }
});

document.querySelectorAll(".toggle-visibility").forEach((btn) => {
  btn.addEventListener("click", () => {
    const input = document.getElementById(btn.dataset.target);
    const isPassword = input.type === "password";
    input.type = isPassword ? "text" : "password";
    btn.classList.toggle("is-visible", isPassword);
    btn.setAttribute(
      "aria-label",
      isPassword ? "Ocultar senha" : "Mostrar senha",
    );
  });
});
