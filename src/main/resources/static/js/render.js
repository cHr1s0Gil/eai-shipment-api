const ShipmentRender = (() => {
    const STATUSES = ["RECEIVED", "PROCESSING", "SUCCESS", "FAILED"];

    function renderShipments(elements, shipments, onSelect) {
        if (shipments.length === 0) {
            elements.tableBody.innerHTML = `
                <tr class="empty-row">
                    <td colspan="8">No shipment data found.</td>
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
            row.addEventListener("click", () => onSelect(row.dataset.id));
        });
    }

    function renderDetail(elements, detail) {
        if (!detail) {
            elements.detailList.innerHTML = detailRows({
                "Shipment No": "-",
                "Status": "-",
                "Customer": "-",
                "Warehouse": "-",
                "Material": "-",
                "Quantity": "-",
                "Message": "-",
                "Requested": "-",
                "Updated": "-"
            });
            return;
        }

        elements.detailList.innerHTML = detailRows({
            "Shipment No": detail.shipmentNo,
            "Status": renderStatus(detail.status),
            "Customer": `${valueOrDash(detail.customerCode)} / ${valueOrDash(detail.customerName)}`,
            "Warehouse": detail.warehouseCode,
            "Material": `${valueOrDash(detail.materialCode)} / ${valueOrDash(detail.materialName)}`,
            "Quantity": `${valueOrDash(detail.quantity)} ${valueOrDash(detail.unit)}`,
            "Message": detail.message,
            "Requested": formatDateTime(detail.requestedAt),
            "Updated": formatDateTime(detail.updatedAt)
        });
    }

    function renderSummary(elements, shipments) {
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

    function setActiveFilter(elements, status) {
        elements.currentFilterLabel.textContent = status;
        elements.summaryTiles.forEach((tile) => {
            tile.classList.toggle("active", tile.dataset.status === status);
        });
    }

    function markSelectedRow(elements, id) {
        elements.tableBody.querySelectorAll("tr[data-id]").forEach((row) => {
            row.classList.toggle("selected", row.dataset.id === String(id));
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

    return {
        renderShipments,
        renderDetail,
        renderSummary,
        setActiveFilter,
        markSelectedRow
    };
})();
