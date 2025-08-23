import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  
  // App Runner用の設定
  output: 'standalone',
  
  // Server External Packages設定
  serverExternalPackages: ['@auth/core', 'bcrypt'],
  
  // 画像最適化設定（App Runner用）
  images: {
    domains: ['cdn.discordapp.com', 'localhost'],
    unoptimized: process.env.NODE_ENV === 'production'
  },
  
  // セキュリティヘッダー
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'X-Frame-Options',
            value: 'DENY'
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff'
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin'
          },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()'
          }
        ]
      }
    ]
  },
  
  // リダイレクト設定
  async redirects() {
    return [
      {
        source: '/logout',
        destination: '/api/auth/signout',
        permanent: false
      }
    ]
  }
};

export default nextConfig;
