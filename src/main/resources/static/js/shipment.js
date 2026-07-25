const state = {
    filter: "ALL",
    shipments: [],
    selectedId: null,
    selectedDetail: null
};

const elements = {
    tableBody: document.querySelector("#shipmentTableBody"),
    listMessage: document.querySelector("#listMessage"),
    detailMessage: document.querySelector("#detailMessage"),
    detailList: document.querySelector("#detailList"),
    currentFilterLabel: document.querySelector("#currentFilterLabel"),
    summaryTiles: document.querySelectorAll(".summary-tile"),
    refreshButton: document.querySelector("#refreshButton"),
    openCreateFormButton: document.querySelector("#openCreateFormButton"),
    closeCreateFormButton: document.querySelector("#closeCreateFormButton"),
    resetFormButton: document.querySelector("#resetFormButton"),
    createDialog: document.querySelector("#createDialog"),
    createForm: document.querySelector("#createForm"),
    formMessage: document.querySelector("#formMessage"),
    dispatchButton: document.querySelector("#dispatchButton"),
    retryButton: document.querySelector("#retryButton"),
    actionMessage: document.querySelector("#actionMessage"),
    apiKeyInput: document.querySelector("#apiKeyInput"),
    saveApiKeyButton: document.querySelector("#saveApiKeyButton"),
    clearApiKeyButton: document.querySelector("#clearApiKeyButton"),
    apiKeyStatus: document.querySelector("#apiKeyStatus"),
    countAll: document.querySelector("#countAll"),
    countReceived: document.querySelector("#countReceived"),
    countProcessing: document.querySelector("#countProcessing"),
    countSuccess: document.querySelector("#countSuccess"),
    countFailed: document.querySelector("#countFailed")
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    hydrateApiKeyControl();
    loadShipments("ALL");
});

function bindEvents() {
    elements.refreshButton.addEventListener("click", () => loadShipments(state.filter));
    elements.openCreateFormButton.addEventListener("click", openCreateDialog);
    elements.closeCreateFormButton.addEventListener("click", closeCreateDialog);
    elements.resetFormButton.addEventListener("click", resetCreateForm);
    elements.createForm.addEventListener("submit", submitCreateForm);
    elements.dispatchButton.addEventListener("click", dispatchSelectedShipment);
    elements.retryButton.addEventListener("click", retrySelectedShipment);
    elements.saveApiKeyButton.addEventListener("click", saveApiKey);
    elements.clearApiKeyButton.addEventListener("click", clearApiKey);
    elements.apiKeyInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            saveApiKey();
        }
    });

    elements.summaryTiles.forEach((tile) => {
        tile.addEventListener("click", () => {
            loadShipments(tile.dataset.status);
        });
    });
}

function hydrateApiKeyControl() {
    const apiKey = ShipmentApi.getApiKey();
    elements.apiKeyInput.value = apiKey;
    setApiKeyStatus(apiKey ? "Key saved" : "Key not set", apiKey ? "success" : "");
}

function saveApiKey() {
    ShipmentApi.setApiKey(elements.apiKeyInput.value);
    hydrateApiKeyControl();
    loadShipments(state.filter);
}

function clearApiKey() {
    ShipmentApi.clearApiKey();
    hydrateApiKeyControl();
    loadShipments(state.filter);
}

function setApiKeyStatus(message, type) {
    elements.apiKeyStatus.textContent = message;
    elements.apiKeyStatus.className = `api-key-status ${type || ""}`.trim();
}
async function loadShipments(status) {
    state.filter = status;
    state.selectedId = null;
    state.selectedDetail = null;
    ShipmentRender.setActiveFilter(elements, status);
    setListMessage("Loading shipment data.");
    ShipmentRender.renderDetail(elements, null);
    setActionControls(false);

    try {
        const response = await ShipmentApi.getShipments(status);
        state.shipments = Array.isArray(response.data) ? response.data : [];
        ShipmentRender.renderShipments(elements, state.shipments, loadShipmentDetail);
        await updateSummaryCounts();
        setListMessage(response.message || "Shipment list returned");
    } catch (error) {
        state.shipments = [];
        ShipmentRender.renderShipments(elements, [], loadShipmentDetail);
        await updateSummaryCounts();
        setListMessage(error.message);
    }
}

async function loadShipmentDetail(id) {
    state.selectedId = id;
    state.selectedDetail = null;
    ShipmentRender.markSelectedRow(elements, id);
    setActionControls(false);
    elements.detailMessage.textContent = "Loading shipment detail.";

    try {
        const response = await ShipmentApi.getShipmentDetail(id);
        state.selectedDetail = response.data;
        ShipmentRender.renderDetail(elements, response.data);
        hydrateActionControls(response.data);
        elements.detailMessage.textContent = response.message || "Shipment detail returned";
    } catch (error) {
        ShipmentRender.renderDetail(elements, null);
        setActionControls(false);
        elements.detailMessage.textContent = error.message;
    }
}

async function submitCreateForm(event) {
    event.preventDefault();
    setFormMessage("Creating shipment.", "");

    const payload = Object.fromEntries(new FormData(elements.createForm).entries());
    payload.quantity = Number(payload.quantity);

    try {
        const response = await ShipmentApi.createShipment(payload);
        setFormMessage(response.message || "Shipment request received", "success");
        elements.createForm.reset();
        await loadShipments(state.filter);
    } catch (error) {
        setFormMessage(error.message, "error");
    }
}

async function retrySelectedShipment() {
    if (!state.selectedId) {
        setActionMessage("Select a shipment before retry.", "error");
        return;
    }

    setActionMessage("Retrying shipment.", "");

    try {
        const response = await ShipmentApi.retryShipment(state.selectedId);
        setActionMessage(response.message || "Shipment retry dispatch requested", "success");
        await afterMutation();
    } catch (error) {
        setActionMessage(error.message, "error");
    }
}

async function dispatchSelectedShipment() {
    if (!state.selectedId) {
        setActionMessage("Select a shipment before dispatch.", "error");
        return;
    }

    setActionMessage("Dispatching shipment.", "");

    try {
        const response = await ShipmentApi.dispatchShipment(state.selectedId);
        setActionMessage(response.message || "Shipment dispatch completed", "success");
        await afterMutation();
    } catch (error) {
        setActionMessage(error.message, "error");
    }
}

async function afterMutation() {
    const selectedId = state.selectedId;
    await loadShipments(state.filter);

    if (selectedId) {
        await loadShipmentDetail(selectedId);
    }
}

async function updateSummaryCounts() {
    let shipments = state.shipments;

    if (state.filter !== "ALL") {
        try {
            const response = await ShipmentApi.getShipments("ALL");
            shipments = Array.isArray(response.data) ? response.data : [];
        } catch (error) {
            shipments = [];
        }
    }

    ShipmentRender.renderSummary(elements, shipments);
}

function hydrateActionControls(detail) {
    setActionControls(true);
    elements.dispatchButton.disabled = detail.status !== "RECEIVED";
    elements.retryButton.disabled = detail.status !== "FAILED";

    if (detail.status === "RECEIVED") {
        setActionMessage("Ready to dispatch this shipment.", "");
        return;
    }

    if (detail.status === "FAILED") {
        setActionMessage("Ready to retry this failed shipment.", "");
        return;
    }

    setActionMessage("This shipment is being processed or already completed.", "");
}

function setActionControls(enabled) {
    elements.dispatchButton.disabled = !enabled;
    elements.retryButton.disabled = !enabled;

    if (!enabled) {
        setActionMessage("", "");
    }
}

function setListMessage(message) {
    elements.listMessage.textContent = message;
}

function openCreateDialog() {
    setFormMessage("", "");
    elements.createDialog.showModal();
}

function closeCreateDialog() {
    elements.createDialog.close();
}

function resetCreateForm() {
    elements.createForm.reset();
    setFormMessage("", "");
}

function setFormMessage(message, type) {
    elements.formMessage.textContent = message;
    elements.formMessage.className = `form-message ${type || ""}`.trim();
}

function setActionMessage(message, type) {
    elements.actionMessage.textContent = message;
    elements.actionMessage.className = `form-message ${type || ""}`.trim();
}

