# Consultation Notifications - API Reference

## Endpoints that Trigger Notifications

### 1. Create Consultation
**Endpoint:** `POST /api/consultations`
**Notifications Sent:** 1 (to client)
**Status Transition:** N/A (new consultation)

```bash
curl -X POST http://localhost:8090/api/consultations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "specialistId": "550e8400-e29b-41d4-a716-446655440001",
    "categoryId": "550e8400-e29b-41d4-a716-446655440002",
    "description": "Need help with career planning",
    "scheduledAt": "2026-02-15T14:00:00Z",
    "duration": 60
  }'
```

**Email Sent to:** Client
**Subject:** Consultation Request Created

---

### 2. Approve Consultation (Specialist)
**Endpoint:** `PUT /api/consultations/{consultationId}/approve`
**Notifications Sent:** 2 (to client and specialist)
**Status Transition:** Requested → Scheduled

```bash
curl -X PUT http://localhost:8090/api/consultations/550e8400-e29b-41d4-a716-446655440003/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440001" \
  -H "X-User-Role: Specialist" \
  -d '{
    "status": "Scheduled",
    "duration": 60
  }'
```

**Emails Sent to:**
- Client: "Consultation Approved"
- Specialist: "New Consultation Scheduled"

---

### 3. Update Consultation Status
**Endpoint:** `PUT /api/consultations/{consultationId}/status`
**Notifications Sent:** Depends on transition (see below)
**Query Headers Required:**
- `X-User-Id`: UUID of the user making the request
- `X-User-Role`: Role of the user (Client, Specialist, or Admin)

#### Example: Mark as Completed

```bash
curl -X PUT http://localhost:8090/api/consultations/550e8400-e29b-41d4-a716-446655440003/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-User-Role: Client" \
  -d '{
    "status": "Completed"
  }'
```

**Emails Sent to:** Client and Specialist

#### Example: Mark as Missed

```bash
curl -X PUT http://localhost:8090/api/consultations/550e8400-e29b-41d4-a716-446655440003/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440001" \
  -H "X-User-Role: Specialist" \
  -d '{
    "status": "Missed"
  }'
```

**Emails Sent to:** Client and Specialist

#### Example: Cancel Scheduled Consultation

```bash
curl -X PUT http://localhost:8090/api/consultations/550e8400-e29b-41d4-a716-446655440003/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440001" \
  -H "X-User-Role: Specialist" \
  -d '{
    "status": "Cancelled"
  }'
```

**Emails Sent to:** Client and Specialist

---

## Status Transitions and Notification Details

| Transition | From | To | Emails Sent | Subject Lines |
|-----------|------|----|----|----------|
| Approval | Requested | Scheduled | 2 | "Consultation Approved" (Client)<br/>"New Consultation Scheduled" (Specialist) |
| Decline | Requested | Cancelled | 1 | "Consultation Request Declined" (Client) |
| Complete | Scheduled | Completed | 2 | "Consultation Completed" (both) |
| Missed | Scheduled | Missed | 2 | "Consultation Marked as Missed" (both) |
| Cancel (Scheduled) | Scheduled | Cancelled | 2 | "Consultation Cancelled" (both) |
| Complete (In Progress) | InProgress | Completed | 1 | "Consultation Completed" (Client) |

---

## Response Format

All endpoints return the updated consultation DTO:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "specialistId": "550e8400-e29b-41d4-a716-446655440001",
  "categoryId": "550e8400-e29b-41d4-a716-446655440002",
  "description": "Need help with career planning",
  "status": "Scheduled",
  "scheduledAt": "2026-02-15T14:00:00Z",
  "duration": 60,
  "price": "150.00",
  "rating": null,
  "review": null,
  "createdAt": "2026-02-01T10:30:00Z",
  "updatedAt": "2026-02-04T23:11:00Z"
}
```

---

## Error Handling

### Consultation Not Found
```json
{
  "status": "ERROR",
  "message": "Consultation not found",
  "code": "CONSULTATION_NOT_FOUND"
}
```

### Missing Authentication Headers
```json
{
  "status": "UNAUTHORIZED",
  "message": "Missing authentication headers",
  "code": "UNAUTHORIZED"
}
```

### Insufficient Permissions
```json
{
  "status": "FORBIDDEN",
  "message": "You don't have permission to update this consultation",
  "code": "FORBIDDEN"
}
```

### Invalid Status Value
```json
{
  "status": "VALIDATION_ERROR",
  "message": "Invalid status",
  "code": "VALIDATION_ERROR"
}
```

**Note:** Even if an error occurs during email sending, the status update still completes successfully. Notifications are sent on a best-effort basis.

---

## Email Notification Examples

### Approval Notification (Client)

```
To: client@example.com
Subject: Consultation Approved

Hello John Doe,

Your consultation request with Jane Smith has been approved and scheduled!

Date & Time: 2026-02-15T14:00:00Z
Duration: 60 minutes

Please make sure you are available at the scheduled time.

Best regards,
Consultant Team
```

### Completion Notification (Specialist)

```
To: specialist@example.com
Subject: Consultation Completed

Hello Jane Smith,

Your consultation with John Doe has been completed.

Best regards,
Consultant Team
```

### Cancellation Notification (Both)

```
To: client@example.com
Subject: Consultation Cancelled

Hello John Doe,

Your consultation with Jane Smith scheduled for 2026-02-15T14:00:00Z has been cancelled.

If you need further assistance, please feel free to request another consultation.

Best regards,
Consultant Team
```

---

## Testing Endpoints

### Get Consultation by ID
```bash
curl -X GET http://localhost:8090/api/consultations/550e8400-e29b-41d4-a716-446655440003
```

### List User's Consultations
```bash
curl -X GET "http://localhost:8090/api/consultations/user/550e8400-e29b-41d4-a716-446655440000?offset=0&limit=20"
```

### List Specialist's Consultations
```bash
curl -X GET "http://localhost:8090/api/consultations/specialist/550e8400-e29b-41d4-a716-446655440001?offset=0&limit=20"
```

---

## Rate Limiting and Best Practices

1. **Don't spam status updates**: Each update triggers notifications
2. **Combine operations**: Use approve endpoint instead of update + status change
3. **Handle failures gracefully**: Email failures are logged but won't break your flow
4. **Monitor email delivery**: Check AWS SES metrics in production

---

## Troubleshooting

### No Email Received
1. Check that user/specialist records exist in database
2. Verify email addresses in user records
3. In development, check console for [MOCK EMAIL] output
4. In production, check AWS SES bounce/complaint rates

### Wrong Recipient
1. Verify X-User-Id and X-User-Role headers are correct
2. Check that authorization is working properly
3. Ensure database relationships are correct

### Missing Email Content
1. Verify consultation has all required fields
2. Check that user/specialist names are populated
3. Verify scheduled date is in correct format

---

## Integration Examples

### Python/Requests
```python
import requests

data = {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "specialistId": "550e8400-e29b-41d4-a716-446655440001",
    "categoryId": "550e8400-e29b-41d4-a716-446655440002",
    "description": "Need help",
    "scheduledAt": "2026-02-15T14:00:00Z",
    "duration": 60
}

response = requests.post(
    "http://localhost:8090/api/consultations",
    json=data
)
print(response.json())
```

### JavaScript/Fetch
```javascript
const data = {
  userId: "550e8400-e29b-41d4-a716-446655440000",
  specialistId: "550e8400-e29b-41d4-a716-446655440001",
  categoryId: "550e8400-e29b-41d4-a716-446655440002",
  description: "Need help",
  scheduledAt: "2026-02-15T14:00:00Z",
  duration: 60
};

fetch("http://localhost:8090/api/consultations", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(data)
})
.then(r => r.json())
.then(data => console.log(data));
```

---

## See Also

- [Complete Feature Documentation](CONSULTATION_NOTIFICATIONS.md)
- [Testing Guide](NOTIFICATIONS_TESTING.md)
- [Implementation Summary](NOTIFICATIONS_IMPLEMENTATION_SUMMARY.md)
