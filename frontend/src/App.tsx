import { useMemo } from "react"
import { useGameStateStream } from "./hooks/useGameState"
import { percent, formatClock } from "./lib/format"
import type { Cell, GameState } from "./types/game"

function cellKey(cell: Cell) {
  return `${cell.x},${cell.y}`
}

async function startScout(hunterId: string, cell: { x: number; y: number }) {
  await fetch("/api/commands/missions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      type: "SCOUT",
      hunterId,
      targetCell: cell,
    }),
  })
}

export default function App() {
  const { gameState, connectionStatus } = useGameStateStream()

  const presenceByMonsterId = useMemo(() => {
    const map = new Map<string, number>()
    if (!gameState) return map
    for (const p of gameState.presences) map.set(p.monsterId, p.presence)
    return map
  }, [gameState])

  const huntersByCell = useMemo(() => {
    const map = new Map<string, GameState["hunters"]>()
    if (!gameState) return map

    for (const h of gameState.hunters) {
      const key = cellKey(h.cell)
      const existing = map.get(key) ?? []
      map.set(key, [...existing, h])
    }
    return map
  }, [gameState])

  const presencesByCell = useMemo(() => {
    const map = new Map<string, GameState["presences"]>()
    if (!gameState) return map

    for (const p of gameState.presences) {
      const key = cellKey(p.cell)
      const existing = map.get(key) ?? []
      map.set(key, [...existing, p])
    }
    return map
  }, [gameState])

  const containerStyle: React.CSSProperties = {
    padding: "1rem",
    fontFamily: "system-ui, sans-serif",
    maxWidth: 1100,
    margin: "0 auto",
  }

  const cardStyle: React.CSSProperties = {
    border: "1px solid #e5e7eb",
    borderRadius: 12,
    padding: "0.75rem 1rem",
    marginTop: "1rem",
  }

  const tableStyle: React.CSSProperties = {
    width: "100%",
    borderCollapse: "collapse",
    marginTop: "0.5rem",
  }

  const thStyle: React.CSSProperties = {
    textAlign: "left",
    fontSize: 12,
    letterSpacing: "0.02em",
    padding: "0.5rem",
    borderBottom: "1px solid #e5e7eb",
    color: "#374151",
    whiteSpace: "nowrap",
  }

  const tdStyle: React.CSSProperties = {
    padding: "0.5rem",
    borderBottom: "1px solid #f3f4f6",
    verticalAlign: "middle",
  }

  const badgeStyle = (status: string): React.CSSProperties => ({
    display: "inline-block",
    padding: "0.15rem 0.5rem",
    borderRadius: 999,
    fontSize: 12,
    border: "1px solid #e5e7eb",
    background: "#f9fafb",
  })

  const cellSize = 88

  const gridStyle: React.CSSProperties = gameState
    ? {
      display: "grid",
      gridTemplateColumns: `repeat(${gameState.map.width}, ${cellSize}px)`,
      gridTemplateRows: `repeat(${gameState.map.height}, ${cellSize}px)`,
      gap: 8,
      marginTop: 10,
    }
    : {}

  const gridCellBase: React.CSSProperties = {
    border: "1px solid #e5e7eb",
    borderRadius: 12,
    background: "#fafafa",
    padding: 8,
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
    overflow: "hidden",
  }

  const tokenStyle: React.CSSProperties = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 30,
    height: 30,
    borderRadius: 10,
    border: "1px solid #e5e7eb",
    background: "#ffffff",
    fontSize: 12,
    fontWeight: 600,
  }

  return (
    <div style={containerStyle}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 12,
          flexWrap: "wrap",
        }}
      >
        <div>
          <h1 style={{ margin: 0 }}>Hunters Registry</h1>

          <div style={{ marginTop: 6, color: "#4b5563" }}>
            {gameState ? (
              <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
                <span
                  style={{
                    fontSize: 12,
                    letterSpacing: "0.02em",
                    textTransform: "uppercase",
                  }}
                >
                  Day <strong>{gameState.time.day}</strong>
                </span>

                <span
                  style={{
                    fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
                    fontWeight: 600,
                  }}
                >
                  {formatClock(gameState.time.hour, gameState.time.minute)}
                </span>
              </div>
            ) : (
              <>Waiting for state…</>
            )}
          </div>
        </div>

        <div style={{ alignSelf: "center" }}>
          <span style={badgeStyle(connectionStatus)}>
            SSE: <strong>{connectionStatus}</strong>
          </span>
        </div>
      </header>

      {/* Map */}
      <section style={cardStyle}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 12 }}>
          <h2 style={{ margin: 0, fontSize: 16 }}>Map</h2>
          {gameState && (
            <div style={{ color: "#6b7280", fontSize: 12 }}>
              {gameState.map.width} × {gameState.map.height}
            </div>
          )}
        </div>

        {gameState ? (
          <>
            <div style={gridStyle}>
              {Array.from({ length: gameState.map.height }).map((_, y) =>
                Array.from({ length: gameState.map.width }).map((_, x) => {
                  const key = `${x},${y}`
                  const cellHunters = huntersByCell.get(key) ?? []
                  const cellPresences = presencesByCell.get(key) ?? []

                  const hasHunters = cellHunters.length > 0
                  const hasPresences = cellPresences.length > 0

                  return (
                    <div
                      key={key}
                      style={{
                        ...gridCellBase,
                        background: hasPresences ? "#fff7ed" : hasHunters ? "#eff6ff" : "#fafafa",
                      }}
                      aria-label={`Cell ${x},${y}`}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "#6b7280" }}>
                        <span>{x},{y}</span>
                        <span>
                          {hasHunters ? `H:${cellHunters.length}` : ""}
                          {hasHunters && hasPresences ? "  " : ""}
                          {hasPresences ? `P:${cellPresences.length}` : ""}
                        </span>
                      </div>

                      <div style={{ display: "flex", gap: 10, justifyContent: "center", alignItems: "center" }}>
                        {cellHunters.map((h) => (
                          <span key={h.id} style={tokenStyle} title={h.name}>
                            H
                          </span>
                        ))}

                        {cellPresences.map((p) => {
                          const pct = percent(p.presence)
                          return (
                            <span
                              key={p.monsterId}
                              style={{ ...tokenStyle, position: "relative" }}
                              title={`Presence ${pct}%`}
                            >
                              P
                              <span
                                style={{
                                  position: "absolute",
                                  bottom: -6,
                                  right: -6,
                                  fontSize: 10,
                                  padding: "0 6px",
                                  borderRadius: 999,
                                  border: "1px solid #e5e7eb",
                                  background: "#ffffff",
                                  color: "#374151",
                                  lineHeight: "16px",
                                  height: 16,
                                  display: "inline-flex",
                                  alignItems: "center",
                                }}
                              >
                                {pct}
                              </span>
                            </span>
                          )
                        })}
                      </div>

                      <div style={{ fontSize: 11, color: "#9ca3af", textAlign: "center" }}>
                        {hasHunters || hasPresences ? "" : "empty"}
                      </div>
                    </div>
                  )
                })
              )}
            </div>

            <div style={{ marginTop: 10, color: "#6b7280", fontSize: 12 }}>
              Legend:{" "}
              <span style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" }}>H</span>{" "}
              = Hunter,{" "}
              <span style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace" }}>P</span>{" "}
              = Presence (percent shown)
            </div>
          </>
        ) : (
          <p style={{ color: "#4b5563" }}>Waiting for map…</p>
        )}
      </section>

      {/* Hunters */}
      <section style={cardStyle}>
        <h2 style={{ margin: 0, fontSize: 16 }}>Hunters</h2>
        {gameState ? (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>Name</th>
                <th style={thStyle}>Skill</th>
                <th style={thStyle}>Status</th>
                <th style={thStyle}>Cell</th>
                <th style={thStyle}>Action</th>
              </tr>
            </thead>
            <tbody>
              {gameState.hunters.map((h) => (
                <tr key={h.id}>
                  <td style={tdStyle}>{h.name}</td>
                  <td style={tdStyle}>{h.skill}</td>
                  <td style={tdStyle}>
                    <span style={badgeStyle(h.status)}>{h.status}</span>
                  </td>
                  <td style={tdStyle}>
                    {h.cell.x},{h.cell.y}
                  </td>
                  <td style={tdStyle}>
                    {h.status === "IDLE" ? (
                      <button
                        onClick={() => startScout(h.id, h.cell)}
                        style={{
                          fontSize: 12,
                          padding: "4px 8px",
                          borderRadius: 6,
                          border: "1px solid #e5e7eb",
                          background: "#f9fafb",
                          cursor: "pointer",
                        }}
                      >
                        Scout
                      </button>
                    ) : (
                      <span style={{ color: "#9ca3af", fontSize: 12 }}>Busy</span>
                    )}
                  </td>

                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ color: "#4b5563" }}>No hunters yet.</p>
        )}
      </section>

      {/* Monsters */}
      <section style={cardStyle}>
        <h2 style={{ margin: 0, fontSize: 16 }}>Monsters</h2>
        {gameState ? (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>Type</th>
                <th style={thStyle}>Threat</th>
                <th style={thStyle}>Presence</th>
              </tr>
            </thead>
            <tbody>
              {gameState.monsters.map((m) => {
                const p = presenceByMonsterId.get(m.id) ?? 0
                const pct = percent(p)

                return (
                  <tr key={m.id}>
                    <td style={tdStyle}>{m.type}</td>
                    <td style={tdStyle}>{m.threat}</td>
                    <td style={tdStyle}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div
                          style={{
                            height: 10,
                            width: 180,
                            borderRadius: 999,
                            border: "1px solid #e5e7eb",
                            background: "#f9fafb",
                            overflow: "hidden",
                          }}
                          aria-label={`Presence ${pct}%`}
                        >
                          <div
                            style={{
                              height: "100%",
                              width: `${pct}%`,
                              background: "#111827",
                            }}
                          />
                        </div>
                        <span style={{ fontVariantNumeric: "tabular-nums", width: 44 }}>
                          {pct}%
                        </span>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        ) : (
          <p style={{ color: "#4b5563" }}>No monsters yet.</p>
        )}
      </section>

      {/* Missions */}
      <section style={cardStyle}>
        <h2 style={{ margin: 0, fontSize: 16 }}>Missions</h2>
        {gameState ? (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>Type</th>
                <th style={thStyle}>Hunter</th>
                <th style={thStyle}>Monster</th>
                <th style={thStyle}>Status</th>
              </tr>
            </thead>
            <tbody>
              {gameState.missions.map((ms) => (
                <tr key={ms.id}>
                  <td style={tdStyle}>{ms.type}</td>
                  <td style={tdStyle}>{ms.hunterId}</td>
                  <td style={tdStyle}>{ms.monsterId}</td>
                  <td style={tdStyle}>
                    <span style={badgeStyle(ms.status)}>{ms.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ color: "#4b5563" }}>No missions yet.</p>
        )}
      </section>

      <footer style={{ marginTop: "1rem", color: "#6b7280", fontSize: 12 }} />
    </div>
  )
}
