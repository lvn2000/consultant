# User Notification Preferences - API Reference

## Overview
These endpoints allow authenticated users to manage their notification preferences. Users can enable or disable notifications by channel (email, SMS) for each notification type.

**Security Requirements:**
- All endpoints require Authorization header with Bearer token (sessionId)
- X-User-Id header identifies whose preferences to access
- Users can access their own preferences
- Administrators can access any user's preferences (role-based, enforced server-side)

### Endpoint 1: Get All Preferences

**Endpoint:** `GET /api/notification-preferences`

**Authentication Requirements:**
- `Authorization` header: Bearer token (sessionId from login response)
- `X-User-Id` header: UUID of the user whose preferences to retrieve

**Description:** Retrieves all notification preferences for the specified user. If no preferences exist, they are automatically created with default values (all enabled).

```bash
curl -X GET http://localhost:8090/api/notification-preferences \
  -H "Authorization: Bearer <sessionId>" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "preferences": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationApproved",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440002",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationDeclined",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440003",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCompleted",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440004",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationMissed",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440005",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCancelled",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    }
  ]
}
```

**Error Responses:**

Missing or invalid Authorization header:
```json
{
  "type": "UNAUTHORIZED",
  "message": "Missing or invalid Authorization header"
}
```

Missing X-User-Id header:
```json
{
  "type": "BAD_REQUEST",
  "message": "Missing X-User-Id header"
}
```

### Endpoint 2: Update Preference by Notification Type

**Endpoint:** `PUT /api/notification-preferences/{notificationType}`

**Authentication Requirements:**
- `Authorization` header: Bearer token (sessionId from login response)
- `X-User-Id` header: UUID of the user whose preference to update

**Path Parameters:**
- `notificationType`: One of: `ConsultationApproved`, `ConsultationDeclined`, `ConsultationCompleted`, `ConsultationMissed`, `ConsultationCancelled`

**Request Body:**
```json
{
  "emailEnabled": false,
  "smsEnabled": true
}
```

**Description:** Updates notification preferences for a specific notification type. Users can update their own preferences, and administrators can update any user's preferences.

```bash
curl -X PUT http://localhost:8090/api/notification-preferences/ConsultationApproved \
  -H "Authorization: Bearer <sessionId>" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "emailEnabled": false,
    "smsEnabled": true
  }'
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "notificationType": "ConsultationApproved",
  "emailEnabled": false,
  "smsEnabled": true,
  "createdAt": "2026-02-01T10:30:00Z",
  "updatedAt": "2026-02-04T15:45:00Z"
}
```

**Error Responses:**

Unauthorized (missing or invalid Authorization header):
```json
{
  "type": "UNAUTHORIZED",
  "message": "Missing or invalid Authorization header"
}
```

Missing X-User-Id header:
```json
{
  "type": "BAD_REQUEST",
  "message": "Missing X-User-Id header"
}
```

Preference not found:
```json
{
  "type": "NOT_FOUND",
  "message": "Preference not found"
}
```

Invalid notification type:
```json
{
  "type": "VALIDATION_ERROR",
  "message": "Invalid notification type"
}
```

### Notification Types

The system supports the following notification types:

| Type | Triggered When | Default |
|------|--------|---------|
| `ConsultationApproved` | Specialist approves a consultation request | Email & SMS enabled |
| `ConsultationDeclined` | Specialist declines a consultation request | Email & SMS enabled |
| `ConsultationCompleted` | Consultation is marked as completed | Email & SMS enabled |
| `ConsultationMissed` | Consultation is marked as missed | Email & SMS enabled |
| `ConsultationCancelled` | Consultation is cancelled | Email & SMS enabled |

---

## Security Implementation

### Authentication Flow
1. **User logs in** → Receives `sessionId` in login response
2. **Client stores** `sessionId` in sessionStorage
3. **All requests to notification endpoints** include:
   - `Authorization: Bearer <sessionId>` - Server validates the session is valid
   - `X-User-Id: <userId>` - Identifies whose preferences to access

### Authorization Rules
- **Users**: Can access and modify their own preferences only
- **Admins**: Can access and modify preferences for any user  
- **Server-side validation**: Enforces role-based access control (admins vs. regular users)

### Important Security Notes
1. **Never share your sessionId** - It validates your identity
2. **Authorization header is required** - Client-only supply of X-User-Id is insufficient
3. **Session validation happens server-side** - The server verifies the session before allowing access
4. **Role checking happens server-side** - Only authorized users can access other users' preferences
5. **HTTPS required in production** - Protect sessionId in transit

---

## Integration Examples

### Python/Requests (User accessing their own preferences)
```python
import requests

session_id = "your-session-id-from-login"
user_id = "550e8400-e29b-41d4-a716-446655440000"

headers = {
    "Authorization": f"Bearer {session_id}",
    "X-User-Id": user_id
}

# Get preferences
response = requests.get(
    "http://localhost:8090/api/notification-preferences",
    headers=headers
)
print(response.json())

# Update preference
response = requests.put(
    "http://localhost:8090/api/notification-preferences/ConsultationApproved",
    headers=headers,
    json={"emailEnabled": False, "smsEnabled": True}
)
print(response.json())
```

### JavaScript/Fetch (User accessing their own preferences)
```javascript
const sessionId = sessionStorage.getItem('sessionId');
const userId = sessionStorage.getItem('userId');

const headers = {
  "Authorization": `Bearer ${sessionId}`,
  "X-User-Id": userId
};

// Get preferences
fetch("http://localhost:8090/api/notification-preferences", {
  method: "GET",
  headers: headers
})
.then(r => r.json())
.then(data => console.log(data));

// Update preference
fetch("http://localhost:8090/api/notification-preferences/ConsultationApproved", {
  method: "PUT",
  headers: {
    ...headers,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    emailEnabled: false,
    smsEnabled: true
  })
})
.then(r => r.json())
.then(data => console.log(data));
```

### JavaScript/Fetch (Admin accessing other user's preferences)
```javascript
const adminSessionId = sessionStorage.getItem('sessionId');
const targetUserId = "550e8400-e29b-41d4-a716-446655440001"; // Another user's ID

const headers = {
  "Authorization": `Bearer ${adminSessionId}`,
  "X-User-Id": targetUserId  // Different from the logged-in user
};

// Admin loads another user's preferences
fetch("http://localhost:8090/api/notification-preferences", {
  method: "GET",
  headers: headers
})
.then(r => r.json())
.then(data => console.log("User preferences:", data));
```

---

## Best Practices

1. **Store sessionId securely**
   - Use sessionStorage (cleared on browser close) rather than localStorage
   - In production apps, consider HTTP-only cookies

2. **Always include both headers**
   - Authorization header for authentication
   - X-User-Id header to specify the target user

3. **Handle authentication errors**
   - If you receive 401 UNAUTHORIZED, the session has expired - re-login
   - If you receive 403 FORBIDDEN, you don't have permission to access that resource

4. **Validate on the client side**
   - Check that both headers are present before making requests
   - Provide clear error messages to users

5. **Monitor failed requests**
   - Log authentication failures
   - Alert users if their session expires


### Endpoint 1: Get All Preferences

**Endpoint:** `GET /api/notification-preferences`

**Authentication:** Required (Bearer token in Authorization header)

**Description:** Retrieves all notification preferences for the authenticated user. If no preferences exist, they are automatically created with default values (all enabled).

```bash
curl -X GET http://localhost:8090/api/notification-preferences \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "preferences": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationApproved",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440002",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationDeclined",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440003",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCompleted",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440004",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationMissed",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440005",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCancelled",
      "emailEnabled": true,
      "smsEnabled": true,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    }
  ]
}
```

**Error Responses:**

Unauthorized (missing or invalid token):
```json
{
  "type": "UNAUTHORIZED",
  "message": "Invalid or missing Authorization header"
}
```

### Endpoint 2: Update Preference by Notification Type

**Endpoint:** `PUT /api/notification-preferences/{notificationType}`

**Authentication:** Required (Bearer token in Authorization header)

**Path Parameters:**
- `notificationType`: One of: `ConsultationApproved`, `ConsultationDeclined`, `ConsultationCompleted`, `ConsultationMissed`, `ConsultationCancelled`

**Request Body:**
```json
{
  "emailEnabled": false,
  "smsEnabled": true
}
```

**Description:** Updates notification preferences for a specific notification type. Only the authenticated user can update their own preferences.

```bash
curl -X PUT http://localhost:8090/api/notification-preferences/ConsultationApproved \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "emailEnabled": false,
    "smsEnabled": true
  }'
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "notificationType": "ConsultationApproved",
  "emailEnabled": false,
  "smsEnabled": true,
  "createdAt": "2026-02-01T10:30:00Z",
  "updatedAt": "2026-02-04T15:45:00Z"
}
```

**Error Responses:**

Unauthorized (missing or invalid token):
```json
{
  "type": "UNAUTHORIZED",
  "message": "Invalid or missing Authorization header"
}
```

Preference not found:
```json
{
  "type": "NOT_FOUND",
  "message": "Preference not found"
}
```

Invalid notification type:
```json
{
  "type": "VALIDATION_ERROR",
  "message": "Invalid notification type"
}
```

### Notification Types

The system supports the following notification types:

| Type | Triggered When | Default |
|------|--------|---------|
| `ConsultationApproved` | Specialist approves a consultation request | Email & SMS enabled |
| `ConsultationDeclined` | Specialist declines a consultation request | Email & SMS enabled |
| `ConsultationCompleted` | Consultation is marked as completed | Email & SMS enabled |
| `ConsultationMissed` | Consultation is marked as missed | Email & SMS enabled |
| `ConsultationCancelled` | Consultation is cancelled | Email & SMS enabled |

---

## Security Considerations

**Important:** 
1. **Authentication Required**: All notification preference endpoints require a valid JWT token
2. **User Isolation**: Users can only access their own preferences. Attempting to modify another user's preferences will fail
3. **Bearer Token Format**: Tokens must be sent as `Authorization: Bearer <token>`
4. **Token Validation**: The server validates:
   - Token format is valid
   - Token is not expired
   - User ID in token matches the requested resource

**Never:**
- Pass user IDs in query parameters
- Use hardcoded user IDs in client code
- Store tokens in localStorage (use secure HTTP-only cookies when possible)
- Log authentication tokens

---

## Integration Examples

### Python/Requests
```python
import requests

headers = {
    "Authorization": "Bearer 550e8400-e29b-41d4-a716-446655440000"
}

# Get preferences
response = requests.get(
    "http://localhost:8090/api/notification-preferences",
    headers=headers
)
print(response.json())

# Update preference
response = requests.put(
    "http://localhost:8090/api/notification-preferences/ConsultationApproved",
    headers=headers,
    json={"emailEnabled": False, "smsEnabled": True}
)
print(response.json())
```

### JavaScript/Fetch
```javascript
const token = "550e8400-e29b-41d4-a716-446655440000";

// Get preferences
fetch("http://localhost:8090/api/notification-preferences", {
  method: "GET",
  headers: {
    "Authorization": `Bearer ${token}`
  }
})
.then(r => r.json())
.then(data => console.log(data));

// Update preference
fetch("http://localhost:8090/api/notification-preferences/ConsultationApproved", {
  method: "PUT",
  headers: {
    "Authorization": `Bearer ${token}`,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    emailEnabled: false,
    smsEnabled: true
  })
})
.then(r => r.json())
.then(data => console.log(data));
```
