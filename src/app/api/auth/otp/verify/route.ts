import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { verifyOTP } from '@/lib/otp'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const verifyOTPSchema = z.object({
  email: z.string().email('有効なメールアドレスを入力してください'),
  otp: z.string().min(6, 'OTPコードは6桁で入力してください').max(6, 'OTPコードは6桁で入力してください'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { email, otp } = verifyOTPSchema.parse(body)

    // Verify OTP
    const otpResult = verifyOTP(email, otp)

    if (!otpResult.success) {
      return NextResponse.json(
        { error: otpResult.message },
        { status: 400 }
      )
    }

    // Get user information
    const user = await prisma.user.findUnique({
      where: { email }
    })

    if (!user) {
      return NextResponse.json(
        { error: 'ユーザーが見つかりません。' },
        { status: 404 }
      )
    }

    // Update user's email verification status if not verified
    if (!user.emailVerified) {
      await prisma.user.update({
        where: { email },
        data: { emailVerified: new Date() }
      })
    }

    // Log the successful OTP verification
    console.log(`OTP verification successful for user: ${email}`)

    return NextResponse.json({
      message: 'OTP認証に成功しました。',
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        emailVerified: true
      }
    })

  } catch (error) {
    console.error('Verify OTP error:', error)

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