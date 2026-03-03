# Notification System

This document describes the notification system in the Consultant Backend application.

## Overview

The notification system provides comprehensive messaging capabilities to keep users informed about important events and updates within the application.

## Notification Types

### Consultation Notifications

The system handles various consultation-related notifications:

- **Consultation Requested**: Sent when a client requests a consultation
- **Consultation Approved**: Sent when a specialist approves a consultation
- **Consultation Scheduled**: Sent when a consultation is scheduled
- **Consultation Reminder**: Sent before scheduled consultations
- **Consultation Completed**: Sent after consultation completion
- **Consultation Cancelled**: Sent when a consultation is cancelled
- **Consultation Missed**: Sent when a consultation is marked as missed

### Account Notifications

- **Registration Confirmation**: Sent upon successful registration
- **Password Reset**: Sent when password reset is requested
- **Account Updates**: Sent for profile or preference changes
- **Account Deactivation**: Sent when account is deactivated

### System Notifications

- **Availability Changes**: Sent when specialist availability changes
- **New Reviews**: Sent when specialists receive new reviews
- **Payment Notifications**: Sent for payment-related events
- **System Announcements**: Sent for important system updates

## Notification Channels

### Email Notifications

Email is the primary notification channel:

```scala
// Email configuration
AWS_SENDER_EMAIL=noreply@consultant.com
```

- **Templates**: HTML and plain text templates
- **Delivery**: Via AWS SES or SMTP
- **Tracking**: Delivery status and open rates
- **Preferences**: User-controlled email frequency

### SMS Notifications

SMS for time-sensitive notifications:

```scala
// SMS configuration
AWS_SNS_TOPIC_PREFIX=consultant-notifications
```

- **Provider**: AWS SNS for SMS delivery
- **Content**: Short, urgent messages
- **Opt-in**: User must opt-in to SMS notifications
- **Cost Management**: SMS cost tracking

### Push Notifications

For mobile applications:

- **Platform**: Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNs)
- **Real-time**: Instant delivery for urgent notifications
- **Rich Content**: Support for images and interactive elements
- **Segmentation**: Targeted notifications based on user preferences

## Notification Preferences

### User Preference Management

Users can control their notification preferences:

- **Channel Selection**: Choose which channels to receive notifications
- **Frequency Control**: Set notification frequency (immediate, daily digest, weekly)
- **Type Selection**: Choose which notification types to receive
- **Time Windows**: Set preferred notification times

### API Endpoints

#### Get Notification Preferences
```
GET /api/notification-preferences/{userId}/{notificationType}
```

#### Update Notification Preferences
```
PUT /api/notification-preferences/{userId}/{notificationType}
{
  "enabled": true,
  "channels": ["email", "sms"],
  "frequency": "immediate"
}
```

## Implementation Architecture

### Domain Model

```scala
// Notification domain entities
case class Notification(
  id: UUID,
  userId: UUID,
  notificationType: NotificationType,
  subject: String,
  body: String,
  channels: List[NotificationChannel],
  sentAt: Option[Instant],
  deliveredAt: Option[Instant]
)

enum NotificationType:
  case ConsultationRequested, ConsultationApproved, ConsultationCancelled, etc.

enum NotificationChannel:
  case Email, Sms, Push, InApp
```

### Service Layer

The notification service handles the business logic:

```scala
class NotificationService(
  emailService: EmailService,
  smsService: SMSService,
  pushService: PushService
) {
  def sendNotification(notification: Notification): IO[Unit]
  def sendEmail(recipient: String, subject: String, body: String): IO[Unit]
  def sendSms(recipient: String, message: String): IO[Unit]
}
```

### Repository Layer

```scala
trait NotificationRepository {
  def save(notification: Notification): IO[Unit]
  def findByUser(userId: UUID): IO[List[Notification]]
  def updateStatus(id: UUID, status: NotificationStatus): IO[Unit]
}
```

## Email Templates

### Template Structure

Email templates are stored as HTML with dynamic content:

```html
<!-- consultation-approved.html -->
<html>
<head>
  <title>Consultation Approved</title>
</head>
<body>
  <h1>Your consultation has been approved!</h1>
  <p>Dear {{userName}},</p>
  <p>Your consultation with {{specialistName}} has been approved.</p>
  <p>Scheduled for: {{scheduledDate}}</p>
  <p>Duration: {{duration}} minutes</p>
</body>
</html>
```

### Template Variables

- **User Information**: userName, userEmail
- **Consultation Details**: specialistName, scheduledDate, duration
- **System Information**: systemUrl, companyInfo

## Delivery Mechanisms

### Email Delivery

Uses AWS SES for reliable email delivery:

```scala
class AWSEmailService extends EmailService {
  def sendEmail(to: String, subject: String, htmlBody: String): IO[Unit] = {
    // Send email via AWS SES
  }
}
```

### SMS Delivery

Uses AWS SNS for SMS delivery:

```scala
class AWSSMSService extends SMSService {
  def sendSMS(to: String, message: String): IO[Unit] = {
    // Send SMS via AWS SNS
  }
}
```

### Asynchronous Processing

Notifications are processed asynchronously:

```scala
// Use queues for reliable delivery
val notificationQueue: Queue[IO, Notification] = ???

// Process notifications in background
def processNotifications: Stream[IO, Unit] = 
  notificationQueue.dequeue.evalMap(sendNotification)
```

## Configuration

### Environment Variables

```bash
# Email configuration
NOTIFICATION_EMAIL_ENABLED=true
AWS_SES_REGION=us-east-1
AWS_SES_SOURCE_ARN=arn:aws:ses:us-east-1:account:identity/domain.com

# SMS configuration
NOTIFICATION_SMS_ENABLED=true
AWS_SNS_REGION=us-east-1

# Push notification configuration
NOTIFICATION_PUSH_ENABLED=true
FCM_SERVER_KEY=your-fcm-server-key
```

### Rate Limiting

```scala
# Notification rate limits
NOTIFICATION_RATE_LIMIT_PER_HOUR=100
NOTIFICATION_BURST_SIZE=10
```

## Error Handling

### Delivery Failures

- **Retry Logic**: Automatic retries for transient failures
- **Fallback Channels**: Fallback to alternative channels
- **Error Logging**: Comprehensive error logging
- **Alerting**: Alerts for persistent failures

### Queue Management

- **Dead Letter Queue**: Failed notifications go to DLQ
- **Retry Policies**: Configurable retry policies
- **Monitoring**: Queue depth and processing metrics

## Security Considerations

### Content Security

- **Template Sanitization**: Sanitize user-generated content in templates
- **XSS Prevention**: Prevent cross-site scripting in notifications
- **URL Validation**: Validate URLs in notification content

### Privacy Protection

- **PII Encryption**: Encrypt personally identifiable information
- **Access Controls**: Restrict access to notification data
- **Audit Logging**: Log access to notification data

## Performance Optimization

### Bulk Operations

- **Batch Processing**: Process notifications in batches
- **Parallel Delivery**: Parallel delivery across channels
- **Connection Pooling**: Efficient connection management

### Caching

- **Template Caching**: Cache compiled email templates
- **Preference Caching**: Cache user preferences
- **Metadata Caching**: Cache recipient metadata

## Testing

### Unit Tests

```scala
class NotificationServiceSpec extends AnyFlatSpec with Matchers {
  "NotificationService" should "send email notifications" in {
    // Test email delivery
  }
  
  it should "respect user preferences" in {
    // Test preference filtering
  }
}
```

### Integration Tests

```scala
class NotificationIntegrationSpec extends AnyFlatSpec with Matchers {
  "Notification system" should "deliver notifications via all channels" in {
    // Test end-to-end delivery
  }
}
```

## Monitoring and Analytics

### Delivery Metrics

- **Sent Count**: Number of notifications sent
- **Delivery Rate**: Percentage of successfully delivered notifications
- **Open Rate**: Email open rates (if tracked)
- **Click Rate**: Link click rates

### Performance Metrics

- **Processing Time**: Time to process notifications
- **Queue Depth**: Notification queue size
- **Error Rate**: Notification processing error rate

### User Engagement

- **Preference Changes**: Track changes in user preferences
- **Unsubscribe Rate**: Track unsubscribe rates
- **Channel Effectiveness**: Compare effectiveness of different channels

## Compliance

### GDPR Compliance

- **Right to Erasure**: Delete notification history for users
- **Data Portability**: Export notification history
- **Consent Management**: Manage consent for different notification types

### CAN-SPAM Compliance

- **Unsubscribe Links**: Include unsubscribe links in emails
- **Valid Sender**: Use verified sender addresses
- **Accurate Headers**: Use accurate email headers

## Best Practices

### Content Guidelines

- **Clear Subject Lines**: Use descriptive subject lines
- **Concise Content**: Keep notifications brief and informative
- **Clear CTAs**: Include clear calls-to-action when appropriate
- **Mobile-Friendly**: Ensure emails render well on mobile devices

### Frequency Management

- **Avoid Spam**: Don't overwhelm users with notifications
- **Batch Similar**: Batch similar notifications when possible
- **Respect Time Zones**: Send notifications at appropriate local times
- **Honor Preferences**: Strictly honor user preferences

## Troubleshooting

### Common Issues

**Delivery Failures**:
```bash
# Check delivery logs
tail -f logs/notifications.log | grep ERROR

# Check queue status
docker exec -it notification-queue redis-cli llen notification_queue
```

**Template Issues**:
```bash
# Validate templates
sbt "testOnly *TemplateSpec"
```

**Configuration Problems**:
```bash
# Check configuration
docker logs notification-service | grep -i config
```

### Debugging

Enable detailed logging for notification debugging:

```bash
# Set log level to DEBUG
LOG_LEVEL=DEBUG
NOTIFICATION_LOG_SENSITIVE_DATA=false  # Don't log sensitive data
```

This notification system provides a robust, scalable foundation for keeping users informed about important events in the Consultant application while respecting their preferences and privacy.