import { auth } from "@/lib/auth"
import Link from "next/link"

export default async function HomePage() {
  const session = await auth()

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <h1 className="text-3xl font-bold text-gray-900">KishaX</h1>
            <nav className="flex space-x-4">
              {session ? (
                <>
                  <span className="text-gray-700">Welcome, {session.user?.name}</span>
                  <Link href="/app/chart" className="text-blue-600 hover:text-blue-800">
                    Chart
                  </Link>
                  <Link href="/mc/auth" className="text-green-600 hover:text-green-800">
                    MC Auth
                  </Link>
                  <Link href="/api/auth/signout" className="text-red-600 hover:text-red-800">
                    Logout
                  </Link>
                </>
              ) : (
                <>
                  <Link href="/signin" className="text-blue-600 hover:text-blue-800">
                    Sign In
                  </Link>
                  <Link href="/signup" className="text-green-600 hover:text-green-800">
                    Sign Up
                  </Link>
                </>
              )}
            </nav>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="text-center">
            <h2 className="text-4xl font-extrabold text-gray-900 sm:text-5xl">
              Welcome to KishaX
            </h2>
            <p className="mt-4 text-xl text-gray-600">
              A modern web application with secure authentication
            </p>
            
            {!session && (
              <div className="mt-8 space-x-4">
                <Link
                  href="/signin"
                  className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
                >
                  Get Started
                </Link>
              </div>
            )}
            
            {session && (
              <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <Link
                  href="/app/chart"
                  className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-indigo-500 rounded-lg shadow hover:shadow-md"
                >
                  <div>
                    <span className="rounded-lg inline-flex p-3 bg-indigo-50 text-indigo-600 group-hover:bg-indigo-100">
                      📊
                    </span>
                  </div>
                  <div className="mt-8">
                    <h3 className="text-lg font-medium">
                      Chart Analytics
                    </h3>
                    <p className="mt-2 text-sm text-gray-500">
                      View statistics and analytics data
                    </p>
                  </div>
                </Link>

                <Link
                  href="/mc/auth"
                  className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-green-500 rounded-lg shadow hover:shadow-md"
                >
                  <div>
                    <span className="rounded-lg inline-flex p-3 bg-green-50 text-green-600 group-hover:bg-green-100">
                      🎮
                    </span>
                  </div>
                  <div className="mt-8">
                    <h3 className="text-lg font-medium">
                      Minecraft Auth
                    </h3>
                    <p className="mt-2 text-sm text-gray-500">
                      Authenticate with Minecraft server
                    </p>
                  </div>
                </Link>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
