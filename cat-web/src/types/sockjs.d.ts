declare module 'sockjs-client' {
  export default class SockJS {
    constructor(url: string)
    close(): void
    send(message: string): void
    onopen: ((event: Event) => void) | null
    onmessage: ((event: MessageEvent) => void) | null
    onclose: ((event: CloseEvent) => void) | null
    onerror: ((event: Event) => void) | null
  }
}