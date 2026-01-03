export type Hunter = {
  id: string
  name: string
  skill: number
  status: string
  cell: Cell
}

export type Monster = {
  id: string
  type: string
  threat: number
  cell: Cell
}

export type MonsterPresence = {
  monsterId: string
  presence: number
}

export type Mission = {
  id: string
  type: string
  hunterId: string
  monsterId: string
  status: string
}

export type GameTime = {
  version: number
  minute: number
  day: number
  hour: number
}

export type Cell = { x: number; y: number }
export type GameMap = { width: number; height: number }

export type GameState = {
  time: GameTime
  map: GameMap
  hunters: Hunter[]
  monsters: Monster[]
  presences: MonsterPresence[]
  missions: Mission[]
}
