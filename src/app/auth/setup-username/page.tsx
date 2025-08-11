'use client'

import { useState, useEffect, useCallback, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { signIn, useSession } from 'next-auth/react'

const usernameSchema = z.object({
  username: z.string()
    .min(3, 'ユーザー名は3文字以上で入力してください')
    .max(50, 'ユーザー名は50文字以内で入力してください')
    .regex(/^[a-zA-Z0-9_-]+$/, 'ユーザー名には英数字、アンダースコア(_)、ハイフン(-)のみ使用できます')
})

type UsernameFormData = z.infer<typeof usernameSchema>

interface UsernameCheckResult {
  available: boolean
  message: string
}

function SetupUsernameContent() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [checkingUsername, setCheckingUsername] = useState(false)
  const [usernameCheck, setUsernameCheck] = useState<UsernameCheckResult | null>(null)
  const [email, setEmail] = useState('')
  
  const searchParams = useSearchParams()
  const router = useRouter()
  const { data: session, status } = useSession()

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<UsernameFormData>({
    resolver: zodResolver(usernameSchema),
    mode: 'onChange',
  })

  const watchedUsername = watch('username')

  useEffect(() => {
    // Skip if still loading
    if (status === 'loading') {
      return
    }

    // If user is not authenticated, redirect to signin after a delay
    if (status === 'unauthenticated') {
      const timer = setTimeout(() => {
        router.push('/signin')
      }, 2000) // Wait 2 seconds for session to load
      
      return () => clearTimeout(timer)
    }

    // If authenticated user already has username, redirect to home
    if (status === 'authenticated' && session?.user?.email) {
      // Check if user already has username set by fetching user data
      fetch('/api/auth/user-status')
        .then(res => res.json())
        .then(data => {
          if (data.hasUsername) {
            router.push('/')
          }
        })
        .catch(() => {
          // If API fails, continue with setup flow
        })
    }

    const emailParam = searchParams.get('email')
    if (emailParam) {
      setEmail(emailParam)
    } else if (status === 'authenticated') {
      if (session?.user?.email) {
        setEmail(session.user.email)
      } else {
        // Authenticated but no email in session, and no email in param
        router.push('/signup')
        return
      }
    }
  }, [searchParams, router, session, status])

  // Debounced username availability check
  const checkUsernameAvailability = useCallback(async (username: string) => {
    if (!username || username.length < 3) {
      setUsernameCheck(null)
      return
    }

    setCheckingUsername(true)
    
    try {
      const response = await fetch('/api/auth/check-username', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username }),
      })

      const data = await response.json()
      setUsernameCheck(data)
    } catch {
      setUsernameCheck({
        available: false,
        message: 'ユーザー名の確認中にエラーが発生しました'
      })
    } finally {
      setCheckingUsername(false)
    }
  }, [])

  // Debounce username checking
  useEffect(() => {
    const timer = setTimeout(() => {
      if (watchedUsername && !errors.username) {
        checkUsernameAvailability(watchedUsername)
      } else {
        setUsernameCheck(null)
      }
    }, 500) // 500ms debounce

    return () => clearTimeout(timer)
  }, [watchedUsername, errors.username, checkUsernameAvailability])

  const onSubmit = async (data: UsernameFormData) => {
    if (!usernameCheck?.available) {
      setError('利用可能なユーザー名を入力してください')
      return
    }

    setLoading(true)
    setError('')

    try {
      const response = await fetch('/api/auth/set-username', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          username: data.username,
        }),
      })

      const result = await response.json()

      if (response.ok) {
        // Auto-login after username setup
        try {
          const signInResult = await signIn("credentials", {
            username: email, // Use email for login
            password: "auto-login-after-setup", // Special flag for auto-login
            redirect: false,
          })

          if (signInResult?.error) {
            // If auto-login fails, redirect to signin with success message
            router.push('/signin?message=username_set_login_required')
          } else {
            // Auto-login success, redirect to home
            router.push('/?message=account_complete')
          }
        } catch {
          // Fallback: redirect to signin
          router.push('/signin?message=username_set_login_required')
        }
      } else {
        setError(result.error || 'ユーザー名の設定に失敗しました')
      }
    } catch {
      setError('ネットワークエラーが発生しました')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            ユーザー名を設定
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            アカウント作成の最後の手順です
          </p>
          {email && (
            <p className="mt-1 text-center text-xs text-blue-600">
              {email}
            </p>
          )}
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
              ユーザー名
            </label>
            <div className="relative">
              <input
                {...register('username')}
                type="text"
                className={`appearance-none relative block w-full px-3 py-2 border ${
                  errors.username || (usernameCheck && !usernameCheck.available)
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-200'
                    : usernameCheck?.available
                    ? 'border-green-500 focus:border-green-500 focus:ring-green-200'
                    : 'border-gray-300 focus:border-indigo-500 focus:ring-indigo-200'
                } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-2 focus:z-10 sm:text-sm`}
                placeholder="例: john_doe123"
              />
              
              {checkingUsername && (
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-indigo-600"></div>
                </div>
              )}
              
              {!checkingUsername && usernameCheck?.available && (
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <svg className="h-5 w-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                  </svg>
                </div>
              )}
              
              {!checkingUsername && usernameCheck && !usernameCheck.available && (
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <svg className="h-5 w-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </div>
              )}
            </div>
            
            {errors.username && (
              <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>
            )}
            
            {!errors.username && usernameCheck && (
              <p className={`mt-1 text-sm ${
                usernameCheck.available ? 'text-green-600' : 'text-red-600'
              }`}>
                {usernameCheck.message}
              </p>
            )}
            
            {!errors.username && !usernameCheck && watchedUsername && watchedUsername.length >= 3 && checkingUsername && (
              <p className="mt-1 text-sm text-gray-500">ユーザー名を確認中...</p>
            )}
            
            <div className="mt-2 text-xs text-gray-500">
              <p>ユーザー名の規則:</p>
              <ul className="list-disc list-inside mt-1">
                <li>3文字以上50文字以内</li>
                <li>英数字、アンダースコア(_)、ハイフン(-)のみ使用可能</li>
                <li>他のユーザーと重複しないもの</li>
              </ul>
            </div>
            
            <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-md">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">
                    重要なお知らせ
                  </h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    <p>このユーザー名は外部に公開されます。プロフィールページのURLやコメント投稿時などに表示されるため、適切な名前を設定してください。</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading || checkingUsername || !usernameCheck?.available}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '設定中...' : 'ユーザー名を設定'}
            </button>
          </div>

          <div className="text-center">
            <button
              type="button"
              onClick={() => router.push('/signup')}
              className="text-indigo-600 hover:text-indigo-500 text-sm"
            >
              最初から始める
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function SetupUsernamePage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">読み込み中...</div>}>
      <SetupUsernameContent />
    </Suspense>
  )
}