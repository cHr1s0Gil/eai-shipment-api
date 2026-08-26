import type { ShipmentListItem, ShipmentDetail, ShipmentRetryResult } from "../types/shipment"
import { httpClient } from "./httpClient"

export async function getShipments(apiKey: string): Promise<ShipmentListItem[]> {
    const path = "/api/shipments"

    const response = await httpClient<ShipmentListItem[]>(path, apiKey)

    if (response.data === null)
        throw new Error('서버 응답에 출고지시 목록 데이터가 없습니다.')

    return response.data
}

export async function getShipmentDetail(id: number, apiKey: string): Promise<ShipmentDetail> {
    const path = `/api/shipments/${id}`

    const response = await httpClient<ShipmentDetail>(path, apiKey)

    if (response.data === null)
        throw new Error('서버 응답에 출고지시 상세 데이터가 없습니다.')

    return response.data
}

export async function retryShipment(id: number, apiKey: string): Promise<ShipmentRetryResult> {
    const path = `/api/shipments/${id}/retry`

    const response = await httpClient<ShipmentRetryResult>(path, apiKey, { method: "POST" })

    if (response.data === null)
        throw new Error('서버 응답에 출고지시 재처리 결과가 없습니다.')

    return response.data
}