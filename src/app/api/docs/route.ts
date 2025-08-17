import { NextResponse } from "next/server"
import { getOpenAPISpec } from "@/lib/openapi"

export async function GET() {
  try {
    const spec = getOpenAPISpec()
    return NextResponse.json(spec)
  } catch (error) {
    console.error("Error generating OpenAPI spec:", error)
    return NextResponse.json(
      { error: "Failed to generate API documentation" },
      { status: 500 }
    )
  }
}