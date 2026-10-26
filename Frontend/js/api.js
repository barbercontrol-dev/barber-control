const API_URL = window.API_BASE_URL;

async function apiLogin(email, senha) {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, senha }),
  });

  if (!response.ok) {
    throw new Error("Email ou senha inválidos");
  }

  return response.json();
}

async function apiRegistrar(nomeBarbearia, nomeResponsavel, email, senha) {
  const response = await fetch(`${API_URL}/auth/registrar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      nomeBarbearia,
      nomeResponsavel,
      email,
      senha,
      consentimentoLgpd: true,
    }),
  });

  if (!response.ok) {
    const erro = await response.json().catch(() => ({}));
    throw new Error(erro.erro || "Não foi possível criar a conta");
  }

  return response.json();
}

// Wrapper que injeta o token JWT automaticamente e trata sessão expirada
async function apiFetch(endpoint, options = {}) {
  const token = localStorage.getItem("token");
  if (!token) {
    window.location.href = "login.html";
    return null;
  }

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      ...(options.headers || {}),
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  if (response.status === 401) {
    localStorage.removeItem("token");
    window.location.href = "login.html";
    return null;
  }

  return response;
}

// Escapa texto antes de colocar em innerHTML (evita que um nome vire código)
function escaparHtml(valor) {
  const div = document.createElement("div");
  div.textContent = valor ?? "";
  return div.innerHTML;
}
