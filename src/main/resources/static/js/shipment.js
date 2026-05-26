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
    statusSelect: document.querySelector("#statusSelect"),
    statusMessageInput: document.querySelector("#statusMessageInput"),
    updateStatusButton: document.querySelector("#updateStatusButton"),
    retryButton: document.querySelector("#retryButton"),
    actionMessage: document.querySelector("#actionMessage"),
    countAll: document.querySelector("#countAll"),
    countReceived: document.querySelector("#countReceived"),
    countProcessing: document.querySelector("#countProcessing"),
    countSuccess: document.querySelector("#countSuccess"),
    countFailed: document.querySelector("#countFailed")
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    loadShipments("ALL");
});

function bindEvents() {
    elements.refreshButton.addEventListener("click", () => loadShipments(state.filter));
    elements.openCreateFormButton.addEventListener("click", openCreateDialog);
    elements.closeCreateFormButton.addEventListener("click", closeCreateDialog);
    elements.resetFormButton.addEventListener("click", resetCreateForm);
    elements.createForm.addEventListener("submit", submitCreateForm);
    elements.updateStatusButton.addEventListener("click", updateSelectedStatus);
    elements.retryButton.addEventListener("click", retrySelectedShipment);

    elements.summaryTiles.forEach((tile) => {
        tile.addEventListener("click", () => {
            loadShipments(tile.dataset.status);
        });
    });
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

async function updateSelectedStatus() {
    if (!state.selectedId) {
        setActionMessage("Select a shipment before updating status.", "error");
        return;
    }

    const payload = {
        status: elements.statusSelect.value
    };

    const message = elements.statusMessageInput.value.trim();
    if (message) {
        payload.message = message;
    }

    setActionMessage("Updating shipment status.", "");

    try {
        const response = await ShipmentApi.updateShipmentStatus(state.selectedId, payload);
        setActionMessage(response.message || "Shipment status updated", "success");
        await afterMutation();
    } catch (error) {
        setActionMessage(error.message, "error");
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
        setActionMessage(response.message || "Shipment retry completed", "success");
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
    elements.statusSelect.value = detail.status || "RECEIVED";
    elements.statusMessageInput.value = detail.message || "";
    elements.retryButton.disabled = detail.status !== "FAILED";
    setActionMessage("", "");
}

function setActionControls(enabled) {
    elements.statusSelect.disabled = !enabled;
    elements.statusMessageInput.disabled = !enabled;
    elements.updateStatusButton.disabled = !enabled;
    elements.retryButton.disabled = true;

    if (!enabled) {
        elements.statusSelect.value = "RECEIVED";
        elements.statusMessageInput.value = "";
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
