const ShipmentApi = (() => {
    const API_BASE = "/api/shipments";
    const API_KEY_STORAGE_KEY = "eai-shipment-api-key";

    function getApiKey() {
        return localStorage.getItem(API_KEY_STORAGE_KEY) || "";
    }

    function setApiKey(apiKey) {
        const normalizedKey = apiKey.trim();

        if (normalizedKey) {
            localStorage.setItem(API_KEY_STORAGE_KEY, normalizedKey);
            return;
        }

        clearApiKey();
    }

    function clearApiKey() {
        localStorage.removeItem(API_KEY_STORAGE_KEY);
    }

    function withApiKeyHeaders(headers = {}) {
        const apiKey = getApiKey();

        if (!apiKey) {
            return headers;
        }

        return {
            ...headers,
            "x-api-key": apiKey
        };
    }

    async function requestJson(url, options = {}) {
        const requestOptions = {
            ...options,
            headers: withApiKeyHeaders(options.headers || {})
        };
        const response = await fetch(url, requestOptions);
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

    // Manual status changes are intentionally disabled.
    // function updateShipmentStatus(id, payload) {
    //     return requestJson(`${API_BASE}/${id}/status`, {
    //         method: "PATCH",
    //         headers: {
    //             "Content-Type": "application/json"
    //         },
    //         body: JSON.stringify(payload)
    //     });
    // }

    function retryShipment(id) {
        return requestJson(`${API_BASE}/${id}/retry`, {
            method: "POST"
        });
    }

    function dispatchShipment(id) {
        return requestJson(`${API_BASE}/${id}/dispatch`, {
            method: "POST"
        });
    }

    return {
        getApiKey,
        setApiKey,
        clearApiKey,
        getShipments,
        getShipmentDetail,
        createShipment,
        // updateShipmentStatus,
        retryShipment,
        dispatchShipment
    };
})();
