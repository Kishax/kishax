import { NextRequest, NextResponse } from "next/server"
import { PrismaClient } from "@prisma/client"
import { auth } from "@/lib/auth"
import { McAuthRequestSchema, McAuthResponseSchema } from "@/lib/schemas"
import { validateRequest, requireAuth, createErrorResponse } from "@/lib/api-middleware"
import jwt from "jsonwebtoken"
import { sendSocketMessage } from "@/lib/socket-client"

const prisma = new PrismaClient()

/**
 * @swagger
 * /api/mc/auth:
 *   post:
 *     summary: Authenticate Minecraft player
 *     description: Complete Minecraft player authentication with one-time password
 *     security:
 *       - sessionAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               token:
 *                 type: string
 *                 description: JWT token containing player info
 *               mcid:
 *                 type: string
 *                 description: Minecraft player ID
 *               uuid:
 *                 type: string
 *                 description: Minecraft player UUID
 *               pass:
 *                 type: string
 *                 pattern: '^\\d{6}$'
 *                 description: 6-digit one-time password
 *             required:
 *               - token
 *               - mcid
 *               - uuid
 *               - pass
 *     responses:
 *       200:
 *         description: Authentication successful
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 message:
 *                   type: string
 *                 user:
 *                   type: object
 *                   properties:
 *                     id:
 *                       type: string
 *                     mcid:
 *                       type: string
 *                     uuid:
 *                       type: string
 *       400:
 *         description: Invalid request or authentication failed
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 *     tags:
 *       - Minecraft
 */
export async function POST(req: NextRequest) {
  try {
    // Require authentication
    const authResult = await requireAuth()
    if (authResult.error) {
      return authResult.error
    }
    
    const session = authResult.session

    // Validate request
    const validation = await validateRequest(McAuthRequestSchema)(req)
    if (validation.error) {
      return validation.error
    }
    
    const { token, mcid, uuid, pass } = validation.data

    // Verify JWT token
    let payload: any
    try {
      payload = jwt.verify(token, process.env.NEXTAUTH_SECRET!)
    } catch (error) {
      return createErrorResponse("Invalid token", "Token verification failed", 400)
    }

    // Verify token payload matches request
    if (payload.mcid !== mcid || payload.uuid !== uuid) {
      return createErrorResponse("Invalid access", "Token data mismatch", 400)
    }

    // Get member from database
    const member = await prisma.member.findFirst({
      where: {
        name: mcid,
        uuid: uuid
      }
    })

    if (!member) {
      return createErrorResponse("Invalid access", "Member not found", 400)
    }

    // Check if already confirmed
    if (member.confirm) {
      const response = McAuthResponseSchema.parse({
        success: false,
        message: "認証済みユーザーです。"
      })
      return NextResponse.json(response)
    }

    // Verify one-time password
    if (member.secret2 !== pass) {
      // Reset OTP on failed attempt
      await prisma.member.update({
        where: { id: member.id },
        data: { secret2: null }
      })

      const response = McAuthResponseSchema.parse({
        success: false,
        message: "ワンタイムパスワードが異なるため、リセットしました。マイクラサーバーで/retryコマンドよりワンタイムパスワードを再生成してください。"
      })
      return NextResponse.json(response)
    }

    // Complete authentication
    await prisma.member.update({
      where: { id: member.id },
      data: {
        confirm: true,
        secret2: null,
        memberId: session.user.id
      }
    })

    // Send notification to Minecraft server
    try {
      const message = {
        web: {
          confirm: {
            who: {
              name: mcid,
              uuid: uuid
            }
          }
        }
      }
      await sendSocketMessage(JSON.stringify(message) + '\r\n')
    } catch (error) {
      console.warn("Failed to send socket message:", error)
      // Don't fail the request if socket message fails
    }

    const response = McAuthResponseSchema.parse({
      success: true,
      message: "WEB認証に成功しました。",
      user: {
        id: session.user.id,
        mcid: member.name,
        uuid: member.uuid
      }
    })

    return NextResponse.json(response)
  } catch (error) {
    console.error("MC Auth API error:", error)
    return createErrorResponse(
      "Internal Server Error",
      "Failed to authenticate",
      500
    )
  }
}