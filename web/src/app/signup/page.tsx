"use client"

import { signIn } from "next-auth/react"
import { useState } from "react"
import Link from "next/link"

export default function SignUpPage() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: ""
  })
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const [step, setStep] = useState<'signup' | 'verify'>('signup')
  const [verificationMethod, setVerificationMethod] = useState<'otp' | 'link'>('otp')
  const [showMethodSelection, setShowMethodSelection] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError("")

    if (formData.password !== formData.confirmPassword) {
      setError("パスワードが一致しません")
      setLoading(false)
      return
    }

    try {
      const response = await fetch("/api/auth/signup", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password,
        }),
      })

      const data = await response.json()

      if (response.ok) {
        // 直接招待リンクを送信（デフォルト）
        await handleVerificationMethod('link')
      } else {
        setError(data.error || "サインアップ中にエラーが発生しました")
      }
    } catch {
      setError("サインアップ中にエラーが発生しました")
    } finally {
      setLoading(false)
    }
  }

  const handleVerificationMethod = async (method: 'otp' | 'link') => {
    setVerificationMethod(method)
    setLoading(true)
    setError("")

    try {
      // 現在は招待リンクのみ使用、OTPロジックは将来のために保持
      const endpoint = method === 'otp' 
        ? '/api/auth/otp/send'  // OTP機能（現在は使用しない）
        : '/api/auth/verification/send'  // 招待リンク（デフォルト）
      
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: formData.email }),
      })

      const data = await response.json()

      if (response.ok) {
        setShowMethodSelection(false)
        if (method === 'link') {
          setStep('verify')
        }
        /* OTP処理（現在は使用しない）
        if (method === 'otp') {
          // OTP認証画面への遷移処理
          setStep('verify-otp')
        }
        */
      } else {
        setError(data.error || '認証メール送信に失敗しました')
      }
    } catch {
      setError('認証メール送信に失敗しました')
    } finally {
      setLoading(false)
    }
  }

  const handleOAuthSignUp = (provider: string) => {
    signIn(provider, { callbackUrl: "/" })
  }

  /* 
  // メール認証方法選択画面（現在は使用しない - 招待リンクがデフォルト）
  if (step === 'verify' && showMethodSelection) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <div>
            <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
              メール認証方法を選択
            </h2>
            <p className="mt-2 text-center text-sm text-gray-600">
              {formData.email} に認証情報を送信します
            </p>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <button
              onClick={() => handleVerificationMethod('otp')}
              disabled={loading}
              className="w-full flex flex-col items-center justify-center px-4 py-6 border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              <div className="text-lg font-medium text-gray-900 mb-2">
                📱 ワンタイムパスワード（OTP）
              </div>
              <div className="text-sm text-gray-600 text-center">
                6桁の認証コードをメールで受け取り、入力して認証します
              </div>
            </button>

            <button
              onClick={() => handleVerificationMethod('link')}
              disabled={loading}
              className="w-full flex flex-col items-center justify-center px-4 py-6 border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              <div className="text-lg font-medium text-gray-900 mb-2">
                🔗 認証リンク
              </div>
              <div className="text-sm text-gray-600 text-center">
                メール内のリンクをクリックするだけで認証完了します
              </div>
            </button>
          </div>

          {loading && (
            <div className="text-center">
              <div className="text-sm text-gray-600">認証メール送信中...</div>
            </div>
          )}
        </div>
      </div>
    )
  }
  */

  if (step === 'verify' && verificationMethod === 'link') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <div>
            <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
              メールを確認してください
            </h2>
            <p className="mt-2 text-center text-sm text-gray-600">
              {formData.email} に認証リンクを送信しました
            </p>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
            <div className="text-sm text-blue-800">
              <p className="font-medium mb-2">📧 次の手順で認証を完了してください：</p>
              <ol className="list-decimal list-inside space-y-1">
                <li>メールボックスを確認してください</li>
                <li>「メールアドレス認証」というタイトルのメールを開きます</li>
                <li>メール内の認証ボタンをクリックしてください</li>
              </ol>
            </div>
          </div>

          <div className="text-center">
            <button
              onClick={() => {
                setStep('signup')
                setShowMethodSelection(false)
              }}
              className="text-indigo-600 hover:text-indigo-500"
            >
              戻る
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            アカウントを作成
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
          
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <input
                id="email"
                name="email"
                type="email"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="メールアドレス"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
            <div>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="パスワード"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>
            <div>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="パスワード確認"
                value={formData.confirmPassword}
                onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              {loading ? "アカウント作成中..." : "アカウント作成"}
            </button>
          </div>

          <>
            <div className="mt-6">
              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-gray-300" />
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="px-2 bg-gray-50 text-gray-500">または他のサービスで続ける</span>
                </div>
              </div>

              <div className="mt-6 grid grid-cols-3 gap-3">
                <button
                  onClick={() => handleOAuthSignUp("google")}
                  className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                >
                  Google
                </button>
                <button
                  onClick={() => handleOAuthSignUp("discord")}
                  className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                >
                  Discord
                </button>
                <button
                  onClick={() => handleOAuthSignUp("twitter")}
                  className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                >
                  Twitter
                </button>
              </div>
            </div>

            <div className="text-center">
              <Link href="/signin" className="text-indigo-600 hover:text-indigo-500">
                すでにアカウントをお持ちですか？ サインイン
              </Link>
            </div>
          </>
        </form>
      </div>
    </div>
  )
}