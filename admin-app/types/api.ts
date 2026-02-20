/**
 * Centralized API types for the admin application.
 * These types ensure type safety across all API interactions.
 */

export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  offset: number
  limit: number
}

/**
 * Base user interface shared by all user roles
 */
export interface User {
  id: string
  login: string
  email: string
  name: string
  phone?: string | null
  role: 'Client' | 'Specialist' | 'Admin'
  createdAt: string
  updatedAt: string
  consultationIds?: string[]
}

/**
 * Specialist user with extended properties
 */
export interface Specialist extends User {
  role: 'Specialist'
  bio?: string | null
  isAvailable: boolean
  categoryRates: SpecialistCategoryRate[]
  connections?: SpecialistConnection[]
}

/**
 * Specialist rate for a specific category
 */
export interface SpecialistCategoryRate {
  id?: string
  categoryId: string
  categoryName?: string
  hourlyRate: number
  experienceYears: number
}

/**
 * Specialist connection (e.g., WhatsApp, Telegram)
 */
export interface SpecialistConnection {
  id: string
  specialistId: string
  connectionTypeId: string
  connectionTypeName?: string
  connectionValue: string
  isVerified: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * Client user (simplified, no special properties)
 */
export interface Client extends User {
  role: 'Client'
}

/**
 * Category for specialist expertise
 */
export interface Category {
  id: string
  name: string
  description?: string | null
  parentId?: string | null
  parentName?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * Connection type (e.g., WhatsApp, Email, Phone)
 */
export interface ConnectionType {
  id: string
  name: string
  description?: string | null
  createdAt?: string
  updatedAt?: string
}

/**
 * Notification preference for a user
 */
export interface NotificationPreference {
  id: string
  userId: string
  notificationType: string
  emailEnabled: boolean
  smsEnabled: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * Login request payload
 */
export interface LoginRequest {
  login: string
  password: string
}

/**
 * Login response with session data
 */
export interface LoginResponse {
  userId: string
  login: string
  email: string
  role: string
  sessionId: string
  accessToken?: string
}

/**
 * Registration request payload
 */
export interface RegisterRequest {
  login: string
  email: string
  password: string
  name: string
  phone?: string | null
  role: string
}

/**
 * Registration response
 */
export interface RegisterResponse {
  accessToken: string
  refreshToken: string
  expiresAt: string
  userId: string
  login: string
  email: string
  name: string
  role: string
}

/**
 * Admin registration response (no auto-login)
 */
export interface AdminRegisterResponse {
  userId: string
  login: string
  email: string
  name: string
  role: string
}

/**
 * Available time slot for consultation booking
 */
export interface TimeSlot {
  startTime: string
  endTime: string
  available: boolean
}

/**
 * Consultation status enum
 */
export type ConsultationStatus =
  | 'Requested'
  | 'Scheduled'
  | 'InProgress'
  | 'Completed'
  | 'Missed'
  | 'Cancelled'

/**
 * Consultation entity
 */
export interface Consultation {
  id: string
  specialistId: string
  clientId: string
  specialistName?: string
  clientName?: string
  categoryId?: string
  categoryName?: string
  description: string
  status: ConsultationStatus
  scheduledDate?: string
  durationMinutes?: number
  price?: number
  createdAt: string
  updatedAt: string
}
