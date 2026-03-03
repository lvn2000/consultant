# API Reference

This document provides comprehensive information about the Consultant Backend API endpoints.

## Base URL

The API is served at: `http://localhost:8090/api` (or your configured server URL)

## Authentication

Most endpoints require authentication using JWT tokens:

```bash
Authorization: Bearer <token>
```

## API Endpoints

### Users

#### Create User
- **Endpoint**: `POST /api/users`
- **Description**: Create a new user account
- **Request Body**: 
  ```json
  {
    "login": "username",
    "email": "user@example.com",
    "password": "securePassword",
    "name": "Full Name",
    "phone": "+1234567890"
  }
  ```
- **Response**: Created user object

#### Get User
- **Endpoint**: `GET /api/users/:id`
- **Description**: Retrieve user information by ID
- **Response**: User object

#### List Users
- **Endpoint**: `GET /api/users?offset=0&limit=20`
- **Description**: List users with pagination
- **Query Parameters**:
  - `offset`: Number of records to skip (default: 0)
  - `limit`: Maximum number of records to return (default: 20)
- **Response**: Paginated list of users

#### Login
- **Endpoint**: `POST /api/auth/login`
- **Description**: Authenticate user and return JWT token
- **Request Body**:
  ```json
  {
    "login": "username",
    "password": "password"
  }
  ```
- **Response**: JWT token and user information

#### Logout
- **Endpoint**: `POST /api/auth/logout`
- **Description**: Invalidate refresh token
- **Request Body**:
  ```json
  {
    "refreshToken": "refresh-token"
  }
  ```

### Specialists

#### Register Specialist
- **Endpoint**: `POST /api/specialists`
- **Description**: Register a new specialist
- **Request Body**:
  ```json
  {
    "email": "specialist@example.com",
    "name": "Specialist Name",
    "phone": "+1234567890",
    "bio": "Brief bio",
    "hourlyRate": 75.00,
    "experienceYears": 5,
    "categoryIds": ["category-id-1", "category-id-2"]
  }
  ```

#### Get Specialist
- **Endpoint**: `GET /api/specialists/:id`
- **Description**: Retrieve specialist information by ID

#### Search Specialists
- **Endpoint**: `GET /api/specialists/search`
- **Description**: Search specialists with filters
- **Query Parameters**:
  - `categoryId`: Filter by category ID
  - `minRating`: Minimum rating threshold
  - `maxPrice`: Maximum hourly rate
  - `offset`: Number of records to skip
  - `limit`: Maximum number of records to return

#### Get Specialist Availability
- **Endpoint**: `GET /api/specialists/:id/availability`
- **Description**: Get specialist's available time slots

#### Update Specialist Availability
- **Endpoint**: `PUT /api/specialists/:id/availability`
- **Description**: Update specialist's available time slots

### Consultations

#### Create Consultation Request
- **Endpoint**: `POST /api/consultations`
- **Description**: Create a new consultation request
- **Request Body**:
  ```json
  {
    "specialistId": "specialist-id",
    "categoryId": "category-id",
    "description": "Consultation description",
    "scheduledAt": "2023-12-25T10:00:00Z",
    "duration": 60
  }
  ```

#### Get Consultation
- **Endpoint**: `GET /api/consultations/:id`
- **Description**: Retrieve consultation by ID

#### Get User Consultations
- **Endpoint**: `GET /api/consultations/user/:userId?offset=0&limit=20`
- **Description**: Get consultations for a specific user
- **Query Parameters**:
  - `offset`: Number of records to skip
  - `limit`: Maximum number of records to return

#### Get Specialist Consultations
- **Endpoint**: `GET /api/consultations/specialist/:specialistId?offset=0&limit=20`
- **Description**: Get consultations for a specific specialist
- **Query Parameters**:
  - `offset`: Number of records to skip
  - `limit`: Maximum number of records to return

#### Update Consultation Status
- **Endpoint**: `PUT /api/consultations/:id/status`
- **Description**: Update consultation status
- **Request Body**:
  ```json
  {
    "status": "Scheduled"  // Options: Requested, Scheduled, InProgress, Completed, Missed, Cancelled
  }
  ```

#### Approve Consultation
- **Endpoint**: `PUT /api/consultations/:id/approve`
- **Description**: Approve a consultation request with duration
- **Request Body**:
  ```json
  {
    "duration": 60
  }
  ```

#### Add Review
- **Endpoint**: `POST /api/consultations/:id/review`
- **Description**: Add a rating and review to a completed consultation
- **Request Body**:
  ```json
  {
    "rating": 5,
    "review": "Great consultation!"
  }
  ```

### Categories

#### Create Category
- **Endpoint**: `POST /api/categories`
- **Description**: Create a new category
- **Request Body**:
  ```json
  {
    "name": "Category Name",
    "description": "Category Description",
    "parentId": "parent-category-id"  // Optional for hierarchical structure
  }
  ```

#### Get Category
- **Endpoint**: `GET /api/categories/:id`
- **Description**: Retrieve category by ID

#### List Categories
- **Endpoint**: `GET /api/categories`
- **Description**: Get all categories

#### Update Category
- **Endpoint**: `PUT /api/categories/:id`
- **Description**: Update category information

#### Delete Category
- **Endpoint**: `DELETE /api/categories/:id`
- **Description**: Delete a category

### Connections

#### Get Connection Types
- **Endpoint**: `GET /api/connection-types`
- **Description**: Get all available connection types (Viber, WhatsApp, Slack, etc.)

#### Get Connection Type
- **Endpoint**: `GET /api/connection-types/:id`
- **Description**: Get specific connection type

#### Add Specialist Connection
- **Endpoint**: `POST /api/specialists/:specialistId/connections`
- **Description**: Add a connection method for a specialist
- **Request Body**:
  ```json
  {
    "connectionTypeId": "whatsapp-type-id",
    "connectionValue": "+1234567890"
  }
  ```

#### Get Specialist Connections
- **Endpoint**: `GET /api/specialists/:specialistId/connections`
- **Description**: Get all connections for a specialist

#### Get Specific Connection
- **Endpoint**: `GET /api/specialists/:specialistId/connections/:connectionId`
- **Description**: Get specific connection for a specialist

#### Update Connection
- **Endpoint**: `PUT /api/specialists/:specialistId/connections/:connectionId`
- **Description**: Update connection information

#### Delete Connection
- **Endpoint**: `DELETE /api/specialists/:specialistId/connections/:connectionId`
- **Description**: Remove a connection for a specialist

### Notification Preferences

#### Get Notification Preferences
- **Endpoint**: `GET /api/notification-preferences/:userId/:notificationType`
- **Description**: Get notification preferences for a user and notification type
- **Path Parameters**:
  - `userId`: User ID
  - `notificationType`: Type of notification

#### Update Notification Preferences
- **Endpoint**: `PUT /api/notification-preferences/:userId/:notificationType`
- **Description**: Update notification preferences for a user and notification type
- **Request Body**:
  ```json
  {
    "enabled": true,
    "channels": ["email", "sms"]
  }
  ```

### Health Check

#### API Health
- **Endpoint**: `GET /api/health`
- **Description**: Check API health status
- **Response**: Health status information

## Response Format

### Success Responses
```json
{
  "status": "success",
  "data": { /* response data */ }
}
```

### Error Responses
```json
{
  "status": "error",
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

## Error Codes

Common error codes returned by the API:

- `UNAUTHORIZED`: Authentication required or invalid
- `FORBIDDEN`: Insufficient permissions
- `NOT_FOUND`: Resource not found
- `VALIDATION_ERROR`: Invalid input data
- `DUPLICATE_RESOURCE`: Attempting to create duplicate resource
- `INTERNAL_ERROR`: Internal server error

## Rate Limiting

The API implements rate limiting to prevent abuse. Default limits:
- 100 requests per minute per IP
- 1000 requests per hour per authenticated user

## Pagination

Most list endpoints support pagination with these parameters:
- `offset`: Number of records to skip (default: 0)
- `limit`: Maximum number of records to return (default: 20, max: 100)

The response includes pagination metadata:
```json
{
  "items": [/* list of items */],
  "pagination": {
    "offset": 0,
    "limit": 20,
    "total": 150,
    "pages": 8
  }
}
```

## Content Types

The API accepts and returns JSON content by default. Set appropriate headers:
- `Content-Type: application/json`
- `Accept: application/json`