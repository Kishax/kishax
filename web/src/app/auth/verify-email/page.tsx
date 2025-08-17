'use client'

import { useEffect, useState, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useSession } from 'next-auth/react'
import Link from 'next/link'

function VerifyEmailContent() {
  const [status, setStatus] = useState<'verifying' | 'success' | 'error'>('verifying')
  const [message, setMessage] = useState('')
  const [needsUsername, setNeedsUsername] = useState(false)
  const [userEmail, setUserEmail] = useState('')
  const searchParams = useSearchParams()
  const router = useRouter()
  const { data: session, status: sessionStatus } = useSession()

  useEffect(() => {
    // If user is already authenticated, redirect appropriately
    if (sessionStatus === 'authenticated') {
      if (session?.user?.name) {
        // User is fully set up, redirect to home
        router.push('/')
        return
      }
      // User needs username setup, continue with current flow but skip verification
    }

    const verifyEmail = async () => {
      const token = searchParams.get('token')
      const email = searchParams.get('email')

      if (!token || !email) {
        setStatus('error')
        setMessage('無効な認証リンクです。')
        return
      }

      try {
        const response = await fetch('/api/auth/verification/verify', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ email, token }),
        })

        const data = await response.json()

        if (response.ok) {
          setStatus('success')
          setMessage(data.message)
          setNeedsUsername(data.user?.needsUsername || false)
          setUserEmail(email)
          
          // Auto redirect after 3 seconds if username is not needed
          if (!data.user?.needsUsername) {
            setTimeout(() => {
              router.push('/signin?message=email_verified')
            }, 3000)
          }
        } else {
          setStatus('error')
          setMessage(data.error || 'メール認証に失敗しました。')
        }
      } catch {
        setStatus('error')
        setMessage('ネットワークエラーが発生しました。')
      }
    }

    if (sessionStatus !== 'loading') {
      verifyEmail()
    }
  }, [searchParams, router, session, sessionStatus])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          {status === 'verifying' && (
            <>
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                メールアドレスを認証中...
              </h2>
              <p className="text-gray-600">
                しばらくお待ちください
              </p>
            </>
          )}

          {status === 'success' && (
            <>
              <div className="w-12 h-12 mx-auto mb-4 bg-green-100 rounded-full flex items-center justify-center">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                認証完了！
              </h2>
              <p className="text-gray-600 mb-6">
                {message}
              </p>

              {needsUsername ? (
                <div className="space-y-4">
                  <p className="text-sm text-blue-600 bg-blue-50 p-3 rounded-md">
                    続いてユーザー名を設定してください
                  </p>
                  <Link
                    href={`/auth/setup-username?email=${encodeURIComponent(userEmail)}&token=${encodeURIComponent(searchParams.get('token') || '')}`}
                    className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                  >
                    ユーザー名を設定する
                  </Link>
                </div>
              ) : (
                <div className="space-y-4">
                  <p className="text-sm text-green-600">
                    3秒後に自動的にログインページに移動します...
                  </p>
                  <Link
                    href="/signin?message=email_verified"
                    className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                  >
                    今すぐログインする
                  </Link>
                </div>
              )}
            </>
          )}

          {status === 'error' && (
            <>
              <div className="w-12 h-12 mx-auto mb-4 bg-red-100 rounded-full flex items-center justify-center">
                <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                認証に失敗しました
              </h2>
              <p className="text-gray-600 mb-6">
                {message}
              </p>

              <div className="space-y-2">
                <Link
                  href="/signup"
                  className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                >
                  新しいアカウントを作成
                </Link>
                <Link
                  href="/signin"
                  className="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                >
                  ログインページに戻る
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">読み込み中...</div>}>
      <VerifyEmailContent />
    </Suspense>
  )
}