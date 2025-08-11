import { auth } from "@/lib/auth"
import { PrismaClient } from "@prisma/client"
import McAuthForm from "@/components/McAuthForm"
import { McAuthPageData } from "@/lib/schemas"
import jwt from "jsonwebtoken"
import Link from "next/link"

const prisma = new PrismaClient()

interface PageProps {
  searchParams: Promise<{ n?: string }>
}

export default async function McAuthPage({ searchParams }: PageProps) {
  const session = await auth()
  const resolvedSearchParams = await searchParams
  const memberId = resolvedSearchParams.n ? parseInt(resolvedSearchParams.n) : undefined

  // Initialize page data
  let pageData: McAuthPageData = {
    isAuth: !!session,
    username: session?.user?.username || "[ユーザーID未設定]",
    mcAuth: false,
    successMessage: undefined,
    errorMessage: undefined,
    infoMessage: undefined
  }

  // If not authenticated, show login required message
  if (!session) {
    pageData.infoMessage = ["WEB認証にはログインが必要です。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Get user from database
  const user = await prisma.user.findUnique({
    where: { id: session?.user?.id || '' }
  })

  if (!user) {
    pageData.errorMessage = ["ユーザー情報が見つかりません。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  pageData.username = user.username || "[ユーザーID未設定]"

  // If no member ID provided, show initial message
  if (!memberId) {
    pageData.infoMessage = ["サーバーに参加しよう！"]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Get Minecraft member data
  const mcuser = await prisma.member.findUnique({
    where: { id: memberId }
  })

  if (!mcuser) {
    pageData.errorMessage = ["不正なアクセスです。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Check if already confirmed
  if (mcuser.confirm) {
    pageData.infoMessage = ["認証済みユーザーです。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Check if OTP is set
  if (!mcuser.secret2) {
    pageData.errorMessage = [
      "ワンタイムパスワードが設定されていません。",
      "サーバーで/retryコマンドよりワンタイムパスワードを生成してください。",
      "生成後、ページのリロードが必要です。"
    ]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Check server status and online players
  const serverStatus = await prisma.status.findUnique({
    where: { name: "proxy" }
  })

  if (!serverStatus?.playerList) {
    pageData.errorMessage = ["プレイヤーがオンラインでないため、WEB認証ができません。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  const onlinePlayers = serverStatus.playerList.split(",")
  if (!onlinePlayers.includes(mcuser.name)) {
    pageData.errorMessage = ["プレイヤーがオンラインでないため、WEB認証ができません。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  // Generate JWT token for authentication
  try {
    const payload = {
      username: user.username || "[ユーザーID未設定]",
      mcid: mcuser.name,
      uuid: mcuser.uuid
    }
    
    const token = jwt.sign(payload, process.env.NEXTAUTH_SECRET!, { expiresIn: '1h' })

    pageData = {
      ...pageData,
      mcid: mcuser.name,
      uuid: mcuser.uuid,
      mcAuth: true,
      token,
      successMessage: ["プレイヤー情報が自動入力されました。"]
    }
  } catch {
    pageData.errorMessage = ["トークンの生成に失敗しました。"]
    return <McAuthPageComponent pageData={pageData} />
  }

  return <McAuthPageComponent pageData={pageData} />
}

function McAuthPageComponent({ pageData }: { pageData: McAuthPageData }) {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <h1 className="text-3xl font-bold text-gray-900">Minecraft Authentication</h1>
            <nav className="flex space-x-4">
              <Link href="/" className="text-blue-600 hover:text-blue-800">
                Home
              </Link>
            </nav>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="bg-white shadow rounded-lg p-6">
            <div className="mb-6">
              {pageData.isAuth ? (
                <p className="text-sm text-gray-600">
                  {pageData.username}さん、ようこそ
                </p>
              ) : (
                <div className="flex items-center space-x-2">
                  <p className="text-sm text-gray-600">
                    {pageData.username}さん、ようこそ &gt;&gt;
                  </p>
                  <Link href="/signup" className="text-blue-600 hover:text-blue-800 text-sm underline">
                    サインアップはこちら
                  </Link>
                </div>
              )}
            </div>

            {/* Messages */}
            {pageData.successMessage && (
              <div className="mb-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded">
                {pageData.successMessage.map((msg, index) => (
                  <p key={index}>{msg}</p>
                ))}
              </div>
            )}

            {pageData.errorMessage && (
              <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                {pageData.errorMessage.map((msg, index) => (
                  <p key={index}>{msg}</p>
                ))}
              </div>
            )}

            {pageData.infoMessage && (
              <div className="mb-4 p-4 bg-blue-100 border border-blue-400 text-blue-700 rounded">
                {pageData.infoMessage.map((msg, index) => (
                  <p key={index}>{msg}</p>
                ))}
              </div>
            )}

            <div className="mb-6">
              <p className="text-gray-700">
                サーバーに参加し、認証コードを入力してWEB認証を完了してください。
              </p>
            </div>

            <McAuthForm pageData={pageData} />
          </div>
        </div>
      </main>
    </div>
  )
}