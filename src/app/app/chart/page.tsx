import { auth } from "@/lib/auth"
import { redirect } from "next/navigation"
import ChartComponent from "@/components/ChartComponent"

export default async function ChartPage() {
  const session = await auth()

  if (!session) {
    redirect("/signin")
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <h1 className="text-3xl font-bold text-gray-900">Chart Analytics</h1>
            <nav className="flex space-x-4">
              <a href="/" className="text-blue-600 hover:text-blue-800">
                Home
              </a>
            </nav>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="bg-white shadow rounded-lg p-6">
            <h2 className="text-xl font-semibold mb-4">Counter Statistics</h2>
            <ChartComponent />
          </div>
        </div>
      </main>
    </div>
  )
}