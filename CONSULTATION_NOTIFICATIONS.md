# Consultation Status Change Notifications

## Overview

This feature implements email notifications that are automatically sent when a consultation's status changes. Both users (clients) and specialists receive appropriate notifications based on the status transition.

## Features

### Supported Status Transitions

#### 1. **Requested → Scheduled** (Specialist Approves)
- **User receives:** Consultation Approved notification
  - Includes consultation date, time, and duration estimate
  - Reminds user to be available at scheduled time
  
- **Specialist receives:** New Consultation Scheduled notification
  - Confirms the approved consultation
  - Shows user name, date, time, and duration

#### 2. **Requested → Cancelled** (Specialist Declines)
- **User receives:** Consultation Request Declined notification
  - Informs that the specialist declined the request
  - Suggests requesting from another specialist

#### 3. **Scheduled → Completed**
- **User receives:** Consultation Completed notification
  - Informs consultation is complete
  - Encourages rating and review
  
- **Specialist receives:** Consultation Completed notification
  - Confirms the completed status

#### 4. **Scheduled → Missed**
- **User receives:** Consultation Marked as Missed notification
  - Informs of missed appointment
  - Suggests rescheduling options
  
- **Specialist receives:** Consultation Marked as Missed notification
  - Confirms the missed status

#### 5. **Scheduled → Cancelled**
- **User receives:** Consultation Cancelled notification
  - Informs of cancellation
  - Invites to reschedule if needed
  
- **Specialist receives:** Consultation Cancelled notification
  - Confirms the cancellation

#### 6. **InProgress → Completed**
- **User receives:** Consultation Completed notification

## Implementation Details

### Service Layer Changes

**File:** `core/src/main/scala/com/consultant/core/service/ConsultationService.scala`

#### Modified Methods

1. **updateConsultationStatus()**
   - Now fetches user and specialist data
   - Calls `sendStatusChangeNotifications()` helper
   - Triggers email sends based on status transition

2. **approveConsultation()**
   - Now sends notifications when consultation is approved
   - Notifies both user and specialist

#### New Private Method

**sendStatusChangeNotifications()**
- Determines which status transition occurred
- Generates appropriate email subjects and bodies
- Sends emails to relevant parties (user, specialist, or both)
- Handles cases where user or specialist data is unavailable

### Email Notification Templates

All notifications include:
- Personalized greeting with user/specialist name
- Relevant consultation details (date, time, duration, category)
- Contextual information based on status change
- Professional footer ("Best regards, Consultant Team")

### Infrastructure Integration

The implementation uses the existing `NotificationService` interface:
- **sendEmail(to: String, subject: String, body: String): IO[Unit]**

This abstraction allows for multiple implementations:
- **AwsNotificationService:** Uses AWS SES for production
- **MockNotificationService:** Prints to console for development

## Usage Examples

### Current Behavior

When a specialist approves a consultation request:
```scala
consultationService.approveConsultation(consultationId, duration = 60)
```

**Automatic actions:**
1. Consultation status updated to "Scheduled"
2. Duration set to 60 minutes
3. Email sent to client saying: "Your consultation request with [Specialist Name] has been approved and scheduled!"
4. Email sent to specialist confirming the approval

### Status Update Example

When marking a consultation as completed:
```scala
consultationService.updateConsultationStatus(consultationId, ConsultationStatus.Completed)
```

**Automatic actions:**
1. Consultation status updated to "Completed"
2. Email sent to client: "Your consultation with [Specialist Name] has been completed"
3. Email sent to specialist confirming completion

## Configuration

### Email Notification Service

**Development:**
```scala
notificationService = MockNotificationService()
```
- Prints notifications to console
- No actual emails sent
- Useful for testing and development

**Production (AWS):**
```scala
notificationService = AwsNotificationService(
  snsClient = ...,
  sesClient = ...,
  senderEmail = "noreply@consultant.example.com"
)
```
- Uses AWS SES to send actual emails
- Requires AWS credentials and configuration

## Error Handling

The implementation gracefully handles missing data:
- If user or specialist cannot be fetched, no email is sent
- Status update continues normally regardless of notification delivery
- Failed emails don't block consultation status changes

## Future Enhancements

1. **SMS Notifications:** Extend to use SMS for urgent notifications
2. **Notification Preferences:** Allow users to choose notification channels
3. **Email Templates:** Move to HTML-based templates with styling
4. **Scheduling:** Schedule emails to send at specific times
5. **Notification History:** Log all sent notifications for audit trail
6. **Retry Logic:** Implement retry mechanism for failed email sends
7. **In-app Notifications:** Add notifications to user dashboard

## Testing

### Manual Testing

1. Create a consultation request via client app
2. Approve consultation as specialist
3. Check console output (development) or email inbox (production)

### Unit Tests

Located in: `core/src/test/scala/com/consultant/core/service/ConsultationServiceSpec.scala`

Test the notification flow by mocking the NotificationService and verifying:
- Correct emails are sent for each status transition
- Both user and specialist receive appropriate notifications
- No emails for status transitions without notifications

### Sample Test

```scala
"approveConsultation should send notifications to both user and specialist" in {
  // Mock consultation, user, specialist
  // Mock notificationService
  
  val result = service.approveConsultation(consultationId, 60).unsafeRunSync()
  
  // Verify two emails were sent (one to user, one to specialist)
  // Verify email subjects and content
}
```

## Logging and Monitoring

For production deployments, consider:
1. Adding structured logging for all sent notifications
2. Monitoring email delivery failures
3. Setting up alerts for failed notification sends
4. Tracking notification delivery metrics

## See Also

- [Consultation Workflow](SPECIALIST_APPROVAL_WORKFLOW.md)
- [Infrastructure Ports](core/src/main/scala/com/consultant/core/ports/InfrastructurePorts.scala)
- [AWS Notification Service](infrastructure/src/main/scala/com/consultant/infrastructure/aws/AwsNotificationService.scala)
- [Mock Notification Service](infrastructure/src/main/scala/com/consultant/infrastructure/local/MockNotificationService.scala)
