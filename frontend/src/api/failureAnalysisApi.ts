import type { FailureAnalysisResponse } from "../types/failureAnalysis"
import { httpClient } from "./httpClient"

export async function analyzeShipmentFailure(shipmentId: number, apiKey: string): Promise<FailureAnalysisResponse> {
    const path = `/api/analyses/shipments/${shipmentId}/failure`

    const response = await httpClient<FailureAnalysisResponse>(path, apiKey, { method: "POST" })

    if (response.data === null) {
        throw new Error('서버 응답에 AI 오류 분석 결과가 없습니다.')
    }

    return response.data
}