import type { FailureAnalysisResult } from "../types/failureAnalysis"

export function parseFailureAnalysisResult(
  payload: string | null,
): FailureAnalysisResult | null {
  if (payload === null || payload.trim() === "") {
    return null
  }

  try {
    return JSON.parse(payload) as FailureAnalysisResult
  } catch {
    return null
  }
}