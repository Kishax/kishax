import { NextRequest, NextResponse } from "next/server"
import { ZodSchema, ZodError } from "zod"
import { auth } from "@/lib/auth"

// Validation middleware
export function validateRequest<T>(schema: ZodSchema<T>) {
  return async (req: NextRequest): Promise<{ data: T; error: null } | { data: null; error: NextResponse }> => {
    try {
      let requestData: unknown

      // Handle different request methods
      if (req.method === "GET") {
        const url = new URL(req.url)
        const params = Object.fromEntries(url.searchParams.entries())
        requestData = params
      } else {
        requestData = await req.json()
      }

      const validatedData = schema.parse(requestData)
      return { data: validatedData, error: null }
    } catch (error) {
      if (error instanceof ZodError) {
        return {
          data: null,
          error: NextResponse.json(
            {
              error: "Validation failed",
              message: error.errors.map(e => `${e.path.join('.')}: ${e.message}`).join(', '),
              statusCode: 400
            },
            { status: 400 }
          )
        }
      }

      return {
        data: null,
        error: NextResponse.json(
          {
            error: "Invalid request",
            message: "Failed to parse request data",
            statusCode: 400
          },
          { status: 400 }
        )
      }
    }
  }
}

// Authentication middleware
export async function requireAuth(): Promise<{ session: any; error: null } | { session: null; error: NextResponse }> {
  try {
    const session = await auth()
    
    if (!session?.user) {
      return {
        session: null,
        error: NextResponse.json(
          {
            error: "Unauthorized",
            message: "Authentication required",
            statusCode: 401
          },
          { status: 401 }
        )
      }
    }

    return { session, error: null }
  } catch (error) {
    return {
      session: null,
      error: NextResponse.json(
        {
          error: "Authentication failed",
          message: "Failed to verify authentication",
          statusCode: 401
        },
        { status: 401 }
      )
    }
  }
}

// Error response helper
export function createErrorResponse(error: string, message?: string, statusCode: number = 500): NextResponse {
  return NextResponse.json(
    {
      error,
      message: message || "An error occurred",
      statusCode
    },
    { status: statusCode }
  )
}

// Success response helper
export function createSuccessResponse(data: unknown, message?: string, statusCode: number = 200): NextResponse {
  return NextResponse.json(
    {
      ...(message && { message }),
      ...(data && { data })
    },
    { status: statusCode }
  )
}