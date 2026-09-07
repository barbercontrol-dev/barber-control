const form = document.getElementById("form-login");
const erroEl = document.getElementById("form-erro");
const btnEntrar = document.getElementById("btn-entrar");
const inputSenha = document.getElementById("senha");

// Se já tiver um token salvo, pula direto pra tela principal
if (localStorage.getItem("token")) {
  window.location.href = "index.html";
}

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
