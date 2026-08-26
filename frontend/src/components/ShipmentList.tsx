import type { ShipmentListItem } from '../types/shipment'
import { StatusBadge } from './StatusBadge'

interface ShipmentListProps {
  shipments: ShipmentListItem[]
  selectedId: number | null
  onSelect: (id: number) => void
}

function formatDateTime(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('ko-KR')
}

export function ShipmentList({
  shipments,
  selectedId,
  onSelect,
}: ShipmentListProps) {
  if (shipments.length === 0) {
    return <div className="empty-state">출고지시 데이터가 없습니다.</div>
  }

  return (
    <div className="table-wrap">
      <table className="shipment-table">
        <thead>
          <tr>
            <th>출고번호</th>
            <th>주문번호</th>
            <th>고객</th>
            <th>품목</th>
            <th>수량</th>
            <th>상태</th>
            <th>요청일시</th>
          </tr>
        </thead>
        <tbody>
          {shipments.map((shipment) => {
            const isSelected = shipment.id === selectedId

            return (
              <tr
                key={shipment.id}
                className={isSelected ? 'is-selected' : undefined}
                aria-selected={isSelected}
                tabIndex={0}
                onClick={() => onSelect(shipment.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    onSelect(shipment.id)
                  }
                }}
              >
                <td title={shipment.shipmentNo}>{shipment.shipmentNo}</td>
                <td title={shipment.orderNo}>{shipment.orderNo}</td>
                <td title={shipment.customerName}>{shipment.customerName}</td>
                <td title={shipment.materialName}>{shipment.materialName}</td>
                <td>{shipment.quantity.toLocaleString()}</td>
                <td>
                  <StatusBadge status={shipment.status} />
                </td>
                <td>{formatDateTime(shipment.requestedAt)}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
