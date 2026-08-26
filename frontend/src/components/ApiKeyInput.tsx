import type { FormEvent } from 'react'
import { RefreshButton } from './RefreshButton'

interface ApiKeyInputProps {
  value: string
  loading: boolean
  onChange: (value: string) => void
  onRefresh: () => void
}

export function ApiKeyInput({
  value,
  loading,
  onChange,
  onRefresh,
}: ApiKeyInputProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onRefresh()
  }

  return (
    <form className="api-key-form" onSubmit={handleSubmit}>
      <div className="field-group">
        <label className="field-label" htmlFor="api-key">
          API Key
        </label>
        <input
          id="api-key"
          className="text-input"
          type="password"
          value={value}
          placeholder="x-api-key를 입력하세요"
          autoComplete="off"
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
      <RefreshButton
        loading={loading}
        disabled={value.trim().length === 0}
        onRefresh={onRefresh}
      />
    </form>
  )
}
