const ShipmentApi = (() => {
    const API_BASE = "/api/shipments";

    async function requestJson(url, options = {}) {
        const response = await fetch(url, options);
        const contentType = response.headers.get("content-type") || "";
        const body = contentType.includes("application/json") ? await response.json() : null;

        if (!response.ok || body?.resultCode === "E") {
            throw new Error(body?.message || `Request failed (${response.status})`);
        }

        return body;
    }

    function getShipments(status) {
        const url = status === "ALL" ? API_BASE : `${API_BASE}/status/${status}`;
        return requestJson(url);
    }

    function getShipmentDetail(id) {
        return requestJson(`${API_BASE}/${id}`);
    }

    function createShipment(payload) {
        return requestJson(API_BASE, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
    }

    function updateShipmentStatus(id, payload) {
        return requestJson(`${API_BASE}/${id}/status`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
    }

    function retryShipment(id) {
        return requestJson(`${API_BASE}/${id}/retry`, {
            method: "POST"
        });
    }

    return {
        getShipments,
        getShipmentDetail,
        createShipment,
        updateShipmentStatus,
        retryShipment
    };
})();
