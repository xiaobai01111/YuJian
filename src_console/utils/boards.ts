export type BoardKey = 'confessions' | 'treehole' | 'help' | 'market' | 'lost-found'

export const BOARD_OPTIONS: { key: BoardKey; label: string; route: string }[] = [
  { key: 'confessions', label: '表白墙', route: '/confessions' },
  { key: 'treehole', label: '树洞', route: '/treehole' },
  { key: 'help', label: '求助问答', route: '/help' },
  { key: 'market', label: '跳蚤市场', route: '/market' },
  { key: 'lost-found', label: '失物招领', route: '/lost-found' }
]

const BOARD_LABELS: Record<BoardKey, string> = {
  confessions: '表白墙',
  treehole: '树洞',
  help: '求助问答',
  market: '跳蚤市场',
  'lost-found': '失物招领'
}

const BOARD_PATHS: Record<BoardKey, string> = {
  confessions: '/confessions',
  treehole: '/treehole',
  help: '/help',
  market: '/market',
  'lost-found': '/lost-found'
}

export const normalizeBoardKey = (value?: string): BoardKey | null => {
  if (!value) return null
  const key = value.trim().toLowerCase()
  if (!key) return null
  if (key === 'confession' || key === 'confessions') return 'confessions'
  if (key === 'tree-hole' || key === 'tree_hole' || key === 'tree hole' || key === 'treehole') return 'treehole'
  if (key === 'help' || key === 'qa' || key === 'qna') return 'help'
  if (key === 'market' || key === 'flea' || key === 'flea-market' || key === 'flea_market') return 'market'
  if (key === 'lostfound' || key === 'lost-found' || key === 'lost_found' || key === 'lost found') return 'lost-found'
  return (['confessions', 'treehole', 'help', 'market', 'lost-found'] as BoardKey[]).includes(key as BoardKey)
    ? (key as BoardKey)
    : null
}

export const normalizeBoardKeys = (values?: string[]): BoardKey[] => {
  if (!values || values.length === 0) return []
  const result: BoardKey[] = []
  values.forEach((value) => {
    const key = normalizeBoardKey(value)
    if (key && !result.includes(key)) {
      result.push(key)
    }
  })
  return result
}

export const getBoardLabel = (value?: string): string => {
  const key = normalizeBoardKey(value)
  return key ? BOARD_LABELS[key] : value || ''
}

export const getBoardPath = (value?: string): string => {
  const key = normalizeBoardKey(value)
  return key ? BOARD_PATHS[key] : '/'
}

export const getPostBoards = (post: { boards?: string[]; board?: string }): BoardKey[] => {
  const boards = post.boards && post.boards.length > 0 ? post.boards : post.board ? [post.board] : []
  return normalizeBoardKeys(boards)
}
