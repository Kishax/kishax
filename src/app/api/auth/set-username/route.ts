import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const setUsernameSchema = z.object({
  email: z.string().email('有効なメールアドレスを入力してください'),
  username: z.string().min(3, 'ユーザー名は3文字以上で入力してください').max(50, 'ユーザー名は50文字以内で入力してください'),
})

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { email, username } = setUsernameSchema.parse(body)

    // Find user by email
    const user = await prisma.user.findUnique({
      where: { email }
    })

    if (!user) {
      return NextResponse.json(
        { error: 'ユーザーが見つかりません。' },
        { status: 404 }
      )
    }

    // For OAuth users, ensure email is verified (update if needed)
    if (!user.emailVerified) {
      // Check if user has OAuth accounts (meaning they came through OAuth)
      const accounts = await prisma.account.findMany({
        where: { userId: user.id }
      })
      
      const hasOAuthAccount = accounts.some(account => 
        account.provider !== 'credentials'
      )
      
      if (hasOAuthAccount) {
        // OAuth user should have verified email automatically
        await prisma.user.update({
          where: { id: user.id },
          data: { emailVerified: new Date() }
        })
      } else {
        // Credentials user without verified email
        return NextResponse.json(
          { error: 'メールアドレスが認証されていません。' },
          { status: 400 }
        )
      }
    }

    // Check if username is already set
    if (user.username) {
      return NextResponse.json(
        { error: 'ユーザー名は既に設定されています。' },
        { status: 400 }
      )
    }

    // Username validation
    const usernameRegex = /^[a-zA-Z0-9_-]+$/
    if (!usernameRegex.test(username)) {
      return NextResponse.json(
        { error: 'ユーザー名には英数字、アンダースコア(_)、ハイフン(-)のみ使用できます。' },
        { status: 400 }
      )
    }

    // Reserved usernames check
    const reservedUsernames = [
      'admin', 'root', 'user', 'test', 'api', 'www', 'mail', 'ftp',
      'blog', 'dev', 'stage', 'support', 'help', 'info', 'contact',
      'about', 'terms', 'privacy', 'security', 'login', 'signup',
      'signin', 'signout', 'register', 'auth', 'oauth', 'dashboard',
      'profile', 'settings', 'account', 'system', 'service', 'server',
      'client', 'guest', 'anonymous', 'kishax', 'minecraft', 'mc'
    ]

    if (reservedUsernames.includes(username.toLowerCase())) {
      return NextResponse.json(
        { error: 'このユーザー名は予約されているため使用できません。' },
        { status: 400 }
      )
    }

    // Check if username already exists
    const existingUser = await prisma.user.findUnique({
      where: { username: username }
    })

    if (existingUser) {
      return NextResponse.json(
        { error: 'このユーザー名は既に使用されています。' },
        { status: 400 }
      )
    }

    // Update user with username
    const updatedUser = await prisma.user.update({
      where: { email },
      data: { username: username }
    })

    return NextResponse.json({
      message: 'ユーザー名が設定されました。',
      user: {
        id: updatedUser.id,
        name: updatedUser.name,
        username: updatedUser.username,
        email: updatedUser.email,
        emailVerified: !!updatedUser.emailVerified
      }
    })

  } catch (error) {
    console.error('Set username error:', error)

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