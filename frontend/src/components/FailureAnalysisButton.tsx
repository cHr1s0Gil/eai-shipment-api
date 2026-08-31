import type { ShipmentDetail } from '../types/shipment'

interface FailureAnalysisButtonProps {
  shipment: ShipmentDetail
  loading: boolean
  disabled?: boolean
  onAnalyze: (id: number) => void
}

export function FailureAnalysisButton({
  shipment,
  loading,
  disabled = false,
  onAnalyze,
}: FailureAnalysisButtonProps) {
  const analyzable = shipment.status === 'FAILED'

  return (
    <button
      className="button button-ai"
      type="button"
      disabled={!analyzable || loading || disabled}
      title={analyzable ? undefined : 'FAILED 상태에서만 AI 오류 분석이 가능합니다.'}
      onClick={() => onAnalyze(shipment.id)}
    >
      {loading ? 'AI 분석 중...' : 'AI 오류 분석'}
    </button>
  )
}
