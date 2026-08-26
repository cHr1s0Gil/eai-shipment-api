import type { ShipmentDetail } from '../types/shipment'
import { RetryButton } from './RetryButton'
import { StatusBadge } from './StatusBadge'

interface ShipmentDetailPanelProps {
  shipment: ShipmentDetail | null
  loading: boolean
  retrying: boolean
  onRetry: (id: number) => void
}

function formatDateTime(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('ko-KR')
}

export function ShipmentDetailPanel({
  shipment,
  loading,
  retrying,
  onRetry,
}: ShipmentDetailPanelProps) {
  if (loading) {
    return (
      <section className="panel" aria-labelledby="shipment-detail-title">
        <div className="panel-header">
          <h2 className="panel-title" id="shipment-detail-title">
            출고지시 상세
          </h2>
        </div>
        <div className="loading-state">상세 정보를 불러오는 중입니다.</div>
      </section>
    )
  }

  if (shipment === null) {
    return (
      <section className="panel" aria-labelledby="shipment-detail-title">
        <div className="panel-header">
          <h2 className="panel-title" id="shipment-detail-title">
            출고지시 상세
          </h2>
        </div>
        <div className="empty-state">목록에서 출고지시를 선택하세요.</div>
      </section>
    )
  }

  return (
    <section className="panel" aria-labelledby="shipment-detail-title">
      <div className="panel-header">
        <h2 className="panel-title" id="shipment-detail-title">
          출고지시 상세
        </h2>
        <StatusBadge status={shipment.status} />
      </div>

      <div className="panel-body">
        <dl className="detail-grid">
          <div className="detail-row">
            <dt>출고번호</dt>
            <dd>{shipment.shipmentNo}</dd>
          </div>
          <div className="detail-row">
            <dt>주문번호</dt>
            <dd>{shipment.orderNo}</dd>
          </div>
          <div className="detail-row">
            <dt>Dispatch Batch ID</dt>
            <dd>{shipment.dispatchBatchId ?? '-'}</dd>
          </div>
          <div className="detail-row">
            <dt>창고</dt>
            <dd>{shipment.warehouseCode}</dd>
          </div>
          <div className="detail-row">
            <dt>고객</dt>
            <dd>
              {shipment.customerCode} / {shipment.customerName}
            </dd>
          </div>
          <div className="detail-row">
            <dt>품목</dt>
            <dd>
              {shipment.materialCode} / {shipment.materialName}
            </dd>
          </div>
          <div className="detail-row">
            <dt>수량</dt>
            <dd>
              {shipment.quantity.toLocaleString()} {shipment.unit}
            </dd>
          </div>
          <div className="detail-row">
            <dt>재처리 횟수</dt>
            <dd>{shipment.retryCount.toLocaleString()}</dd>
          </div>
          <div className="detail-row">
            <dt>요청일시</dt>
            <dd>{formatDateTime(shipment.requestedAt)}</dd>
          </div>
          <div className="detail-row">
            <dt>생성일시</dt>
            <dd>{formatDateTime(shipment.createdAt)}</dd>
          </div>
          <div className="detail-row">
            <dt>수정일시</dt>
            <dd>{formatDateTime(shipment.updatedAt)}</dd>
          </div>
        </dl>

        {shipment.message && (
          <div className="detail-message" role="status">
            {shipment.message}
          </div>
        )}
      </div>

      <div className="panel-actions">
        <RetryButton
          shipment={shipment}
          loading={retrying}
          onRetry={onRetry}
        />
      </div>
    </section>
  )
}
