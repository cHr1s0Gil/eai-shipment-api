const API_BASE = "/api/shipments";
const STATUSES = ["RECEIVED", "PROCESSING", "SUCCESS", "FAILED"];

const state = {
    filter: "ALL",
    shipments: [],
    selectedId: null
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

    elements.summaryTiles.forEach((tile) => {
        tile.addEventListener("click", () => {
            const status = tile.dataset.status;
            loadShipments(status);
        });
    });
}

async function loadShipments(status) {
    state.filter = status;
    state.selectedId = null;
    setActiveFilter(status);
    setListMessage("데이터를 불러오는 중입니다.");
    renderDetail(null);

    try {
        const url = status === "ALL" ? API_BASE : `${API_BASE}/status/${status}`;
        const response = await requestJson(url);
        state.shipments = Array.isArray(response.data) ? response.data : [];
        renderShipments(state.shipments);
        await updateSummaryCounts();
        setListMessage(response.message || "출고 지시 목록 조회 성공");
    } catch (error) {
        state.shipments = [];
        renderShipments([]);
        await updateSummaryCounts();
        setListMessage(error.message);
    }
}

async function loadShipmentDetail(id) {
    state.selectedId = id;
    markSelectedRow(id);
    elements.detailMessage.textContent = "상세 데이터를 불러오는 중입니다.";

    try {
        const response = await requestJson(`${API_BASE}/${id}`);
        renderDetail(response.data);
        elements.detailMessage.textContent = response.message || "출고 지시 상세 조회 성공";
    } catch (error) {
        renderDetail(null);
        elements.detailMessage.textContent = error.message;
    }
}

async function submitCreateForm(event) {
    event.preventDefault();
    setFormMessage("등록 중입니다.", "");

    const payload = Object.fromEntries(new FormData(elements.createForm).entries());
    payload.quantity = Number(payload.quantity);

    try {
        const response = await requestJson(API_BASE, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        setFormMessage(response.message || "출고 지시 수신 성공", "success");
        elements.createForm.reset();
        await loadShipments(state.filter);
    } catch (error) {
        setFormMessage(error.message, "error");
    }
}

async function requestJson(url, options = {}) {
    const response = await fetch(url, options);
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : null;

    if (!response.ok || body?.status === "E") {
        throw new Error(body?.message || `요청 실패 (${response.status})`);
    }

    return body;
}

function renderShipments(shipments) {
    if (shipments.length === 0) {
        elements.tableBody.innerHTML = `
            <tr class="empty-row">
                <td colspan="8">조회된 출고 지시가 없습니다.</td>
            </tr>
        `;
        return;
    }

    elements.tableBody.innerHTML = shipments.map((shipment) => `
        <tr data-id="${escapeHtml(shipment.id)}">
            <td>${escapeHtml(shipment.id)}</td>
            <td>${escapeHtml(shipment.shipmentNo)}</td>
            <td>${escapeHtml(shipment.orderNo)}</td>
            <td>${escapeHtml(shipment.customerName)}</td>
            <td>${escapeHtml(shipment.materialName)}</td>
            <td>${escapeHtml(shipment.quantity)}</td>
            <td>${renderStatus(shipment.status)}</td>
            <td>${formatDateTime(shipment.requestedAt)}</td>
        </tr>
    `).join("");

    elements.tableBody.querySelectorAll("tr[data-id]").forEach((row) => {
        row.addEventListener("click", () => loadShipmentDetail(row.dataset.id));
    });
}

function renderDetail(detail) {
    if (!detail) {
        elements.detailList.innerHTML = detailRows({
            "출고번호": "-",
            "상태": "-",
            "고객": "-",
            "창고": "-",
            "품목": "-",
            "수량": "-",
            "요청일시": "-",
            "수정일시": "-"
        });
        return;
    }

    elements.detailList.innerHTML = detailRows({
        "출고번호": detail.shipmentNo,
        "상태": renderStatus(detail.status),
        "고객": `${valueOrDash(detail.customerCode)} / ${valueOrDash(detail.customerName)}`,
        "창고": detail.warehouseCode,
        "품목": `${valueOrDash(detail.materialCode)} / ${valueOrDash(detail.materialName)}`,
        "수량": `${valueOrDash(detail.quantity)} ${valueOrDash(detail.unit)}`,
        "요청일시": formatDateTime(detail.requestedAt),
        "수정일시": formatDateTime(detail.updatedAt)
    });
}

function detailRows(rows) {
    return Object.entries(rows).map(([label, value]) => `
        <div>
            <dt>${escapeHtml(label)}</dt>
            <dd>${String(value).includes("<span") ? value : escapeHtml(value)}</dd>
        </div>
    `).join("");
}

async function updateSummaryCounts() {
    let shipments = state.shipments;

    if (state.filter !== "ALL") {
        try {
            const response = await requestJson(API_BASE);
            shipments = Array.isArray(response.data) ? response.data : [];
        } catch (error) {
            shipments = [];
        }
    }

    const counts = STATUSES.reduce((acc, status) => {
        acc[status] = 0;
        return acc;
    }, {});

    shipments.forEach((shipment) => {
        if (counts[shipment.status] !== undefined) {
            counts[shipment.status] += 1;
        }
    });

    elements.countAll.textContent = shipments.length;
    elements.countReceived.textContent = counts.RECEIVED;
    elements.countProcessing.textContent = counts.PROCESSING;
    elements.countSuccess.textContent = counts.SUCCESS;
    elements.countFailed.textContent = counts.FAILED;
}

function setActiveFilter(status) {
    elements.currentFilterLabel.textContent = status;
    elements.summaryTiles.forEach((tile) => {
        tile.classList.toggle("active", tile.dataset.status === status);
    });
}

function markSelectedRow(id) {
    elements.tableBody.querySelectorAll("tr[data-id]").forEach((row) => {
        row.classList.toggle("selected", row.dataset.id === String(id));
    });
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

function renderStatus(status) {
    const safeStatus = escapeHtml(status || "-");
    return `<span class="status-badge status-${safeStatus}">${safeStatus}</span>`;
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

function valueOrDash(value) {
    return value === null || value === undefined || value === "" ? "-" : value;
}

function escapeHtml(value) {
    return String(valueOrDash(value))
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
