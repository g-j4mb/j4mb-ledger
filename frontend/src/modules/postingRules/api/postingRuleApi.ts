import { apiClient } from '@/shell/api/client'
import type { PostingRule } from '../types'
import type { Page } from '@/shell/api/types'

export const postingRuleApi = {
  list: () => apiClient.get<Page<PostingRule>>('/api/v1/posting-rules').then((r) => r.data),
}
