import type { NextAuthConfig } from 'next-auth';

export const authConfig = {
  pages: {
    signIn: '/signin',
  },
  providers: [
    // プロバイダーの定義はメインのauth.tsに置くため、ここは空の配列のままにします。
    // middlewareはプロバイダー設定を必要としないため、これで問題ありません。
  ],
} satisfies NextAuthConfig;
