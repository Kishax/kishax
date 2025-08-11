import { NextRequest, NextResponse } from "next/server"
import { PrismaClient } from "@prisma/client"
import { auth } from "@/lib/auth"
import { AvatarQuerySchema } from "@/lib/schemas"
import { validateRequest, createErrorResponse } from "@/lib/api-middleware"
import fs from "fs/promises"
import path from "path"

const prisma = new PrismaClient()

/**
 * @swagger
 * /api/avatar:
 *   get:
 *     summary: Get user avatar
 *     description: Retrieve user avatar image. Returns custom avatar for authenticated users, random default for others.
 *     parameters:
 *       - in: query
 *         name: userId
 *         schema:
 *           type: string
 *         description: Optional user ID to get specific user's avatar
 *     responses:
 *       200:
 *         description: Avatar image
 *         content:
 *           image/png:
 *             schema:
 *               type: string
 *               format: binary
 *           image/jpeg:
 *             schema:
 *               type: string
 *               format: binary
 *           image/gif:
 *             schema:
 *               type: string
 *               format: binary
 *       302:
 *         description: Redirect to external avatar URL
 *       404:
 *         description: Avatar not found
 *       500:
 *         description: Internal server error
 *     tags:
 *       - User
 */
export async function GET(req: NextRequest) {
  try {
    // Get current session
    const session = await auth()
    
    // Validate request (optional userId parameter)
    const validation = await validateRequest(AvatarQuerySchema)(req)
    if (validation.error) {
      return validation.error
    }
    
    const { userId } = validation.data

    // If authenticated and no specific userId requested, use session user
    if (session?.user && !userId) {
      const user = await prisma.user.findUnique({
        where: { id: session.user.id }
      })

      if (user) {
        // Check for custom avatar
        if (user.customAvatar) {
          return NextResponse.redirect(user.customAvatar)
        }
        
        // Check for Discord avatar
        if (user.avatar && user.discordId) {
          const isAnimated = user.avatar.startsWith('a_')
          const extension = isAnimated ? 'gif' : 'png'
          const discordAvatarUrl = `https://cdn.discordapp.com/avatars/${user.discordId}/${user.avatar}.${extension}`
          return NextResponse.redirect(discordAvatarUrl)
        }
        
        // Check for other avatar URL
        if (user.avatar && isUrl(user.avatar)) {
          return NextResponse.redirect(user.avatar)
        }
      }
    }

    // Fall back to random default avatar
    const avatarsDir = path.join(process.cwd(), 'public', 'images', 'avatar', 'default')
    
    try {
      const files = await fs.readdir(avatarsDir)
      const imageFiles = files.filter(file => 
        file.match(/\.(png|jpg|jpeg|gif)$/i)
      )

      if (imageFiles.length === 0) {
        return createErrorResponse("No avatar files found", undefined, 404)
      }

      const randomIndex = Math.floor(Math.random() * imageFiles.length)
      const randomFileName = imageFiles[randomIndex]
      const imagePath = path.join(avatarsDir, randomFileName)

      const imageBuffer = await fs.readFile(imagePath)
      const mimeType = getMimeType(randomFileName)

      return new NextResponse(imageBuffer, {
        status: 200,
        headers: {
          'Content-Type': mimeType,
          'Cache-Control': 'public, max-age=31536000'
        }
      })
    } catch (error) {
      console.error('Error reading avatar directory:', error)
      return createErrorResponse("Error serving avatar", undefined, 500)
    }
  } catch (error) {
    console.error("Avatar API error:", error)
    return createErrorResponse(
      "Internal Server Error",
      "Failed to retrieve avatar",
      500
    )
  }
}

// Helper function to determine MIME type
function getMimeType(filename: string): string {
  const ext = path.extname(filename).toLowerCase()
  switch (ext) {
    case '.png':
      return 'image/png'
    case '.jpg':
    case '.jpeg':
      return 'image/jpeg'
    case '.gif':
      return 'image/gif'
    default:
      return 'application/octet-stream'
  }
}

// Helper function to check if string is URL
function isUrl(string: string): boolean {
  try {
    new URL(string)
    return true
  } catch {
    return false
  }
}