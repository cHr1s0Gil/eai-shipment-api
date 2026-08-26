interface RefreshButtonProps {
  loading: boolean
  disabled?: boolean
  onRefresh: () => void
}

export function RefreshButton({
  loading,
  disabled = false,
  onRefresh,
}: RefreshButtonProps) {
  return (
    <button
      className="button"
      type="button"
      disabled={disabled || loading}
      aria-label="출고지시 데이터 새로고침"
      onClick={onRefresh}
    >
      {loading ? '새로고침 중...' : '새로고침'}
    </button>
  )
}
