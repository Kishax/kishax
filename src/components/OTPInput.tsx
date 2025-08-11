'use client'

import { useState, useRef, useEffect } from 'react'

interface OTPInputProps {
  length?: number
  onComplete: (otp: string) => void
  disabled?: boolean
  error?: string
}

export default function OTPInput({ 
  length = 6, 
  onComplete, 
  disabled = false, 
  error 
}: OTPInputProps) {
  const [otp, setOtp] = useState<string[]>(Array(length).fill(''))
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  useEffect(() => {
    // Focus first input on mount
    if (inputRefs.current[0]) {
      inputRefs.current[0].focus()
    }
  }, [])

  const handleChange = (index: number, value: string) => {
    if (disabled) return

    // Only allow digits
    if (value && !/^\d$/.test(value)) return

    const newOtp = [...otp]
    newOtp[index] = value

    setOtp(newOtp)

    // Move to next input if value entered
    if (value && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
    }

    // Call onComplete if all fields are filled
    if (newOtp.every(digit => digit !== '')) {
      onComplete(newOtp.join(''))
    }
  }

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return

    if (e.key === 'Backspace') {
      if (otp[index] === '' && index > 0) {
        // Move to previous input if current is empty
        inputRefs.current[index - 1]?.focus()
      } else {
        // Clear current input
        const newOtp = [...otp]
        newOtp[index] = ''
        setOtp(newOtp)
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus()
    } else if (e.key === 'ArrowRight' && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  const handlePaste = (e: React.ClipboardEvent) => {
    if (disabled) return

    e.preventDefault()
    const pastedData = e.clipboardData.getData('text/plain').replace(/\D/g, '')
    
    if (pastedData.length === length) {
      const newOtp = pastedData.split('')
      setOtp(newOtp)
      onComplete(pastedData)
      
      // Focus last input
      inputRefs.current[length - 1]?.focus()
    }
  }

  const clearOTP = () => {
    setOtp(Array(length).fill(''))
    inputRefs.current[0]?.focus()
  }

  return (
    <div className="flex flex-col items-center space-y-4">
      <div className="flex space-x-3">
        {otp.map((digit, index) => (
          <input
            key={index}
            ref={(el) => { inputRefs.current[index] = el }}
            type="text"
            inputMode="numeric"
            pattern="\d"
            maxLength={1}
            value={digit}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            disabled={disabled}
            className={`
              w-12 h-12 text-center text-xl font-bold border-2 rounded-lg
              focus:outline-none focus:ring-2 transition-all duration-200
              ${error 
                ? 'border-red-500 focus:border-red-500 focus:ring-red-200' 
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-200'
              }
              ${disabled 
                ? 'bg-gray-100 cursor-not-allowed' 
                : 'bg-white hover:border-gray-400'
              }
            `}
            aria-label={`OTP digit ${index + 1}`}
          />
        ))}
      </div>
      
      {error && (
        <p className="text-red-500 text-sm text-center">{error}</p>
      )}
      
      <button
        type="button"
        onClick={clearOTP}
        disabled={disabled || otp.every(digit => digit === '')}
        className="text-sm text-gray-500 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        クリア
      </button>
    </div>
  )
}