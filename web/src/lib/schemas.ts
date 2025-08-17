import { z } from "zod"

// Counter API schemas
export const CounterQuerySchema = z.object({
  type: z.enum(["year", "month", "last7days"]).optional().default("last7days")
})

export const CounterDataSchema = z.object({
  date: z.string(),
  count: z.number()
})

export const CounterResponseSchema = z.object({
  data: z.array(CounterDataSchema).nullable(),
  error: z.string().nullable()
})

// Avatar API schemas
export const AvatarQuerySchema = z.object({
  userId: z.string().optional()
})

// Common API schemas
export const ErrorResponseSchema = z.object({
  error: z.string(),
  message: z.string().optional(),
  statusCode: z.number().optional()
})

export const SuccessResponseSchema = z.object({
  message: z.string(),
  data: z.unknown().optional()
})

// Auth API schemas
export const SignupRequestSchema = z.object({
  username: z.string().min(3).max(50),
  password: z.string().min(6).max(100)
})

export const SignupResponseSchema = z.object({
  message: z.string(),
  userId: z.string()
})

// MC Auth API schemas
export const McAuthRequestSchema = z.object({
  token: z.string(),
  mcid: z.string(),
  uuid: z.string(),
  pass: z.string().regex(/^\d{6}$/, "Must be 6 digits")
})

export const McAuthResponseSchema = z.object({
  success: z.boolean(),
  message: z.string(),
  user: z.object({
    id: z.string(),
    mcid: z.string(),
    uuid: z.string()
  }).optional()
})

export const McAuthPageDataSchema = z.object({
  isAuth: z.boolean(),
  username: z.string().optional(),
  mcid: z.string().optional(),
  uuid: z.string().optional(),
  mcAuth: z.boolean(),
  token: z.string().optional(),
  successMessage: z.array(z.string()).optional(),
  errorMessage: z.array(z.string()).optional(),
  infoMessage: z.array(z.string()).optional()
})

export const McAuthQuerySchema = z.object({
  n: z.string().optional().transform(val => val ? parseInt(val) : undefined)
})

// Type exports
export type CounterQuery = z.infer<typeof CounterQuerySchema>
export type CounterData = z.infer<typeof CounterDataSchema>
export type CounterResponse = z.infer<typeof CounterResponseSchema>
export type AvatarQuery = z.infer<typeof AvatarQuerySchema>
export type ErrorResponse = z.infer<typeof ErrorResponseSchema>
export type SuccessResponse = z.infer<typeof SuccessResponseSchema>
export type SignupRequest = z.infer<typeof SignupRequestSchema>
export type SignupResponse = z.infer<typeof SignupResponseSchema>
export type McAuthRequest = z.infer<typeof McAuthRequestSchema>
export type McAuthResponse = z.infer<typeof McAuthResponseSchema>
export type McAuthPageData = z.infer<typeof McAuthPageDataSchema>
export type McAuthQuery = z.infer<typeof McAuthQuerySchema>