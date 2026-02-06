# Consultation Notifications - Testing Guide

## Quick Start

The consultation status change notification feature is now live. Follow these steps to test it.

## Testing in Development

### Setup

1. **Start the backend server** with mock notifications:
```bash
cd /home/lvn/prg/scala/Consultant/backend
sbt run
```

The mock notification service will print emails to the console instead of sending real emails.

### Test Scenario 1: Create Consultation (User Initiated)

**Client App:**
1. Login as a client
2. Go to "Book Consultation" tab
3. Fill in the form:
   - Select a specialist
   - Select a category
   - Enter description
   - Select scheduled date and time
4. Click "Book Consultation"

**Expected Output (Console):**
```
[MOCK EMAIL] To: user@example.com, 
Subject: Consultation Request Created, 
Body: Your consultation with [Specialist Name] has been requested.
```

### Test Scenario 2: Specialist Approves Consultation

**Specialist App:**
1. Login as a specialist
2. Go to "My Consultations"
3. Find a consultation with "Requested" status
4. Click "✓ Approve" button
5. Enter duration (e.g., 60 minutes)
6. Click "Confirm"

**Expected Console Output (Two Emails):**

**Email 1 - To Client:**
```
[MOCK EMAIL] To: client@example.com, 
Subject: Consultation Approved, 
Body: 
Hello [Client Name],

Your consultation request with [Specialist Name] has been approved and scheduled!

Date & Time: [date and time]
Duration: 60 minutes

Please make sure you are available at the scheduled time.

Best regards,
Consultant Team
```

**Email 2 - To Specialist:**
```
[MOCK EMAIL] To: specialist@example.com, 
Subject: New Consultation Scheduled, 
Body: 
Hello [Specialist Name],

You have approved a consultation with [Client Name].

Date & Time: [date and time]
Duration: 60 minutes
Category: [categoryId]

Best regards,
Consultant Team
```

### Test Scenario 3: Specialist Declines Consultation

**Specialist App:**
1. Find a consultation with "Requested" status
2. Click "✗ Decline" button

**Expected Console Output:**
```
[MOCK EMAIL] To: client@example.com, 
Subject: Consultation Request Declined, 
Body: 
Hello [Client Name],

Unfortunately, [Specialist Name] has declined your consultation request.

You may want to request a consultation with another specialist.

Best regards,
Consultant Team
```

### Test Scenario 4: Mark Consultation as Completed

**Either App:**
1. Find a consultation with "Scheduled" status
2. Use the API to update status to "Completed"

```bash
curl -X PUT http://localhost:8090/api/consultations/{consultationId}/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: {userId}" \
  -H "X-User-Role: {userRole}" \
  -d '{"status": "Completed"}'
```

**Expected Console Output (Two Emails):**
- Email to client: "Consultation Completed"
- Email to specialist: "Consultation Completed"

### Test Scenario 5: Mark Consultation as Missed

```bash
curl -X PUT http://localhost:8090/api/consultations/{consultationId}/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: {userId}" \
  -H "X-User-Role: {userRole}" \
  -d '{"status": "Missed"}'
```

**Expected Console Output (Two Emails):**
- Email to client: "Consultation Marked as Missed"
- Email to specialist: "Consultation Marked as Missed"

### Test Scenario 6: Cancel Scheduled Consultation

```bash
curl -X PUT http://localhost:8090/api/consultations/{consultationId}/status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: {userId}" \
  -H "X-User-Role: {userRole}" \
  -d '{"status": "Cancelled"}'
```

**Expected Console Output (Two Emails):**
- Email to client: "Consultation Cancelled"
- Email to specialist: "Consultation Cancelled"

## Testing in Production

### Configure AWS SES

1. Set up AWS credentials in your environment
2. Update `api/Server.scala` to use `AwsNotificationService` instead of `MockNotificationService`
3. Set the sender email in your configuration:
```
export AWS_SENDER_EMAIL="noreply@consultant.example.com"
```

### Verify Emails

1. Check the specified email accounts
2. Verify correct recipient (user or specialist)
3. Verify correct subject line
4. Verify email content includes all relevant consultation details

## Troubleshooting

### No Emails in Console

1. Check that the backend is running with verbose logging
2. Verify you're using the MockNotificationService in development
3. Check that the consultation and user/specialist records exist in the database

### Missing User/Specialist

The service gracefully handles missing data:
- If user cannot be found, email to user is skipped
- If specialist cannot be found, email to specialist is skipped
- Status update continues regardless of notification delivery

### Emails Sent but Content is Incomplete

This could happen if:
- Consultation has missing fields (e.g., no scheduledAt)
- User or specialist name is empty
- Duration is not set

The service still sends emails but with "TBD" or empty placeholders.

## Email Notification Flow

```
User Action (Create/Update/Approve)
    ↓
ConsultationService method called
    ↓
Fetch Consultation, User, Specialist from Database
    ↓
Determine status transition (old → new)
    ↓
Match against supported transitions
    ↓
Generate appropriate email(s)
    ↓
Send via NotificationService (Mock or AWS)
    ↓
Return result (with or without notification delivery status)
```

## Database Requirements

Ensure these tables exist with correct data:
- `users` - email addresses for all clients
- `specialists` - email addresses for all specialists
- `consultations` - correct user_id and specialist_id foreign keys

## Monitoring

In production, monitor:
1. Email delivery failures in AWS SES dashboards
2. Bounced or rejected emails
3. Email open rates and click-through rates
4. Complaint rates

## Next Steps

After successful testing:
1. Deploy to staging environment
2. Test with real AWS SES integration
3. Monitor email delivery for 24-48 hours
4. Deploy to production
5. Monitor ongoing email delivery metrics

## Support

For issues or questions:
- Check the complete implementation: [CONSULTATION_NOTIFICATIONS.md](CONSULTATION_NOTIFICATIONS.md)
- Review the source code: `core/src/main/scala/com/consultant/core/service/ConsultationService.scala`
