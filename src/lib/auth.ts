import NextAuth from "next-auth"
import { authConfig } from "@/auth.config"
import Google from "next-auth/providers/google"
import Discord from "next-auth/providers/discord"
import Twitter from "next-auth/providers/twitter"
import Credentials from "next-auth/providers/credentials"
import { PrismaAdapter } from "@auth/prisma-adapter"
import { PrismaClient } from "@prisma/client"
import bcrypt from "bcrypt"

const prisma = new PrismaClient()

export const { handlers, auth, signIn, signOut } = NextAuth({
  ...authConfig,
  adapter: PrismaAdapter(prisma),
  
  providers: [
    Google({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
      allowDangerousEmailAccountLinking: true,
    }),
    Discord({
      clientId: process.env.DISCORD_CLIENT_ID!,
      clientSecret: process.env.DISCORD_CLIENT_SECRET!,
      allowDangerousEmailAccountLinking: true,
    }),
    Twitter({
      clientId: process.env.TWITTER_CLIENT_ID!,
      clientSecret: process.env.TWITTER_CLIENT_SECRET!,
      
      allowDangerousEmailAccountLinking: true,
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
          user = await prisma.user.findFirst({
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
      // For OAuth providers, allow sign in but handle username check in JWT callback
      if (account?.provider !== "credentials") {
        if (!user?.email) return false;

        // Find or create the user
        let dbUser = await prisma.user.findUnique({
          where: { email: user.email },
        });

        // If user doesn't exist, create them
        if (!dbUser) {
          dbUser = await prisma.user.create({
            data: {
              email: user.email,
              name: user.name || profile?.name || '',
              image: user.image,
              emailVerified: new Date(), // OAuth providers are pre-verified
            }
          });
        } else {
          // If user exists but emailVerified is null, update it for OAuth
          if (!dbUser.emailVerified) {
            await prisma.user.update({
              where: { id: dbUser.id },
              data: { emailVerified: new Date() }
            });
          }
        }

        // Always allow OAuth sign in - we'll handle username check elsewhere
        return true;
      }

      // For credentials provider, we want to ensure the user is verified.
      if (!user?.email) return false;

      const dbUser = await prisma.user.findUnique({
        where: { email: user.email },
      });

      if (!dbUser || !dbUser.emailVerified) {
        // Returning false will display a default error message
        // You can also throw an error with a specific message
        return false
      }

      return true
    },
    async jwt({ token, user }) {
      // If user object exists, it's the first sign-in, so we set the subject (user id)
      if (user) {
        token.sub = user.id
      }

      // On subsequent calls, token.sub will exist. 
      // We check the database to see if the username is still missing.
      if (token.sub) {
        const dbUser = await prisma.user.findUnique({
          where: { id: token.sub },
          select: { username: true }, // Only fetch the username
        });

        // Set or unset the needsUsername flag based on the latest db state
        token.needsUsername = !dbUser?.username;
      }

      return token
    },
    async session({ session, token }) {
      if (token) {
        session.user.id = token.sub!
        if (token.needsUsername === true) {
          session.user.needsUsername = true
        }
        
        // Add username to session
        if (token.sub) {
          const dbUser = await prisma.user.findUnique({
            where: { id: token.sub },
            select: { username: true },
          });
          session.user.username = dbUser?.username || null;
        }
      }
      return session
    },
  },
})