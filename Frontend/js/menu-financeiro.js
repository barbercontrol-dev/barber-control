document.querySelectorAll(".nav-financeiro-toggle").forEach((botao) => {
  const item = botao.closest(".nav-financeiro");
  if (!item) return;

  botao.setAttribute("aria-expanded", String(item.classList.contains("is-open")));
  botao.addEventListener("click", () => {
    const aberto = item.classList.toggle("is-open");
    botao.setAttribute("aria-expanded", String(aberto));
  });
});

const Permissoes = (() => {
  let cache = null;

  async function usuario() {
    if (cache) return cache;
    const resp = await apiFetch("/api/usuarios/meus-dados");
    cache = resp && resp.ok ? await resp.json() : {};
    return cache;
  }

  function ehAdmin(u) {
    const perfil = String(u?.perfil || u?.role || "").toUpperCase();
    return !perfil || perfil.includes("ADMIN");
  }

  return { usuario, ehAdmin };
})();

(async () => {
  if (!localStorage.getItem("token")) return;
  const u = await Permissoes.usuario();
  if (Permissoes.ehAdmin(u)) return;

  document
      .querySelectorAll('a[href="barbeiros.html"]')
      .forEach((link) => link.closest("li")?.remove());
  if (location.pathname.endsWith("barbeiros.html")) {
    location.href = "index.html";
  }
})();