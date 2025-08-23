'use client'

import { useEffect } from 'react'
import { useSession } from 'next-auth/react'
import { useRouter } from 'next/navigation'

export function OAuthRedirectHandler() {
  const { data: session, status } = useSession()
  const router = useRouter()

  useEffect(() => {
    if (status === 'loading') return

    if (status === 'authenticated' && session?.user?.email) {
      // Check if user needs to set username
      fetch('/api/auth/user-status')
        .then(res => res.json())
        .then(data => {
          if (!data.hasUsername) {
            router.push('/auth/setup-username')
          }
        })
        .catch(() => {
          // If API fails, continue normally
        })
    }
  }, [status, session, router])

  return null
}