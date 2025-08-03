"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { McAuthPageData } from "@/lib/schemas"

interface McAuthFormProps {
  pageData: McAuthPageData
}

export default function McAuthForm({ pageData }: McAuthFormProps) {
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const router = useRouter()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!pageData.isAuth || !pageData.mcAuth) {
      setError("認証が必要です。")
      return
    }

    if (!password || !/^\d{6}$/.test(password)) {
      setError("6桁の数字を入力してください。")
      return
    }

    setLoading(true)
    setError("")

    try {
      const response = await fetch("/api/mc/auth", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token: pageData.token,
          mcid: pageData.mcid,
          uuid: pageData.uuid,
          pass: password,
        }),
      })

      const result = await response.json()

      if (response.ok && result.success) {
        // Redirect to success page or refresh
        router.push("/mc/auth?success=true")
      } else {
        setError(result.message || "認証に失敗しました。")
      }
    } catch (error) {
      setError("認証中にエラーが発生しました。")
    } finally {
      setLoading(false)
    }
  }

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 6)
    setPassword(value)
  }

  const isReadOnly = !pageData.isAuth || !pageData.mcAuth

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}

      <div>
        <label htmlFor="mcid" className="block text-sm font-medium text-gray-700">
          MCID
        </label>
        <input
          type="text"
          id="mcid"
          name="mcid"
          value={pageData.mcid || "自動入力"}
          readOnly
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm bg-gray-100 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      <div>
        <label htmlFor="uuid" className="block text-sm font-medium text-gray-700">
          UUID
        </label>
        <input
          type="text"
          id="uuid"
          name="uuid"
          value={pageData.uuid || "自動入力"}
          readOnly
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm bg-gray-100 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        />
      </div>

      <div>
        <label htmlFor="password" className="block text-sm font-medium text-gray-700">
          ワンタイムパスワード
        </label>
        <input
          type="text"
          id="password"
          name="password"
          value={password}
          onChange={handlePasswordChange}
          placeholder="6桁の半角数字を入力"
          maxLength={6}
          readOnly={isReadOnly}
          className={`mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm ${
            isReadOnly ? 'bg-gray-100' : 'bg-white'
          }`}
        />
      </div>

      <div>
        {pageData.isAuth && pageData.mcAuth ? (
          <button
            type="submit"
            disabled={loading || !password || !/^\d{6}$/.test(password)}
            className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "認証中..." : "認証"}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => alert("ログインまたはサーバーへの参加が必要です。")}
            className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-gray-400 cursor-not-allowed"
          >
            🔒 認証
          </button>
        )}
      </div>
    </form>
  )
}