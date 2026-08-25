const form = document.getElementById("form-login");
const erroEl = document.getElementById("form-erro");
const btnEntrar = document.getElementById("btn-entrar");
const togglePass = document.getElementById("toggle-pass");
const inputSenha = document.getElementById("senha");

// Se já tiver um token salvo, pula direto pra tela principal
if (localStorage.getItem("token")) {
  window.location.href = "index.html";
}

// Botão do olho: mostra/oculta a senha digitada
togglePass.addEventListener("click", () => {
  const visivel = inputSenha.type === "text";
  inputSenha.type = visivel ? "password" : "text";
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const email = document.getElementById("email").value.trim();
  const senha = inputSenha.value;

  erroEl.classList.remove("visivel");
  btnEntrar.disabled = true;
  btnEntrar.textContent = "Entrando...";

  try {
    const dados = await apiLogin(email, senha);
    localStorage.setItem("token", dados.token);
    window.location.href = "index.html";
  } catch (err) {
    erroEl.textContent = "Email ou senha incorretos.";
    erroEl.classList.add("visivel");
  } finally {
    btnEntrar.disabled = false;
    btnEntrar.textContent = "Entrar";
  }
});
