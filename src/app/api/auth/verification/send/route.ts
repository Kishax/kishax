import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { sendVerificationEmail } from '@/lib/email'
import { PrismaClient } from '@prisma/client'
import crypto from 'crypto'

const prisma = new PrismaClient()

const sendVerificationSchema = z.object({
  email: z.string().email('有効なメールアドレスを入力してください'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { email } = sendVerificationSchema.parse(body)

    // Check if user exists
    const user = await prisma.user.findUnique({
      where: { email }
    })

    if (!user) {
      return NextResponse.json(
        { error: 'このメールアドレスは登録されていません。' },
        { status: 404 }
      )
    }

    // Check if already verified
    if (user.emailVerified) {
      return NextResponse.json(
        { error: 'このメールアドレスは既に認証済みです。' },
        { status: 400 }
      )
    }

    // Generate verification token
    const token = crypto.randomBytes(32).toString('hex')
    const expires = new Date(Date.now() + 24 * 60 * 60 * 1000) // 24 hours

    // Store or update verification token
    await prisma.verificationToken.upsert({
      where: {
        identifier_token: {
          identifier: email,
          token: token
        }
      },
      update: {
        token: token,
        expires: expires
      },
      create: {
        identifier: email,
        token: token,
        expires: expires
      }
    })

    // Create verification URL
    const baseUrl = process.env.NEXTAUTH_URL || 'http://localhost:3000'
    const verificationUrl = `${baseUrl}/auth/verify-email?token=${token}&email=${encodeURIComponent(email)}`

    // Send verification email
    const emailSent = await sendVerificationEmail(email, verificationUrl)

    if (!emailSent) {
      return NextResponse.json(
        { error: 'メール送信に失敗しました。しばらく時間をおいて再度お試しください。' },
        { status: 500 }
      )
    }

    return NextResponse.json({
      message: '認証リンクをメールに送信しました。メールをご確認ください。',
      expiresIn: 24 * 60 * 60 // 24 hours in seconds
    })

  } catch (error) {
    console.error('Send verification email error:', error)

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