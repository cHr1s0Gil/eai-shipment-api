import type {
  FailureAnalysisResponse,
  FailureAnalysisResult,
  RetryDecision,
} from '../types/failureAnalysis'

interface FailureAnalysisResultPanelProps {
  analysis: FailureAnalysisResponse | null
  result: FailureAnalysisResult | null
  loading: boolean
}

const retryDecisionLabels: Record<RetryDecision, string> = {
  RETRY: '재처리 가능',
  CHECK_REQUIRED: '확인 후 재처리',
  DO_NOT_RETRY: '재처리 금지',
}

const retryDecisionClasses: Record<RetryDecision, string> = {
  RETRY: 'decision-retry',
  CHECK_REQUIRED: 'decision-check-required',
  DO_NOT_RETRY: 'decision-do-not-retry',
}

export function FailureAnalysisResultPanel({
  analysis,
  result,
  loading,
}: FailureAnalysisResultPanelProps) {
  if (!loading && analysis === null) {
    return null
  }

  return (
    <section className="analysis-section" aria-labelledby="analysis-title">
      <div className="analysis-header">
        <h3 className="analysis-title" id="analysis-title">
          AI 오류 분석 결과
        </h3>

        {analysis !== null && (
          <span
            className={`analysis-status analysis-status-${analysis.status.toLowerCase()}`}
          >
            {analysis.status}
          </span>
        )}
      </div>

      {loading && (
        <div className="analysis-loading" role="status">
          실패 정보와 처리 이력을 분석하고 있습니다.
        </div>
      )}

      {!loading && analysis?.status === 'PENDING' && (
        <div className="analysis-loading" role="status">
          분석 요청이 처리 대기 중입니다.
        </div>
      )}

      {!loading && analysis?.status === 'FAILED' && (
        <div className="analysis-error" role="alert">
          {analysis.analysisErrorMessage ?? 'AI 오류 분석에 실패했습니다.'}
        </div>
      )}

      {!loading && analysis?.status === 'COMPLETE' && result === null && (
        <div className="analysis-error" role="alert">
          AI 분석 결과를 화면에 표시할 수 없습니다.
        </div>
      )}

      {!loading && analysis?.status === 'COMPLETE' && result !== null && (
        <div className="analysis-content">
          <div>
            <h4 className="analysis-block-title">요약</h4>
            <p className="analysis-summary">{result.summary}</p>
          </div>

          <div>
            <h4 className="analysis-block-title">가능한 원인</h4>
            <ul className="analysis-list">
              {result.possibleCauses.map((cause, index) => (
                <li key={`${index}-${cause}`}>{cause}</li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="analysis-block-title">확인 항목</h4>
            <ol className="analysis-list">
              {result.checks.map((check, index) => (
                <li key={`${index}-${check}`}>{check}</li>
              ))}
            </ol>
          </div>

          <div className="analysis-recommendation">
            <div className="analysis-recommendation-header">
              <h4 className="analysis-block-title">재처리 권고</h4>
              <span
                className={`decision-badge ${retryDecisionClasses[result.retryRecommendation.decision]}`}
              >
                {retryDecisionLabels[result.retryRecommendation.decision]}
              </span>
            </div>
            <p>{result.retryRecommendation.reason}</p>
          </div>
        </div>
      )}

      {!loading && analysis?.status === 'COMPLETE' && (
        <p className="analysis-disclaimer">
          AI 분석은 장애 확인을 돕는 참고 정보이며 실제 원인을 확정하지 않습니다.
        </p>
      )}
    </section>
  )
}
