import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const checkUsernameSchema = z.object({
  username: z.string().min(1, 'ユーザー名は必須です').max(50, 'ユーザー名は50文字以内で入力してください'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { username } = checkUsernameSchema.parse(body)

    // Username validation rules
    const usernameRegex = /^[a-zA-Z0-9_-]+$/
    if (!usernameRegex.test(username)) {
      return NextResponse.json({
        available: false,
        message: 'ユーザー名には英数字、アンダースコア(_)、ハイフン(-)のみ使用できます。'
      })
    }

    if (username.length < 3) {
      return NextResponse.json({
        available: false,
        message: 'ユーザー名は3文字以上で入力してください。'
      })
    }

    // Reserved usernames
    const reservedUsernames = [
      'admin', 'root', 'user', 'test', 'api', 'www', 'mail', 'ftp',
      'blog', 'dev', 'stage', 'support', 'help', 'info', 'contact',
      'about', 'terms', 'privacy', 'security', 'login', 'signup',
      'signin', 'signout', 'register', 'auth', 'oauth', 'dashboard',
      'profile', 'settings', 'account', 'system', 'service', 'server',
      'client', 'guest', 'anonymous', 'kishax', 'minecraft', 'mc'
    ]

    if (reservedUsernames.includes(username.toLowerCase())) {
      return NextResponse.json({
        available: false,
        message: 'このユーザー名は予約されているため使用できません。'
      })
    }

    // Check if username already exists
    const existingUser = await prisma.user.findUnique({
      where: { username: username }
    })

    if (existingUser) {
      return NextResponse.json({
        available: false,
        message: 'このユーザー名は既に使用されています。'
      })
    }

    return NextResponse.json({
      available: true,
      message: 'このユーザー名は利用可能です。'
    })

  } catch (error) {
    console.error('Check username error:', error)

    if (error instanceof z.ZodError) {
      return NextResponse.json({
        available: false,
        message: error.issues[0].message
      })
    }

    return NextResponse.json({
      available: false,
      message: '内部サーバーエラーが発生しました。'
    }, { status: 500 })
  }
}