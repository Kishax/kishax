import { createConnection } from "net"

interface SocketConfig {
  host: string
  port: number
  timeout?: number
}

// Default configuration - should be moved to environment variables
const defaultConfig: SocketConfig = {
  host: process.env.MC_SOCKET_HOST || "localhost",
  port: parseInt(process.env.MC_SOCKET_PORT || "9999"),
  timeout: 5000
}

export async function sendSocketMessage(message: string, config: SocketConfig = defaultConfig): Promise<boolean> {
  return new Promise((resolve) => {
    const client = createConnection(config.port, config.host)
    
    client.setTimeout(config.timeout || 5000)
    
    client.on('connect', () => {
      console.log('Connected to Minecraft server socket')
      client.write(message)
      client.end()
      resolve(true)
    })
    
    client.on('error', (error) => {
      console.error('Socket connection error:', error)
      resolve(false)
    })
    
    client.on('timeout', () => {
      console.error('Socket connection timeout')
      client.destroy()
      resolve(false)
    })
    
    client.on('close', () => {
      console.log('Socket connection closed')
    })
  })
}

export async function testSocketConnection(config: SocketConfig = defaultConfig): Promise<boolean> {
  try {
    return await sendSocketMessage('{"test": true}\r\n', config)
  } catch (error) {
    console.error('Socket test failed:', error)
    return false
  }
}