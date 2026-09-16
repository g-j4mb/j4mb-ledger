import { Component, type ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { hasError: boolean; message: string }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: '' }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex h-full min-h-[200px] items-center justify-center p-8">
          <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center max-w-md">
            <p className="text-sm font-medium text-red-700 mb-1">Something went wrong</p>
            <p className="text-xs text-red-600">{this.state.message}</p>
            <button
              className="mt-4 text-xs text-red-600 underline"
              onClick={() => this.setState({ hasError: false, message: '' })}
            >
              Try again
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
