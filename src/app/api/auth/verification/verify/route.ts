import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const verifyEmailSchema = z.object({
  email: z.string().email('有効なメールアドレスを入力してください'),
  token: z.string().min(1, 'トークンは必須です'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { email, token } = verifyEmailSchema.parse(body)

    // Find and validate verification token
    const verificationToken = await prisma.verificationToken.findUnique({
      where: {
        identifier_token: {
          identifier: email,
          token: token
        }
      }
    })

    if (!verificationToken) {
      return NextResponse.json(
        { error: '無効な認証トークンです。' },
        { status: 400 }
      )
    }

    // Check if token is expired
    if (new Date() > verificationToken.expires) {
      // Clean up expired token
      await prisma.verificationToken.delete({
        where: {
          identifier_token: {
            identifier: email,
            token: token
          }
        }
      })

      return NextResponse.json(
        { error: '認証トークンの有効期限が切れています。新しい認証メールを要求してください。' },
        { status: 400 }
      )
    }

    // Find user and verify email
    const user = await prisma.user.findUnique({
      where: { email }
    })

    if (!user) {
      return NextResponse.json(
        { error: 'ユーザーが見つかりません。' },
        { status: 404 }
      )
    }

    // Update user's email verification status
    await prisma.user.update({
      where: { email },
      data: { emailVerified: new Date() }
    })

    // Clean up verification token
    await prisma.verificationToken.delete({
      where: {
        identifier_token: {
          identifier: email,
          token: token
        }
      }
    })

    return NextResponse.json({
      message: 'メールアドレスの認証が完了しました。',
      user: {
        id: user.id,
        email: user.email,
        emailVerified: true,
        needsUsername: !user.name // Indicate if username setup is needed
      }
    })

  } catch (error) {
    console.error('Verify email error:', error)

    if (error instanceof z.ZodError) {
      return NextResponse.json(
        { error: error.issues[0].message },
        { status: 400 }
      )
    }

    return NextResponse.json(
      { error: '内部サーバーエラーが発生しました。' },
      { status: 500 }
    )
  }
}

// Handle GET request for email verification link clicks
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const email = searchParams.get('email')
    const token = searchParams.get('token')

    if (!email || !token) {
      return NextResponse.redirect(new URL('/signup?error=invalid_token', request.url))
    }

    // Verify the token using the same logic as POST
    const verificationToken = await prisma.verificationToken.findUnique({
      where: {
        identifier_token: {
          identifier: email,
          token: token
        }
      }
    })

    if (!verificationToken) {
      return NextResponse.redirect(new URL('/signup?error=invalid_token', request.url))
    }

    if (new Date() > verificationToken.expires) {
      await prisma.verificationToken.delete({
        where: {
          identifier_token: {
            identifier: email,
            token: token
          }
        }
      })
      return NextResponse.redirect(new URL('/signup?error=expired_token', request.url))
    }

    // Find user
    const user = await prisma.user.findUnique({
      where: { email }
    })

    if (!user) {
      return NextResponse.redirect(new URL('/signup?error=user_not_found', request.url))
    }

    // Update user's email verification status
    await prisma.user.update({
      where: { email },
      data: { emailVerified: new Date() }
    })

    // Clean up verification token
    await prisma.verificationToken.delete({
      where: {
        identifier_token: {
          identifier: email,
          token: token
        }
      }
    })

    // Redirect to username setup if needed, otherwise to signin
    const redirectUrl = user.name 
      ? `/signin?message=email_verified`
      : `/auth/setup-username?email=${encodeURIComponent(email)}`
    
    return NextResponse.redirect(new URL(redirectUrl, request.url))

  } catch (error) {
    console.error('GET verify email error:', error)
    return NextResponse.redirect(new URL('/signup?error=server_error', request.url))
  }
}