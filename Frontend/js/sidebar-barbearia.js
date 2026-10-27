(() => {
  const avatarEl = document.querySelector(".profile-avatar");
  if (!avatarEl) return;

  const iconEl = avatarEl.querySelector("i");
  let fotoEl = avatarEl.querySelector("img");

  window.atualizarFotoSidebar = (foto) => {
    if (!foto) {
      if (fotoEl) fotoEl.style.display = "none";
      if (iconEl) iconEl.style.display = "";
      return;
    }

    if (!fotoEl) {
      fotoEl = document.createElement("img");
      fotoEl.alt = "Foto da barbearia";
      fotoEl.addEventListener("error", () => {
        fotoEl.style.display = "none";
        if (iconEl) iconEl.style.display = "";
        console.error("Não foi possível exibir a foto da barbearia na sidebar.");
      });
      avatarEl.appendChild(fotoEl);
    }

    fotoEl.src = foto;
    fotoEl.style.display = "block";
    if (iconEl) iconEl.style.display = "none";
  };

  async function carregarFotoSidebar() {
    try {
      const resposta = await apiFetch("/api/barbearia");
      if (!resposta) return;
      if (!resposta.ok) {
        console.error(`Não foi possível carregar a foto da barbearia: HTTP ${resposta.status}.`);
        return;
      }

      const dados = await resposta.json();
      window.atualizarFotoSidebar(dados.foto);
    } catch (erro) {
      console.error("Não foi possível carregar a foto da barbearia na sidebar.", erro);
    }
  }

  carregarFotoSidebar();
})();
