import { NextRequest, NextResponse } from 'next/server'
import { PrismaClient } from '@prisma/client'
import { verify } from 'jsonwebtoken'

const prisma = new PrismaClient()

export async function POST(request: NextRequest) {
  try {
    const { sessionToken } = await request.json()

    if (!sessionToken) {
      return NextResponse.json(
        { error: 'Session token is required' },
        { status: 400 }
      )
    }

    // Verify the session token
    let decodedToken
    try {
      decodedToken = verify(sessionToken, process.env.NEXTAUTH_SECRET!) as {
        id: string
        email: string
        username: string
        purpose: string
      }
    } catch {
      return NextResponse.json(
        { error: 'Invalid session token' },
        { status: 401 }
      )
    }

    // Check token purpose
    if (decodedToken.purpose !== 'auto-login') {
      return NextResponse.json(
        { error: 'Invalid token purpose' },
        { status: 401 }
      )
    }

    // Get user from database to ensure they still exist and have username
    const user = await prisma.user.findUnique({
      where: { id: decodedToken.id },
      select: {
        id: true,
        email: true,
        username: true,
        name: true,
        emailVerified: true,
        image: true,
      },
    })

    if (!user || !user.username) {
      return NextResponse.json(
        { error: 'User not found or username not set' },
        { status: 404 }
      )
    }

    return NextResponse.json({
      success: true,
      user: {
        id: user.id,
        email: user.email,
        name: user.name || user.username,
        username: user.username,
        image: user.image,
        emailVerified: !!user.emailVerified,
      },
    })
  } catch (error) {
    console.error('Auto-login error:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}