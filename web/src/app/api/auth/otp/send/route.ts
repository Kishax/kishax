// OTP（ワンタイムパスワード）送信機能
// 現在は使用していないが、将来的な機能として保持
import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { sendOneTimePassword } from '@/lib/email'
import { generateOTP, storeOTP, hasActiveOTP, getOTPRemainingTime } from '@/lib/otp'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const sendOTPSchema = z.object({
  email: z.string().email('有効なメールアドレスを入力してください'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { email } = sendOTPSchema.parse(body)

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

    // Check if OTP is already active
    if (hasActiveOTP(email)) {
      const remainingTime = getOTPRemainingTime(email)
      const minutes = Math.floor(remainingTime / 60)
      const seconds = remainingTime % 60
      
      return NextResponse.json(
        { 
          error: `既にOTPコードが送信されています。${minutes}分${seconds}秒後に再度お試しください。` 
        },
        { status: 429 }
      )
    }

    // Generate and store OTP
    const otp = generateOTP()
    storeOTP(email, otp)

    // Send OTP email
    const emailSent = await sendOneTimePassword(email, otp)

    if (!emailSent) {
      return NextResponse.json(
        { error: 'メール送信に失敗しました。しばらく時間をおいて再度お試しください。' },
        { status: 500 }
      )
    }

    return NextResponse.json({
      message: 'OTPコードをメールに送信しました。メールをご確認ください。',
      expiresIn: 600 // 10 minutes in seconds
    })

  } catch (error) {
    console.error('Send OTP error:', error)

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