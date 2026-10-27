(() => {
  const api = `${window.API_BASE_URL}/api/publico`;
  const form = document.getElementById("booking-form");
  const barberOptions = document.getElementById("barbeiros");
  const serviceOptions = document.getElementById("servicos");
  const dateInput = document.getElementById("data");
  const timeSelect = document.getElementById("hora");
  const nameInput = document.getElementById("nome");
  const phoneInput = document.getElementById("telefone");
  const submitButton = document.getElementById("btn-agendar");
  const message = document.getElementById("form-message");
  const shopInfo = document.getElementById("barbearia-info");
  const shopName = document.getElementById("nome-barbearia");
  const shopPhoto = document.getElementById("barbearia-foto");
  const shopAddress = document.getElementById("barbearia-endereco");

  let barbers = [];
  let services = [];
  let availabilityRequest = 0;
  let barberRequest = 0;
  let serviceRequest = 0;
  let isSubmitting = false;

  function localDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function setMessage(text, type = "") {
    message.textContent = text;
    message.className = type;
  }

  async function getJson(url) {
    const response = await fetch(url);
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.erro || "Não foi possível carregar os dados.");
    }
    return data;
  }

  function selectedValue(groupName) {
    return form.querySelector(`input[name="${groupName}"]:checked`)?.value || "";
  }

  function selectedValues(groupName) {
    return Array.from(
      form.querySelectorAll(`input[name="${groupName}"]:checked`),
      (input) => input.value,
    );
  }

  function updateSubmitState() {
    submitButton.disabled =
      isSubmitting ||
      !nameInput.value.trim() ||
      phoneInput.value.replace(/\D/g, "").length < 10 ||
      !selectedValue("barbeiro") ||
      !selectedValues("servico").length ||
      !dateInput.value ||
      !timeSelect.value;
  }

  function clearTimeOptions(placeholder) {
    timeSelect.replaceChildren(new Option(placeholder, ""));
    timeSelect.disabled = true;
  }

  function selectedRadioCard(container, radio) {
    container.querySelectorAll("label").forEach((label) => {
      label.classList.toggle(
        "is-selected",
        label.querySelector('input[type="radio"]')?.checked === true,
      );
    });
  }

  function renderBarbers() {
    barberOptions.replaceChildren();
    if (!barbers.length) {
      const empty = document.createElement("p");
      empty.className = "empty-message";
      empty.textContent = "Nenhum barbeiro está disponível para agendamento.";
      barberOptions.appendChild(empty);
      return;
    }

    barbers.forEach((barber, index) => {
      const label = document.createElement("label");
      label.className = "barber-card";
      const name = document.createElement("span");
      name.textContent = barber.nome || `Barbeiro ${barber.id}`;
      const radio = document.createElement("input");
      radio.type = "radio";
      radio.name = "barbeiro";
      radio.value = barber.id;
      radio.required = true;
      radio.setAttribute("aria-label", `Barbeiro ${name.textContent}`);
      if (index === 0) radio.checked = true;
      radio.addEventListener("change", () => {
        selectedRadioCard(barberOptions, radio);
        setMessage("");
        loadServicesAndShop();
        updateSubmitState();
      });
      label.append(name, radio);
      barberOptions.appendChild(label);
      if (radio.checked) label.classList.add("is-selected");
    });
  }

  function renderServices() {
    serviceOptions.replaceChildren();
    if (!services.length) {
      const empty = document.createElement("p");
      empty.className = "empty-message";
      empty.textContent = "Este barbeiro ainda não tem serviços disponíveis.";
      serviceOptions.appendChild(empty);
      return;
    }

    services.forEach((service) => {
      const label = document.createElement("label");
      label.className = "service-card";
      const photoContainer = document.createElement("span");
      photoContainer.className = "service-photo";

      if (service.foto) {
        const image = document.createElement("img");
        image.src = service.foto;
        image.alt = `Foto do serviço ${service.nome}`;
        image.loading = "lazy";
        image.addEventListener("error", () => {
          const fallback = document.createElement("span");
          fallback.className = "service-photo-fallback";
          fallback.textContent = "Foto indisponível";
          photoContainer.replaceChildren(fallback);
        }, { once: true });
        photoContainer.appendChild(image);
      } else {
        const fallback = document.createElement("span");
        fallback.className = "service-photo-fallback";
        fallback.textContent = "Foto indisponível";
        photoContainer.appendChild(fallback);
      }

      const details = document.createElement("span");
      details.className = "service-details";
      const description = document.createElement("span");
      description.className = "service-description";
      const name = document.createElement("span");
      name.className = "service-name";
      name.textContent = service.nome;
      const meta = document.createElement("span");
      meta.className = "service-meta";
      meta.textContent = `${Number(service.valor).toLocaleString("pt-BR", {
        style: "currency",
        currency: "BRL",
      })} · ${service.duracaoMinutos} min`;
      description.append(name, meta);

      const radio = document.createElement("input");
      radio.type = "checkbox";
      radio.name = "servico";
      radio.value = service.id;
      radio.setAttribute("aria-label", service.nome);
      radio.addEventListener("change", () => {
        label.classList.toggle("is-selected", radio.checked);
        setMessage("");
        loadAvailability();
        updateSubmitState();
      });
      details.append(description, radio);
      label.append(photoContainer, details);
      serviceOptions.appendChild(label);
    });
  }

  async function loadShop(barberId, requestId) {
    shopInfo.hidden = true;
    shopPhoto.hidden = true;
    shopAddress.textContent = "";
    if (!barberId) return;

    try {
      const data = await getJson(
        `${api}/barbearia?barbeiroId=${encodeURIComponent(barberId)}`,
      );
      if (requestId !== barberRequest) return;
      if (data.nomeBarbearia) shopName.textContent = data.nomeBarbearia;
      if (data.foto) {
        shopPhoto.src = data.foto;
        shopPhoto.hidden = false;
        shopPhoto.onerror = () => {
          shopPhoto.hidden = true;
          setMessage("Não foi possível exibir a foto da barbearia.", "error");
        };
      }
      shopAddress.textContent = data.endereco ? `📍 ${data.endereco}` : "";
      shopInfo.hidden = !data.foto && !data.endereco;
    } catch (error) {
      if (requestId === barberRequest) setMessage(error.message, "error");
    }
  }

  async function loadServicesAndShop() {
    const barberId = selectedValue("barbeiro");
    const requestId = ++barberRequest;
    const currentServiceRequest = ++serviceRequest;
    services = [];
    serviceOptions.innerHTML = '<p class="loading-message">Carregando serviços...</p>';
    clearTimeOptions("Escolha um serviço e uma data");
    updateSubmitState();
    loadShop(barberId, requestId);
    if (!barberId) {
      serviceOptions.innerHTML = '<p class="empty-message">Selecione um barbeiro para ver os serviços.</p>';
      return;
    }

    try {
      const data = await getJson(
        `${api}/servicos?barbeiroId=${encodeURIComponent(barberId)}`,
      );
      if (currentServiceRequest !== serviceRequest) return;
      services = Array.isArray(data) ? data : [];
      renderServices();
      loadAvailability();
    } catch (error) {
      if (currentServiceRequest !== serviceRequest) return;
      serviceOptions.replaceChildren();
      const failure = document.createElement("p");
      failure.className = "empty-message";
      failure.textContent = error.message;
      serviceOptions.appendChild(failure);
      clearTimeOptions("Não foi possível carregar os serviços");
      updateSubmitState();
    }
  }

  async function loadAvailability() {
    const requestId = ++availabilityRequest;
    const barberId = selectedValue("barbeiro");
    const serviceIds = selectedValues("servico");
    const selectedDate = dateInput.value;
    clearTimeOptions("Carregando horários...");
    updateSubmitState();

    if (!barberId || !serviceIds.length || !selectedDate) {
      clearTimeOptions("Escolha a data, o barbeiro e o serviço");
      return;
    }

    try {
      const params = new URLSearchParams();
      params.set("barbeiroId", barberId);
      params.set("data", selectedDate);
      serviceIds.forEach((serviceId) => params.append("servicoIds", serviceId));
      const times = await getJson(`${api}/horarios?${params}`);
      if (requestId !== availabilityRequest) return;
      timeSelect.replaceChildren(new Option("Selecione um horário", ""));

      if (!Array.isArray(times) || !times.length) {
        clearTimeOptions("Não há horários disponíveis nesta data");
        setMessage("Não encontramos horários livres para essa combinação. Escolha outra data.", "error");
        updateSubmitState();
        return;
      }

      times.forEach((time) => timeSelect.add(new Option(time, time)));
      timeSelect.disabled = false;
      setMessage("");
      updateSubmitState();
    } catch (error) {
      if (requestId !== availabilityRequest) return;
      clearTimeOptions("Erro ao consultar horários");
      setMessage(error.message, "error");
      updateSubmitState();
    }
  }

  async function loadBarbers() {
    try {
      const data = await getJson(`${api}/barbeiros`);
      barbers = Array.isArray(data) ? data : [];
      renderBarbers();
      if (!barbers.length) return;
      await loadServicesAndShop();
    } catch (error) {
      barberOptions.replaceChildren();
      const failure = document.createElement("p");
      failure.className = "empty-message";
      failure.textContent = error.message;
      barberOptions.appendChild(failure);
      setMessage("Não foi possível carregar os barbeiros. Atualize a página para tentar novamente.", "error");
    }
  }

  async function submitBooking(event) {
    event.preventDefault();
    setMessage("");
    if (!form.reportValidity()) return;

    isSubmitting = true;
    updateSubmitState();
    submitButton.textContent = "Confirmando...";
    try {
      const response = await fetch(`${api}/agendar`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          barbeiroId: Number(selectedValue("barbeiro")),
          servicoIds: selectedValues("servico").map(Number),
          clienteNome: nameInput.value.trim(),
          clienteTelefone: phoneInput.value,
          dataHora: `${dateInput.value}T${timeSelect.value}:00`,
        }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.erro || "Não foi possível confirmar o agendamento.");
      }

      const bookedDate = new Date(`${dateInput.value}T${timeSelect.value}:00`);
      const formattedDate = bookedDate.toLocaleDateString("pt-BR");
      const barberName = barbers.find(
        (barber) => String(barber.id) === selectedValue("barbeiro"),
      )?.nome;
      const bookedTime = timeSelect.value;
      nameInput.value = "";
      phoneInput.value = "";
      timeSelect.value = "";
      await loadAvailability();
      setMessage(
        `Agendamento confirmado para ${formattedDate} às ${bookedTime}${barberName ? ` com ${barberName}` : ""}.`,
        "success",
      );
    } catch (error) {
      setMessage(error.message || "Erro de conexão. Tente novamente.", "error");
    } finally {
      isSubmitting = false;
      submitButton.textContent = "Confirmar agendamento";
      updateSubmitState();
    }
  }

  const today = localDateString(new Date());
  dateInput.min = today;
  dateInput.value = today;

  dateInput.addEventListener("change", () => {
    setMessage("");
    loadAvailability();
  });
  timeSelect.addEventListener("change", updateSubmitState);
  nameInput.addEventListener("input", updateSubmitState);
  phoneInput.addEventListener("input", () => {
    const digits = phoneInput.value.replace(/\D/g, "").slice(0, 11);
    if (digits.length > 6) {
      const splitAt = digits.length > 10 ? 7 : 6;
      phoneInput.value = `(${digits.slice(0, 2)}) ${digits.slice(2, splitAt)}-${digits.slice(splitAt)}`;
    } else if (digits.length > 2) {
      phoneInput.value = `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
    } else {
      phoneInput.value = digits;
    }
    updateSubmitState();
  });
  form.addEventListener("submit", submitBooking);

  updateSubmitState();
  loadBarbers();
})();
