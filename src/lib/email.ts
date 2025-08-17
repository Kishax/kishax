import nodemailer, { Transporter } from 'nodemailer'
import SMTPTransport from 'nodemailer/lib/smtp-transport'

let transporter: Transporter | null = null

function createTransporter(): Transporter {
  if (transporter) {
    return transporter
  }

  const port = parseInt(process.env.EMAIL_PORT || '587')
  
  const config: SMTPTransport.Options = {
    host: process.env.EMAIL_HOST || 'smtp.gmail.com',
    port: port,
    secure: port === 465,
    requireTLS: true,
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS,
    },
  }
  
  transporter = nodemailer.createTransport(config)

  return transporter
}

export async function sendOneTimePassword(recipient: string, otp: string): Promise<boolean> {
  try {
    const transporter = createTransporter()
    
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>ワンタイムパスワード - KishaX</title>
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
          .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
          .header { text-align: center; margin-bottom: 30px; }
          .logo { font-size: 24px; font-weight: bold; color: #2563eb; margin-bottom: 10px; }
          .title { font-size: 20px; margin-bottom: 20px; color: #1f2937; }
          .otp-code { font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #dc2626; background: #fef2f2; padding: 15px; border-radius: 6px; text-align: center; margin: 20px 0; }
          .message { color: #4b5563; line-height: 1.6; margin-bottom: 20px; }
          .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; color: #92400e; }
          .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 14px; text-align: center; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <div class="logo">KishaX</div>
            <h1 class="title">ワンタイムパスワード</h1>
          </div>
          
          <div class="message">
            <p>こんにちは、</p>
            <p>KishaXアカウントのログインに必要なワンタイムパスワードをお送りします。</p>
          </div>
          
          <div class="otp-code">${otp}</div>
          
          <div class="message">
            <p>このコードは<strong>10分間</strong>有効です。ログイン画面で上記のコードを入力してください。</p>
          </div>
          
          <div class="warning">
            <strong>⚠ セキュリティ上の注意</strong><br>
            • このコードは他人に教えないでください<br>
            • 身に覚えのないリクエストの場合は、このメールを無視してください<br>
            • 不審なアクティビティを検出した場合は、すぐにパスワードを変更してください
          </div>
          
          <div class="footer">
            <p>このメールは自動送信されています。返信はできません。</p>
            <p>&copy; 2025 KishaX. All rights reserved.</p>
          </div>
        </div>
      </body>
      </html>
    `

    const mailOptions = {
      from: `"KishaX" <${process.env.EMAIL_FROM || process.env.EMAIL_USER}>`,
      to: recipient,
      subject: '[KishaX] ワンタイムパスワード',
      html,
    }

    const info = await transporter.sendMail(mailOptions)
    console.log('OTP email sent successfully: %s', info.messageId)

    return true
  } catch (error) {
    console.error('Error sending OTP email:', error)
    return false
  }
}

export async function sendVerificationEmail(recipient: string, verificationUrl: string): Promise<boolean> {
  try {
    const transporter = createTransporter()
    
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>メールアドレス認証 - KishaX</title>
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
          .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
          .header { text-align: center; margin-bottom: 30px; }
          .logo { font-size: 24px; font-weight: bold; color: #2563eb; margin-bottom: 10px; }
          .title { font-size: 20px; margin-bottom: 20px; color: #1f2937; }
          .message { color: #4b5563; line-height: 1.6; margin-bottom: 20px; }
          .button { display: inline-block; background-color: #2563eb; color: white; padding: 15px 30px; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 20px 0; }
          .button:hover { background-color: #1d4ed8; }
          .url { background: #f3f4f6; padding: 15px; border-radius: 6px; word-break: break-all; font-family: monospace; font-size: 14px; margin: 15px 0; }
          .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 14px; text-align: center; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <div class="logo">KishaX</div>
            <h1 class="title">メールアドレス認証</h1>
          </div>
          
          <div class="message">
            <p>こんにちは、</p>
            <p>KishaXアカウントの登録ありがとうございます。以下のボタンをクリックして、メールアドレスの認証を完了してください。</p>
          </div>
          
          <div style="text-align: center;">
            <a href="${verificationUrl}" class="button">メールアドレスを認証する</a>
          </div>
          
          <div class="message">
            <p>ボタンが機能しない場合は、以下のURLをコピーしてブラウザに貼り付けてください：</p>
          </div>
          
          <div class="url">${verificationUrl}</div>
          
          <div class="message">
            <p>このリンクは<strong>24時間</strong>有効です。期限が切れた場合は、再度登録手続きを行ってください。</p>
          </div>
          
          <div class="footer">
            <p>このメールは自動送信されています。返信はできません。</p>
            <p>&copy; 2025 KishaX. All rights reserved.</p>
          </div>
        </div>
      </body>
      </html>
    `

    const mailOptions = {
      from: `"KishaX" <${process.env.EMAIL_FROM || process.env.EMAIL_USER}>`,
      to: recipient,
      subject: '[KishaX] メールアドレス認証のお願い',
      html,
    }

    const info = await transporter.sendMail(mailOptions)
    console.log('Verification email sent successfully: %s', info.messageId)

    return true
  } catch (error) {
    console.error('Error sending verification email:', error)
    return false
  }
}