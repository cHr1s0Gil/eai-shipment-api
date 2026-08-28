import { useState, useEffect } from "react";
import "./App.css";

import { ApiKeyInput } from "./components/ApiKeyInput";
import { ShipmentList } from "./components/ShipmentList";
import { ShipmentDetailPanel } from "./components/ShipmentDetailPanel";

import {
  getShipmentDetail,
  getShipments,
  retryShipment,
} from "./api/shipmentApi";

import type { ShipmentDetail, ShipmentListItem } from "./types/shipment";

function App() {
  const [apiKey, setApiKey] = useState("");

  const [shipments, setShipments] = useState<ShipmentListItem[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedShipment, setSelectedShipment] =
    useState<ShipmentDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSelectShipment(id: number) {
    setSelectedId(id);
    setDetailLoading(true);

    try {
      const result = await getShipmentDetail(id, apiKey);
      setSelectedShipment(result);
    } catch (error: unknown) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleRetry(id: number) {
    setError(null);
    setRetrying(true);

    try {
      await retryShipment(id, apiKey);

      const shipmentListResult = await getShipments(apiKey);
      setShipments(shipmentListResult);

      const detailResult = await getShipmentDetail(id, apiKey);
      setSelectedShipment(detailResult);
    } catch (error: unknown) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setRetrying(false);
    }
  }

  async function handleRefresh() {
    setError(null);
    setLoading(true);

    try {
      const shipmentListResult = await getShipments(apiKey);
      setShipments(shipmentListResult);

      if (selectedId !== null) {
        setDetailLoading(true);
        const shipmentDetailResult = await getShipmentDetail(
          selectedId,
          apiKey,
        );
        setSelectedShipment(shipmentDetailResult);
      }
    } catch (error: unknown) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
      setDetailLoading(false);
    }
  }

  useEffect(() => {
    if (
      apiKey.trim() === "" ||
      selectedId === null ||
      selectedShipment?.status !== "PROCESSING"
    )
      return;

    const pollingShipmentId = selectedId;
    let requestInProgress = false;
    let cancelled = false;

    const pollShipmentStatus = async () => {
      if (requestInProgress) return;

      requestInProgress = true;

      try {
        const detailResult = await getShipmentDetail(pollingShipmentId, apiKey);

        if (cancelled) return;

        if (detailResult.status === "PROCESSING") {
          setSelectedShipment(detailResult);
          setError(null);
          return;
        }

        const shipmentListResult = await getShipments(apiKey);

        if (cancelled) return;

        setSelectedShipment(detailResult);
        setShipments(shipmentListResult);
        setError(null);
      } catch (error: unknown) {
        if (cancelled) return;

        if (error instanceof Error) {
          setError(error.message);
        } else {
          setError("상태를 갱신하는 중 알 수 없는 오류가 발생했습니다.");
        }
      } finally {
        requestInProgress = false;
      }
    };

    const timer = window.setInterval(() => {
      void pollShipmentStatus();
    }, 2000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [apiKey, selectedId, selectedShipment?.status]);

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <h1 className="app-title">Shipment Management</h1>
          <p className="app-subtitle">
            출고지시 처리 상태와 실패 내역을 관리합니다.
          </p>
        </div>

        <ApiKeyInput
          value={apiKey}
          loading={loading}
          onChange={setApiKey}
          onRefresh={handleRefresh}
        />
      </header>

      {error !== null && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}

      <div className="dashboard">
        <section className="panel">
          <div className="panel-header">
            <h2 className="panel-title">출고지시 목록</h2>
            <span className="panel-meta">{shipments.length}건</span>
          </div>

          <ShipmentList
            shipments={shipments}
            selectedId={selectedId}
            onSelect={handleSelectShipment}
          />
        </section>

        <ShipmentDetailPanel
          shipment={selectedShipment}
          loading={detailLoading}
          retrying={retrying}
          onRetry={handleRetry}
        />
      </div>
    </main>
  );
}

export default App;
