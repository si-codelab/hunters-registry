import { useEffect, useRef, useState } from "react"
import type { GameState } from "../types/game"

export function useGameStateStream() {
  const [gameState, setGameState] = useState<GameState | null>(null)
  const [connectionStatus, setConnectionStatus] = useState<
    "connecting" | "connected" | "reconnecting"
  >("connecting")

  const lastVersionRef = useRef<number>(-1)

  useEffect(() => {
    const source = new EventSource("/api/state/stream")

    source.onopen = () => setConnectionStatus("connected")
    source.onerror = () => setConnectionStatus("reconnecting")

    const onState = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as GameState

        const v = data.time?.version
        if (typeof v === "number") {
          if (v <= lastVersionRef.current) return
          lastVersionRef.current = v
        }

        setGameState(data)
      } catch (e) {
        console.error("Failed to parse SSE state event", e, event.data)
      }
    }

    source.addEventListener("state", onState as EventListener)

    return () => {
      source.removeEventListener("state", onState as EventListener)
      source.close()
    }
  }, [])

  return { gameState, connectionStatus }
}
