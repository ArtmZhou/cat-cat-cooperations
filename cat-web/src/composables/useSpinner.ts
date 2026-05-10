import { ref, onUnmounted } from 'vue'

const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']

export function useSpinner() {
  const spinnerFrame = ref('⠋')
  let interval: ReturnType<typeof setInterval> | null = null

  function start() {
    if (interval) return
    let i = 0
    interval = setInterval(() => {
      spinnerFrame.value = spinnerFrames[i % spinnerFrames.length]
      i++
    }, 80)
  }

  function stop() {
    if (interval) { clearInterval(interval); interval = null }
  }

  onUnmounted(stop)
  return { spinnerFrame, startSpinner: start, stopSpinner: stop }
}
