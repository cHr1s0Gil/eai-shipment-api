export type ShipmentStatus =
  | "RECEIVED"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"

export interface ApiResponse<T> {
  resultCode: 'S' | 'E'
  message: string
  data: T | null
}

export interface ShipmentListItem {
  id: number
  shipmentNo: string
  orderNo: string
  customerName: string
  materialName: string
  quantity: number
  status: ShipmentStatus
  requestedAt: string
  createdAt: string
}

export interface ShipmentDetail {
  id: number
  shipmentNo: string
  orderNo: string
  dispatchBatchId: string | null
  requestedAt: string
  warehouseCode: string
  customerCode: string
  customerName: string
  materialCode: string
  materialName: string
  quantity: number
  unit: string
  status: ShipmentStatus
  message: string | null
  createdAt: string
  updatedAt: string
  retryCount: number
}

export interface ShipmentRetryResult {
  shipmentNo: string,
  status: ShipmentStatus,
  retryCount: number,
  message: string | null
}
