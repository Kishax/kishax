import { NextRequest, NextResponse } from "next/server"
import { PrismaClient } from "@prisma/client"
import { CounterQuerySchema, CounterResponseSchema } from "@/lib/schemas"
import { validateRequest, createErrorResponse } from "@/lib/api-middleware"
import { format, subDays, startOfMonth, startOfYear, endOfDay } from "date-fns"

const prisma = new PrismaClient()

/**
 * @swagger
 * /api/counter:
 *   get:
 *     summary: Get counter statistics
 *     description: Retrieve counter data for specified time period
 *     parameters:
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [year, month, last7days]
 *           default: last7days
 *         description: Time period for statistics
 *     responses:
 *       200:
 *         description: Counter data retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 data:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       date:
 *                         type: string
 *                         format: date
 *                       count:
 *                         type: number
 *                 error:
 *                   type: string
 *                   nullable: true
 *       400:
 *         description: Invalid request parameters
 *       500:
 *         description: Internal server error
 *     tags:
 *       - Statistics
 */
export async function GET(req: NextRequest) {
  try {
    // Validate request
    const validation = await validateRequest(CounterQuerySchema)(req)
    if (validation.error) {
      return validation.error
    }
    
    const { type } = validation.data

    // Calculate date range based on type
    let startDate: Date
    const endDate = new Date()

    switch (type) {
      case "last7days":
        startDate = subDays(endDate, 6) // Last 7 days including today
        break
      case "month":
        startDate = startOfMonth(endDate)
        break
      case "year":
        startDate = startOfYear(endDate)
        break
      default:
        startDate = subDays(endDate, 6)
    }

    // Query counter data
    const counters = await prisma.counter.findMany({
      where: {
        date: {
          gte: startDate,
          lte: endOfDay(endDate)
        }
      },
      orderBy: {
        date: 'asc'
      }
    })

    // Group by date and aggregate counts
    const aggregatedData = counters.reduce((acc, counter) => {
      const dateKey = format(counter.date, 'yyyy-MM-dd')
      
      if (!acc[dateKey]) {
        acc[dateKey] = {
          date: dateKey,
          count: 0
        }
      }
      
      acc[dateKey].count += counter.count
      return acc
    }, {} as Record<string, { date: string; count: number }>)

    // Convert to array and sort
    const data = Object.values(aggregatedData).sort((a, b) => a.date.localeCompare(b.date))

    // Validate response
    const response = CounterResponseSchema.parse({
      data,
      error: null
    })

    return NextResponse.json(response)
  } catch (error) {
    console.error("Counter API error:", error)
    return createErrorResponse(
      "Internal Server Error",
      "Failed to retrieve counter data",
      500
    )
  }
}