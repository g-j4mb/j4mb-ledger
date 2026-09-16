import { apiClient } from '@/shell/api/client'
import type { CoaNode } from '../types'

// GET /api/v1/coa is a lazy per-level endpoint (optional ?parentId=), not a
// full-tree endpoint — it returns only the direct children of the given
// parent (or the roots, when parentId is omitted). We walk it recursively
// here so the rest of the module can keep working with one flat array.
async function fetchLevel(parentId?: string): Promise<CoaNode[]> {
  const url = parentId ? `/api/v1/coa?parentId=${parentId}` : '/api/v1/coa'
  const nodes = await apiClient.get<CoaNode[]>(url).then((r) => r.data)
  const childLists = await Promise.all(nodes.map((n) => fetchLevel(n.id)))
  return [...nodes, ...childLists.flat()]
}

export const coaApi = {
  list: () => fetchLevel(),
}
