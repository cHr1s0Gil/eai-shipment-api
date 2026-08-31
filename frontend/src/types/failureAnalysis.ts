export type FailureAnalysisStatus =
    | "PENDING"
    | "COMPLETE"
    | "FAILED"

export type RetryDecision =
    | "RETRY"
    | "CHECK_REQUIRED"
    | "DO_NOT_RETRY"

export interface RetryRecommendation {
    decision: RetryDecision,
    reason: string
}

export interface FailureAnalysisResult {
    summary: string,
    possibleCauses: string[],
    checks: string[],
    retryRecommendation: RetryRecommendation
}

export interface FailureAnalysisResponse {
    analysisId: number,
    shipmentId: number,
    dispatchBatchId: string,
    retryCount: number,
    failureMessage: string | null,
    status: FailureAnalysisStatus,
    analysisResult: string | null,
    analyzerName: string,
    analysisErrorMessage: string | null,
    createdAt: string,
    updatedAt: string
}