import { useQuery } from '@tanstack/react-query'
import { postingRuleApi } from '../api/postingRuleApi'

export function usePostingRules() {
  return useQuery({ queryKey: ['posting-rules'], queryFn: postingRuleApi.list })
}
