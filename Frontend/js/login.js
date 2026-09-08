const form = document.getElementById("form-login");
const erroEl = document.getElementById("form-erro");
const btnEntrar = document.getElementById("btn-entrar");
const togglePass = document.querySelector(".toggle-visibility");
const inputSenha = document.getElementById("senha");

if (localStorage.getItem("token")) {
  window.location.href = "index.html";
}

togglePass.addEventListener("click", () => {
  const visivel = inputSenha.type === "text";
  inputSenha.type = visivel ? "password" : "text";
  togglePass.classList.toggle("is-visible", !visivel);
  togglePass.setAttribute(
    "aria-label",
    visivel ? "Mostrar senha" : "Ocultar senha",
  );
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
