import { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  const baseUrl = process.env.NEXTAUTH_URL || 'http://localhost:3000'

  return {
    rules: {
      userAgent: '*',
      allow: ['/', '/signin', '/signup', '/app/chart', '/docs'],
      disallow: ['/api/', '/mc/auth', '/auth/'],
    },
    sitemap: `${baseUrl}/sitemap.xml`,
  }
}