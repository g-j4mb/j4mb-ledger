import { useQuery } from '@tanstack/react-query'
import { coaApi } from '../api/coaApi'
import type { CoaNode } from '../types'

export function useCoa() {
  return useQuery({ queryKey: ['coa'], queryFn: coaApi.list })
}

export type TreeNode = CoaNode & { children: TreeNode[] }

export function buildTree(nodes: CoaNode[]): TreeNode[] {
  const map = new Map<string, TreeNode>()
  nodes.forEach((n) => map.set(n.id, { ...n, children: [] }))
  const roots: TreeNode[] = []
  nodes.forEach((n) => {
    if (n.parentId) {
      map.get(n.parentId)?.children.push(map.get(n.id)!)
    } else {
      roots.push(map.get(n.id)!)
    }
  })
  return roots
}
