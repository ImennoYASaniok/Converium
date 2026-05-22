import { defineStore } from 'pinia'

function decodeJwtPayload(token) {
  try {
    const parts = token.split('.')
    if (parts.length === 3) {
      const payloadPart = parts[1]
      if (!payloadPart) return null

      const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=')
      const json = atob(padded)
      return JSON.parse(json)
    }

    // Fallback for tokens with format: base64url("id:login:expiresAtMs").base64url(signature)
    if (parts.length === 2) {
      const payloadB64 = parts[0]
      if (!payloadB64) return null

      const base64 = payloadB64.replace(/-/g, '+').replace(/_/g, '/')
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=')
      const decoded = atob(padded)
      const [id] = decoded.split(':')
      const parsedId = Number(id)
      return Number.isFinite(parsedId) ? { sub: String(parsedId) } : null
    }

    return null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
    userId: (state) => {
      if (!state.accessToken) return null
      const payload = decodeJwtPayload(state.accessToken)
      const sub = payload?.sub
      const parsed = Number(sub)
      return Number.isFinite(parsed) ? parsed : null
    },
  },
  actions: {
    setToken(token) {
      this.accessToken = token
      if (token) {
        localStorage.setItem('accessToken', token)
      } else {
        localStorage.removeItem('accessToken')
      }
    },
    logout() {
      this.setToken('')
    },
  },
})
