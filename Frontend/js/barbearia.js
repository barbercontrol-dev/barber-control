(() => {
    const formEl = document.getElementById("form-barbearia");
    if (!formEl) return;

    const enderecoEl = document.getElementById("barbearia-endereco");
    const fotoEl = document.getElementById("barbearia-foto");
    const previewEl = document.getElementById("barbearia-preview");
    const removerEl = document.getElementById("barbearia-remover-foto");
    const msgEl = document.getElementById("barbearia-msg");
    let fotoNova = null;

    function mostrarFoto(src) {
        previewEl.src = src || "";
        previewEl.style.display = src ? "block" : "none";
    }

    function redimensionar(arquivo) {
        return new Promise((resolve, reject) => {
            const leitor = new FileReader();
            leitor.onerror = () => reject(new Error("Não foi possível ler a imagem."));
            leitor.onload = () => {
                const img = new Image();
                img.onerror = () => reject(new Error("Arquivo de imagem inválido."));
                img.onload = () => {
                    const escala = Math.min(1, 900 / Math.max(img.width, img.height));
                    const canvas = document.createElement("canvas");
                    canvas.width = Math.round(img.width * escala);
                    canvas.height = Math.round(img.height * escala);
                    canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
                    resolve(canvas.toDataURL("image/jpeg", 0.82));
                };
                img.src = leitor.result;
            };
            leitor.readAsDataURL(arquivo);
        });
    }

    async function carregar() {
        const resp = await apiFetch("/api/barbearia");
        if (!resp || !resp.ok) return;
        const dados = await resp.json();
        enderecoEl.value = dados.endereco || "";
        mostrarFoto(dados.foto);
    }

    fotoEl.addEventListener("change", async () => {
        msgEl.textContent = "";
        const arquivo = fotoEl.files[0];
        if (!arquivo) return;
        try {
            fotoNova = await redimensionar(arquivo);
            mostrarFoto(fotoNova);
        } catch (erro) {
            msgEl.textContent = erro.message;
        }
    });

    removerEl.addEventListener("click", () => {
        fotoNova = "";
        fotoEl.value = "";
        mostrarFoto("");
    });

    formEl.addEventListener("submit", async (event) => {
        event.preventDefault();
        msgEl.textContent = "";
        const corpo = { endereco: enderecoEl.value.trim() };
        if (fotoNova !== null) corpo.foto = fotoNova;

        const resp = await apiFetch("/api/barbearia", { method: "PUT", body: JSON.stringify(corpo) });
        if (!resp || !resp.ok) {
            const erro = resp ? await resp.json().catch(() => ({})) : {};
            msgEl.textContent = erro.erro || "Não foi possível salvar os dados da barbearia.";
            return;
        }
        fotoNova = null;
        msgEl.textContent = "Dados da barbearia salvos.";
    });

    carregar();
})();