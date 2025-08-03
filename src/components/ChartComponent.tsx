"use client"

import { useState, useEffect } from "react"
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js"
import { Line } from "react-chartjs-2"

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

interface CounterData {
  labels: string[]
  datasets: {
    label: string
    data: number[]
    borderColor: string
    backgroundColor: string
  }[]
}

export default function ChartComponent() {
  const [chartData, setChartData] = useState<CounterData | null>(null)
  const [timeRange, setTimeRange] = useState<"last7days" | "month" | "year">("last7days")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const response = await fetch(`/api/counter?type=${timeRange}`)
        if (!response.ok) {
          throw new Error("Failed to fetch data")
        }
        const result = await response.json()
        
        if (result.error) {
          setError(result.error)
          return
        }

        const data = result.data
        setChartData({
          labels: data.map((item: any) => item.date),
          datasets: [
            {
              label: "Counter",
              data: data.map((item: any) => item.count),
              borderColor: "rgb(75, 192, 192)",
              backgroundColor: "rgba(75, 192, 192, 0.2)",
            },
          ],
        })
      } catch (err) {
        setError("Failed to load chart data")
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [timeRange])

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: "top" as const,
      },
      title: {
        display: true,
        text: "Counter Statistics",
      },
    },
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-red-600 text-center p-4">
        Error: {error}
      </div>
    )
  }

  return (
    <div>
      <div className="mb-4">
        <label htmlFor="timeRange" className="block text-sm font-medium text-gray-700 mb-2">
          Time Range:
        </label>
        <select
          id="timeRange"
          value={timeRange}
          onChange={(e) => setTimeRange(e.target.value as "last7days" | "month" | "year")}
          className="block w-40 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        >
          <option value="last7days">Last 7 Days</option>
          <option value="month">This Month</option>
          <option value="year">This Year</option>
        </select>
      </div>
      
      {chartData && (
        <div className="h-96">
          <Line data={chartData} options={options} />
        </div>
      )}
    </div>
  )
}