import type { ShipmentStatus } from '../types/shipment'

interface StatusBadgeProps {
  status: ShipmentStatus
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const statusClass = `status-${status.toLowerCase()}`

  return (
    <span className={`status-badge ${statusClass}`}>
      {status}
    </span>
  )
}