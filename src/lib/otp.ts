// OTP（ワンタイムパスワード）ユーティリティ関数
// 現在は招待リンク認証を使用しているが、将来的なOTP機能のために保持
import crypto from 'crypto'

export interface OTPRecord {
  code: string
  email: string
  createdAt: Date
  expiresAt: Date
  attempts: number
}

// In-memory storage for OTP codes (production should use Redis or database)
const otpStorage = new Map<string, OTPRecord>()

// OTP configuration  
const OTP_EXPIRY_MINUTES = 10
const MAX_ATTEMPTS = 3

/**
 * Generate a random OTP code
 */
export function generateOTP(): string {
  return crypto.randomInt(100000, 999999).toString()
}

/**
 * Store OTP for an email address
 */
export function storeOTP(email: string, code: string): void {
  const now = new Date()
  const expiresAt = new Date(now.getTime() + OTP_EXPIRY_MINUTES * 60 * 1000)
  
  otpStorage.set(email, {
    code,
    email,
    createdAt: now,
    expiresAt,
    attempts: 0
  })
  
  // Clean up expired OTPs periodically
  cleanupExpiredOTPs()
}

/**
 * Verify OTP for an email address
 */
export function verifyOTP(email: string, inputCode: string): { success: boolean, message: string } {
  const record = otpStorage.get(email)
  
  if (!record) {
    return { success: false, message: 'OTPコードが見つかりません。新しいコードを要求してください。' }
  }
  
  // Check if expired
  if (new Date() > record.expiresAt) {
    otpStorage.delete(email)
    return { success: false, message: 'OTPコードの有効期限が切れています。新しいコードを要求してください。' }
  }
  
  // Check attempts limit
  if (record.attempts >= MAX_ATTEMPTS) {
    otpStorage.delete(email)
    return { success: false, message: '試行回数が上限に達しました。新しいコードを要求してください。' }
  }
  
  // Increment attempts
  record.attempts++
  
  // Verify code
  if (record.code !== inputCode) {
    return { 
      success: false, 
      message: `OTPコードが正しくありません。残り${MAX_ATTEMPTS - record.attempts}回試行できます。` 
    }
  }
  
  // Success - remove from storage
  otpStorage.delete(email)
  return { success: true, message: 'OTP認証に成功しました。' }
}

/**
 * Check if OTP exists for email
 */
export function hasActiveOTP(email: string): boolean {
  const record = otpStorage.get(email)
  if (!record) return false
  
  // Check if expired
  if (new Date() > record.expiresAt) {
    otpStorage.delete(email)
    return false
  }
  
  return true
}

/**
 * Get remaining time for OTP
 */
export function getOTPRemainingTime(email: string): number {
  const record = otpStorage.get(email)
  if (!record) return 0
  
  const now = new Date()
  if (now > record.expiresAt) {
    otpStorage.delete(email)
    return 0
  }
  
  return Math.floor((record.expiresAt.getTime() - now.getTime()) / 1000)
}

/**
 * Clean up expired OTP records
 */
function cleanupExpiredOTPs(): void {
  const now = new Date()
  
  for (const [email, record] of otpStorage.entries()) {
    if (now > record.expiresAt) {
      otpStorage.delete(email)
    }
  }
}

/**
 * Get OTP statistics (for debugging)
 */
export function getOTPStats(): { totalActive: number, records: OTPRecord[] } {
  cleanupExpiredOTPs()
  
  return {
    totalActive: otpStorage.size,
    records: Array.from(otpStorage.values())
  }
}