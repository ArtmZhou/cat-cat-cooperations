import { ref, onUnmounted } from 'vue'
import { getAgentStatus, getAgentTokenStats } from '@/api/cliAgent'

export function useAgentPolling(agentId: string, intervalMs: number = 5000) {
  const processStatus = ref<any>(null)
  const tokenStats = ref<any>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  async function loadProcessStatus() {
    try { processStatus.value = await getAgentStatus(agentId) } catch (e) { console.error(e) }
  }

  async function loadTokenStats() {
    try { tokenStats.value = await getAgentTokenStats(agentId) } catch (e) { console.error(e) }
  }

  function start() {
    loadProcessStatus()
    loadTokenStats()
    timer = setInterval(() => { loadProcessStatus(); loadTokenStats() }, intervalMs)
  }

  function stop() {
    if (timer) { clearInterval(timer); timer = null }
  }

  onUnmounted(stop)
  return { processStatus, tokenStats, startPolling: start, stopPolling: stop }
}
