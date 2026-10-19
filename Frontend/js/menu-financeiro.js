document.querySelectorAll(".nav-financeiro-toggle").forEach((botao) => {
  const item = botao.closest(".nav-financeiro");
  if (!item) return;

  botao.setAttribute("aria-expanded", String(item.classList.contains("is-open")));
  botao.addEventListener("click", () => {
    const aberto = item.classList.toggle("is-open");
    botao.setAttribute("aria-expanded", String(aberto));
  });
});
