import NextAuth from "next-auth"
import Google from "next-auth/providers/google"
import Discord from "next-auth/providers/discord"
import Twitter from "next-auth/providers/twitter"
import Credentials from "next-auth/providers/credentials"
import { PrismaAdapter } from "@auth/prisma-adapter"
import { PrismaClient } from "@prisma/client"
import bcrypt from "bcrypt"

const prisma = new PrismaClient()

export const { handlers, auth, signIn, signOut } = NextAuth({
  adapter: PrismaAdapter(prisma),
  // 同じメールアドレスのアカウント自動リンクを許可
  allowDangerousEmailAccountLinking: true,
  providers: [
    Google({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    }),
    Discord({
      clientId: process.env.DISCORD_CLIENT_ID!,
      clientSecret: process.env.DISCORD_CLIENT_SECRET!,
    }),
    Twitter({
      clientId: process.env.TWITTER_CLIENT_ID!,
      clientSecret: process.env.TWITTER_CLIENT_SECRET!,
      version: "2.0"
    }),
    Credentials({
      name: "credentials",
      credentials: {
        username: { label: "Username", type: "text" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        if (!credentials?.username || !credentials?.password) {
          return null
        }

        // Special case for OTP login
        if (credentials.password === "otp-verified") {
          const user = await prisma.user.findUnique({
            where: {
              email: credentials.username as string
            }
          })

          if (!user || !user.emailVerified) {
            return null
          }

          return {
            id: user.id.toString(),
            name: user.name,
            email: user.email,
          }
        }

        // Special case for auto-login after username setup
        if (credentials.password === "auto-login-after-setup") {
          const user = await prisma.user.findUnique({
            where: {
              email: credentials.username as string
            }
          })

          if (!user || !user.emailVerified || !user.name) {
            return null
          }

          return {
            id: user.id.toString(),
            name: user.name,
            email: user.email,
          }
        }

        // Try email login first (new system)
        let user = await prisma.user.findUnique({
          where: {
            email: credentials.username as string
          }
        })

        // If no user found by email, try username (old system compatibility)
        if (!user) {
          user = await prisma.user.findUnique({
            where: {
              name: credentials.username as string
            }
          })
        }

        if (!user) {
          return null
        }

        // Check if email is verified for new users
        if (user.email && !user.emailVerified) {
          return null
        }

        const isPasswordValid = await bcrypt.compare(
          credentials.password as string,
          user.password || ""
        )

        if (!isPasswordValid) {
          return null
        }

        return {
          id: user.id.toString(),
          name: user.name,
          email: user.email,
        }
      }
    })
  ],
  session: {
    strategy: "jwt",
  },
  callbacks: {
    async signIn({ user, account, profile }) {
      if (account?.provider === "google" || account?.provider === "discord" || account?.provider === "twitter") {
        // OAuth認証の場合の処理
        let existingUser = null
        
        // メールアドレスがある場合はメールで検索
        if (user.email) {
          existingUser = await prisma.user.findUnique({
            where: { email: user.email },
            include: { accounts: true }
          })
        }
        
        if (existingUser) {
          // 既存ユーザーの場合、アカウントをリンク
          const existingAccount = existingUser.accounts.find(
            acc => acc.provider === account.provider
          )
          
          if (!existingAccount) {
            await prisma.account.create({
              data: {
                userId: existingUser.id,
                type: account.type,
                provider: account.provider,
                providerAccountId: account.providerAccountId,
                refresh_token: account.refresh_token,
                access_token: account.access_token,
                expires_at: account.expires_at,
                token_type: account.token_type,
                scope: account.scope,
                id_token: account.id_token,
                session_state: account.session_state,
              }
            })
          }
          
          // usernameが設定されていない場合はsetup-usernameページへ
          if (!existingUser.username) {
            // redirect用のフラグを設定（このフラグはjwtコールバックで使用）
            user.needsUsername = true
          }
          
          return true
        } else {
          // 新規ユーザーの場合もsetup-usernameページへ
          user.needsUsername = true
          return true
        }
      }
      return true
    },
    async jwt({ token, user }) {
      if (user) {
        token.sub = user.id
        if (user.needsUsername) {
          token.needsUsername = true
        }
      }
      return token
    },
    async session({ session, token }) {
      if (token) {
        session.user.id = token.sub!
        if (token.needsUsername) {
          session.user.needsUsername = true
        }
      }
      return session
    },
  },
  pages: {
    signIn: '/signin',
  },
})