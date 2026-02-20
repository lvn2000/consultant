# User Notification Preferences - API Reference

## Overview
These endpoints allow authenticated users to manage their notification preferences. Users can enable or disable notifications by channel (email, SMS) for each notification type.

**Security Requirements:**
- All endpoints require authentication headers
- `X-Auth-User-Id` header: UUID of the authenticated user
- `X-User-Role` header: Role of the authenticated user (Client, Specialist, Admin)
- Users can access their own preferences
- Administrators can access any user's preferences (role-based, enforced server-side)

### Endpoint 1: Get Current User's Preferences

**Endpoint:** `GET /api/notification-preferences`

**Authentication Requirements:**
- `X-Auth-User-Id` header: UUID of the authenticated user
- `X-User-Role` header: Role of the authenticated user

**Description:** Retrieves all notification preferences for the current authenticated user. If no preferences exist, they are automatically created with default values (email notifications enabled, SMS disabled).

```bash
curl -X GET http://localhost:8090/api/notification-preferences \
  -H "X-Auth-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-User-Role: Client"
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
      "smsEnabled": false,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440002",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationDeclined",
      "emailEnabled": true,
      "smsEnabled": false,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440003",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCompleted",
      "emailEnabled": true,
      "smsEnabled": false,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440004",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationMissed",
      "emailEnabled": true,
      "smsEnabled": false,
      "createdAt": "2026-02-01T10:30:00Z",
      "updatedAt": "2026-02-01T10:30:00Z"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440005",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "notificationType": "ConsultationCancelled",
      "emailEnabled": true,
      "smsEnabled": false,
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

**Endpoint:** `PUT /api/notification-preferences/{userId}/{notificationType}`

**Authentication Requirements:**
- `X-Auth-User-Id` header: UUID of the authenticated user
- `X-User-Role` header: Role of the authenticated user

**Path Parameters:**
- `userId`: UUID of the user whose preference to update
- `notificationType`: One of: `ConsultationApproved`, `ConsultationDeclined`, `ConsultationCompleted`, `ConsultationMissed`, `ConsultationCancelled`

**Request Body:**
```json
{
  "emailEnabled": false,
  "smsEnabled": false
}
```

**Description:** Updates notification preferences for a specific notification type. Users can update their own preferences, and administrators can update any user's preferences.

```bash
curl -X PUT http://localhost:8090/api/notification-preferences/550e8400-e29b-41d4-a716-446655440000/ConsultationApproved \
  -H "X-Auth-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-User-Role: Client" \
  -H "Content-Type: application/json" \
  -d '{
    "emailEnabled": false,
    "smsEnabled": false
  }'
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "notificationType": "ConsultationApproved",
  "emailEnabled": false,
  "smsEnabled": false,
  "createdAt": "2026-02-01T10:30:00Z",
  "updatedAt": "2026-02-04T15:45:00Z"
}
```

**Error Responses:**

Unauthorized (missing authentication headers):
```json
{
  "type": "UNAUTHORIZED",
  "message": "Missing authentication headers"
}
```

Forbidden (not authorized to modify these preferences):
```json
{
  "type": "FORBIDDEN",
  "message": "Not authorized to modify these preferences"
}
```

Preference not found:
```json
{
  "type": "NOT_FOUND",
  "message": "Preference not found"
}
```

Invalid notification type or user ID:
```json
{
  "type": "VALIDATION_ERROR",
  "message": "Invalid notification type or user ID"
}
```

### Notification Types

The system supports the following notification types:

| Type | Triggered When | Default |
|------|--------|---------|
| `ConsultationApproved` | Specialist approves a consultation request | Email enabled, SMS disabled |
| `ConsultationDeclined` | Specialist declines a consultation request | Email enabled, SMS disabled |
| `ConsultationCompleted` | Consultation is marked as completed | Email enabled, SMS disabled |
| `ConsultationMissed` | Consultation is marked as missed | Email enabled, SMS disabled |
| `ConsultationCancelled` | Consultation is cancelled | Email enabled, SMS disabled |

---

## Security Implementation

### Authentication Flow
1. **User logs in** → Server sets authentication headers
2. **All requests to notification endpoints** include:
   - `X-Auth-User-Id: <userId>` - UUID of the authenticated user
   - `X-User-Role: <role>` - Role of the authenticated user (Client, Specialist, Admin)

### Authorization Rules
- **Users**: Can access and modify their own preferences only
- **Admins**: Can access and modify preferences for any user
- **Server-side validation**: Enforces role-based access control (admins vs. regular users)

### Important Security Notes
1. **Never share your authentication headers** - They validate your identity
2. **Both headers are required** - X-Auth-User-Id and X-User-Role must be supplied
3. **Session validation happens server-side** - The server verifies the session before allowing access
4. **Role checking happens server-side** - Only authorized users can access other users' preferences
5. **HTTPS required in production** - Protect authentication headers in transit

---

## Integration Examples

### Python/Requests (User accessing their own preferences)
```python
import requests

user_id = "550e8400-e29b-41d4-a716-446655440000"
user_role = "Client"

headers = {
    "X-Auth-User-Id": user_id,
    "X-User-Role": user_role
}

# Get preferences
response = requests.get(
    "http://localhost:8090/api/notification-preferences",
    headers=headers
)
print(response.json())

# Update preference
response = requests.put(
    f"http://localhost:8090/api/notification-preferences/{user_id}/ConsultationApproved",
    headers=headers,
    json={"emailEnabled": False, "smsEnabled": False}
)
print(response.json())
```

### JavaScript/Fetch (User accessing their own preferences)
```javascript
const userId = "550e8400-e29b-41d4-a716-446655440000";
const userRole = "Client";

const headers = {
  "X-Auth-User-Id": userId,
  "X-User-Role": userRole
};

// Get preferences
fetch("http://localhost:8090/api/notification-preferences", {
  method: "GET",
  headers: headers
})
.then(r => r.json())
.then(data => console.log(data));

// Update preference
fetch(`http://localhost:8090/api/notification-preferences/${userId}/ConsultationApproved`, {
  method: "PUT",
  headers: {
    ...headers,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    emailEnabled: false,
    smsEnabled: false
  })
})
.then(r => r.json())
.then(data => console.log(data));
```

### JavaScript/Fetch (Admin accessing other user's preferences)
```javascript
const adminUserId = "admin-uuid";
const adminRole = "Admin";
const targetUserId = "550e8400-e29b-41d4-a716-446655440001"; // Another user's ID

const headers = {
  "X-Auth-User-Id": adminUserId,
  "X-User-Role": adminRole
};

// Admin loads another user's preferences
fetch(`http://localhost:8090/api/notification-preferences/${targetUserId}`, {
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
