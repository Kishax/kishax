import 'next-auth'
import { DefaultSession } from 'next-auth'

declare module 'next-auth' {
  interface User {
    needsUsername?: boolean
    username?: string | null
  }

  interface Session {
    user: {
      needsUsername?: boolean
    } & DefaultSession['user']
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    needsUsername?: boolean
  }
}
