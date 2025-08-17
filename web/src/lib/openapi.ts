import swaggerJSDoc from 'swagger-jsdoc'

const options: swaggerJSDoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'KishaX API',
      version: '1.0.0',
      description: 'API documentation for KishaX web application',
      contact: {
        name: 'KishaX Team',
        email: 'contact@kishax.com'
      }
    },
    servers: [
      {
        url: process.env.NEXTAUTH_URL || 'http://localhost:3000',
        description: 'Development server'
      },
      {
        url: 'https://your-production-domain.com',
        description: 'Production server'
      }
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT'
        },
        sessionAuth: {
          type: 'apiKey',
          in: 'cookie',
          name: 'next-auth.session-token'
        }
      },
      schemas: {
        Error: {
          type: 'object',
          properties: {
            error: {
              type: 'string',
              description: 'Error type'
            },
            message: {
              type: 'string',
              description: 'Error message'
            },
            statusCode: {
              type: 'number',
              description: 'HTTP status code'
            }
          },
          required: ['error']
        },
        CounterData: {
          type: 'object',
          properties: {
            date: {
              type: 'string',
              format: 'date',
              description: 'Date in YYYY-MM-DD format'
            },
            count: {
              type: 'number',
              description: 'Counter value for the date'
            }
          },
          required: ['date', 'count']
        },
        CounterResponse: {
          type: 'object',
          properties: {
            data: {
              type: 'array',
              items: {
                $ref: '#/components/schemas/CounterData'
              },
              nullable: true
            },
            error: {
              type: 'string',
              nullable: true
            }
          }
        },
        SignupRequest: {
          type: 'object',
          properties: {
            username: {
              type: 'string',
              minLength: 3,
              maxLength: 50,
              description: 'Username for the new account'
            },
            password: {
              type: 'string',
              minLength: 6,
              maxLength: 100,
              description: 'Password for the new account'
            }
          },
          required: ['username', 'password']
        },
        SignupResponse: {
          type: 'object',
          properties: {
            message: {
              type: 'string',
              description: 'Success message'
            },
            userId: {
              type: 'string',
              description: 'ID of the created user'
            }
          },
          required: ['message', 'userId']
        }
      }
    },
    tags: [
      {
        name: 'Authentication',
        description: 'User authentication and registration'
      },
      {
        name: 'Statistics',
        description: 'Application statistics and analytics'
      },
      {
        name: 'User',
        description: 'User profile and data'
      },
      {
        name: 'Minecraft',
        description: 'Minecraft server integration'
      }
    ]
  },
  apis: [
    './src/app/api/**/route.ts',
    './src/app/api/**/*.ts'
  ]
}

export const swaggerSpec = swaggerJSDoc(options)

export function getOpenAPISpec() {
  return swaggerSpec
}