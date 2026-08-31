import type { ShipmentDetail } from "../types/shipment"

interface RetryButtonProps {
  shipment: ShipmentDetail
  loading: boolean
  disabled?: boolean
  onRetry: (id: number) => void
}

export function RetryButton({
  shipment,
  loading,
  disabled = false,
  onRetry,
}: RetryButtonProps) {
  const retryable = shipment.status === "FAILED"

  return (
    <button
      className="button button-danger"
      type="button"
      disabled={!retryable || loading || disabled}
      onClick={() => onRetry(shipment.id)}
    >
      {loading ? "재처리 중..." : "재처리"}
    </button>
  )
}
