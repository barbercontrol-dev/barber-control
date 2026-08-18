const form = document.getElementById("form-login");
const erroDiv = document.getElementById("erro-login");
const btnEntrar = document.getElementById("btn-entrar");
const toggleSenha = document.getElementById("toggle-senha");
const inputSenha = document.getElementById("senha");

// Se já tiver um token salvo, pula direto pra agenda
if (localStorage.getItem("token")) {
  window.location.href = "agenda.html";
}

toggleSenha.addEventListener("click", () => {
  const visivel = inputSenha.type === "text";
  inputSenha.type = visivel ? "password" : "text";
  toggleSenha.textContent = visivel ? "👁" : "🙈";
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const email = document.getElementById("email").value.trim();
  const senha = inputSenha.value;

  erroDiv.classList.remove("visible");
  btnEntrar.disabled = true;
  btnEntrar.textContent = "Entrando...";

  try {
    const dados = await apiLogin(email, senha);
    localStorage.setItem("token", dados.token);
    window.location.href = "agenda.html";
  } catch (err) {
    erroDiv.textContent = "Email ou senha incorretos.";
    erroDiv.classList.add("visible");
  } finally {
    btnEntrar.disabled = false;
    btnEntrar.textContent = "Entrar";
  }
});
