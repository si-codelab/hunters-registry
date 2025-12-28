import { useMemo } from "react"
import { useGameStateStream } from "./hooks/useGameState"
import { percent, formatClock } from "./lib/format"
import type { GameState } from "./types/game"

export default function App() {
  const { gameState, connectionStatus } = useGameStateStream()

  const presenceByMonsterId = useMemo(() => {
    const map = new Map<string, number>()
    if (!gameState) return map
    for (const p of gameState.presences) map.set(p.monsterId, p.presence)
    return map
  }, [gameState])

  const containerStyle: React.CSSProperties = {
    padding: "1rem",
    fontFamily: "system-ui, sans-serif",
    maxWidth: 1000,
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
              <div
                style={{
                  display: "flex",
                  alignItems: "baseline",
                  gap: 12,
                }}
              >
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
                    fontFamily:
                      "ui-monospace, SFMono-Regular, Menlo, monospace",
                    fontWeight: 600,
                  }}
                >
                  {formatClock(
                    gameState.time.hour,
                    gameState.time.minute
                  )}
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
              </tr>
            </thead>
            <tbody>
              {gameState.hunters.map((h) => (
                <tr key={h.id}>
                  <td style={tdStyle}>{h.name}</td>
                  <td style={tdStyle}>{h.skill}</td>
                  <td style={tdStyle}>
                    <span style={badgeStyle(h.status)}>
                      {h.status}
                    </span>
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
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 10,
                        }}
                      >
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
                        <span
                          style={{
                            fontVariantNumeric: "tabular-nums",
                            width: 44,
                          }}
                        >
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
                    <span style={badgeStyle(ms.status)}>
                      {ms.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ color: "#4b5563" }}>No missions yet.</p>
        )}
      </section>

      <footer
        style={{
          marginTop: "1rem",
          color: "#6b7280",
          fontSize: 12,
        }}
      >
      </footer>
    </div>
  )
}
