import { useEffect, useState } from "react"

function App() {
  const [status, setStatus] = useState("loading")

  useEffect(() => {
    fetch("/api/health")
      .then(res => res.json())
      .then(data => setStatus(data.status))
      .catch(() => setStatus("error"))
  }, [])

  return (
    <>
      <h1>Hunters Registry</h1>
      <p>Backend status: {status}</p>
    </>
  )
}

export default App
