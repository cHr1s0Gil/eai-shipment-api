import type { ShipmentDetail } from '../types/shipment'

interface RetryButtonProps {
  shipment: ShipmentDetail
  loading: boolean
  onRetry: (id: number) => void
}

export function RetryButton({
  shipment,
  loading,
  onRetry,
}: RetryButtonProps) {
  const retryable = shipment.status === 'FAILED'

  return (
    <button
      className="button button-danger"
      type="button"
      disabled={!retryable || loading}
      onClick={() => onRetry(shipment.id)}
    >
      {loading ? '재처리 중...' : '재처리'}
    </button>
  )
}
