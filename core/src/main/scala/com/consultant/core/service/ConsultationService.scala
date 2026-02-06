package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*

// Helper type to carry recipient information with notifications
private sealed trait NotificationRecipient:
  def email: String

private case class ClientRecipient(email: String, userId: java.util.UUID) extends NotificationRecipient
private case class SpecialistRecipient(email: String)                     extends NotificationRecipient

// Notification with explicit recipient type
private case class NotificationToSend(
  recipient: NotificationRecipient,
  subject: String,
  body: String,
  notificationType: NotificationType
)

class ConsultationService(
  consultationRepo: ConsultationRepository,
  specialistRepo: SpecialistRepository,
  userRepo: UserRepository,
  notificationService: NotificationService,
  notificationPreferenceRepo: NotificationPreferenceRepository
):

  def createConsultation(
    request: CreateConsultationRequest
  ): IO[Either[DomainError, Consultation]] =
    for
      userOpt       <- userRepo.findById(request.userId)
      specialistOpt <- specialistRepo.findById(request.specialistId)
      result <- (userOpt, specialistOpt) match
        case (Some(user), Some(specialist)) =>
          specialist.categoryRates.find(_.categoryId == request.categoryId) match
            case None =>
              IO.pure(
                Left(
                  DomainError.ValidationError(
                    s"Specialist is not available for category: ${request.categoryId}"
                  )
                )
              )
            case Some(categoryRate) if !specialist.isAvailable =>
              IO.pure(Left(DomainError.SpecialistNotAvailable(specialist.id)))
            case Some(categoryRate) =>
              val price = calculatePrice(categoryRate.hourlyRate, request.duration)
              for
                consultation <- consultationRepo.create(request, price)
                // Wrap email send with error handling: log failures but don't fail consultation creation
                _ <- notificationService
                  .sendEmail(
                    user.email,
                    "Consultation Request Created",
                    s"Your consultation with ${specialist.name} has been requested."
                  )
                  .attempt
                  .flatMap {
                    case Right(_) => IO.unit
                    case Left(error) =>
                      IO.println(
                        s"[WARNING] Failed to send consultation creation email to ${user.email}: ${error.getMessage}"
                      )
                  }
              yield Right(consultation)
        case (None, _) => IO.pure(Left(DomainError.UserNotFound(request.userId)))
        case (_, None) => IO.pure(Left(DomainError.SpecialistNotFound(request.specialistId)))
    yield result

  def getConsultation(id: ConsultationId): IO[Either[DomainError, Consultation]] =
    consultationRepo.findById(id).map {
      case Some(consultation) => Right(consultation)
      case None               => Left(DomainError.ConsultationNotFound(id))
    }

  def updateConsultationStatus(
    id: ConsultationId,
    status: ConsultationStatus
  ): IO[Either[DomainError, Unit]] =
    for
      consultationOpt <- consultationRepo.findById(id)
      result <- consultationOpt match
        case Some(consultation) =>
          for
            _ <- consultationRepo.updateStatus(id, status)
            // Only fetch user/specialist if this transition requires notifications
            _ <-
              if statusTransitionRequiresNotifications(consultation.status, status) then
                for
                  user       <- userRepo.findById(consultation.userId)
                  specialist <- specialistRepo.findById(consultation.specialistId)
                  _          <- sendStatusChangeNotifications(consultation, status, user, specialist)
                yield ()
              else IO.unit
          yield Right(())
        case None => IO.pure(Left(DomainError.ConsultationNotFound(id)))
    yield result

  def approveConsultation(
    id: ConsultationId,
    duration: Int
  ): IO[Either[DomainError, Unit]] =
    for
      consultationOpt <- consultationRepo.findById(id)
      result <- consultationOpt match
        case Some(consultation) =>
          val updated = consultation.copy(
            status = ConsultationStatus.Scheduled,
            duration = Some(duration)
          )
          for
            _          <- consultationRepo.update(updated)
            user       <- userRepo.findById(consultation.userId)
            specialist <- specialistRepo.findById(consultation.specialistId)
            _          <- sendStatusChangeNotifications(consultation, updated.status, user, specialist)
          yield Right(())
        case None => IO.pure(Left(DomainError.ConsultationNotFound(id)))
    yield result

  def addReview(
    id: ConsultationId,
    rating: Int,
    review: String
  ): IO[Either[DomainError, Unit]] =
    for
      consultationOpt <- consultationRepo.findById(id)
      result <- consultationOpt match
        case Some(consultation) =>
          for
            _          <- consultationRepo.addReview(id, rating, review)
            specialist <- specialistRepo.findById(consultation.specialistId)
            _ <- specialist match
              case Some(s) =>
                s.categoryRates.find(_.categoryId == consultation.categoryId) match
                  case Some(rate) =>
                    val newRating = calculateNewRating(rate.rating, rate.totalConsultations, rating)
                    specialistRepo.updateCategoryRating(
                      s.id,
                      consultation.categoryId,
                      newRating,
                      rate.totalConsultations + 1
                    )
                  case None => IO.unit
              case None => IO.unit
          yield Right(())
        case None => IO.pure(Left(DomainError.ConsultationNotFound(id)))
    yield result

  def getUserConsultations(userId: UserId, offset: Int, limit: Int): IO[List[Consultation]] =
    consultationRepo.findByUser(userId, offset, limit)

  def getSpecialistConsultations(
    specialistId: SpecialistId,
    offset: Int,
    limit: Int
  ): IO[List[Consultation]] =
    consultationRepo.findBySpecialist(specialistId, offset, limit)

  private def calculatePrice(hourlyRate: BigDecimal, duration: Option[Int]): BigDecimal =
    duration match
      case Some(minutes) => (hourlyRate * minutes) / 60
      case None          => hourlyRate // Default to 1 hour if duration not yet set

  private def calculateNewRating(
    currentRating: Option[BigDecimal],
    totalConsultations: Int,
    newRating: Int
  ): BigDecimal =
    currentRating match
      case Some(rating) =>
        val totalRating = rating * totalConsultations + newRating
        totalRating / (totalConsultations + 1)
      case None => BigDecimal(newRating)

  /**
   * Check if a status transition requires sending notifications
   *
   * This avoids fetching user/specialist data when no notifications will be sent. Transitions that produce
   * notifications:
   *   - Requested -> Scheduled (approval)
   *   - Requested -> Cancelled (decline)
   *   - Scheduled -> Completed
   *   - Scheduled -> Missed
   *   - Scheduled -> Cancelled
   *   - InProgress -> Completed All other transitions have no notifications.
   */
  private def statusTransitionRequiresNotifications(
    oldStatus: ConsultationStatus,
    newStatus: ConsultationStatus
  ): Boolean =
    (oldStatus, newStatus) match
      case (ConsultationStatus.Requested, ConsultationStatus.Scheduled)  => true
      case (ConsultationStatus.Requested, ConsultationStatus.Cancelled)  => true
      case (ConsultationStatus.Scheduled, ConsultationStatus.Completed)  => true
      case (ConsultationStatus.Scheduled, ConsultationStatus.Missed)     => true
      case (ConsultationStatus.Scheduled, ConsultationStatus.Cancelled)  => true
      case (ConsultationStatus.InProgress, ConsultationStatus.Completed) => true
      case _                                                             => false

  private def sendStatusChangeNotifications(
    consultation: Consultation,
    newStatus: ConsultationStatus,
    userOpt: Option[User],
    specialistOpt: Option[Specialist]
  ): IO[Unit] =
    val oldStatus = consultation.status

    // Build notification list based on available entities and transition type.
    // Note: for some transitions (such as approval), notifications are only sent when both user and specialist are available.
    val notificationsToSend: List[NotificationToSend] = (oldStatus, newStatus) match
      // User requested, specialist approves - sends to both only when both user and specialist are available
      case (ConsultationStatus.Requested, ConsultationStatus.Scheduled) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Approved"
            val userBody = s"""
                              |Hello ${user.name},
                              |
                              |Your consultation request with ${specialist.name} has been approved and scheduled!
                              |
                              |Date & Time: ${consultation.scheduledAt}
                              |Duration: ${consultation.duration.getOrElse("TBD")} minutes
                              |
                              |Please make sure you are available at the scheduled time.
                              |
                              |Best regards,
                              |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationApproved
            )
          }
        }

        specialistOpt.foreach { specialist =>
          userOpt.foreach { user =>
            val specialistSubject = "New Consultation Scheduled"
            val specialistBody = s"""
                                    |Hello ${specialist.name},
                                    |
                                    |You have approved a consultation with ${user.name}.
                                    |
                                    |Date & Time: ${consultation.scheduledAt}
                                    |Duration: ${consultation.duration.getOrElse("TBD")} minutes
                                    |Category: ${consultation.categoryId}
                                    |
                                    |Best regards,
                                    |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              SpecialistRecipient(specialist.email),
              specialistSubject,
              specialistBody,
              NotificationType.ConsultationApproved
            )
          }
        }

        notifications.toList

      // User requested, specialist declines - sends to user only if available
      case (ConsultationStatus.Requested, ConsultationStatus.Cancelled) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Request Declined"
            val userBody = s"""
                              |Hello ${user.name},
                              |
                              |Unfortunately, ${specialist.name} has declined your consultation request.
                              |
                              |You may want to request a consultation with another specialist.
                              |
                              |Best regards,
                              |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationDeclined
            )
          }
        }

        notifications.toList

      // Scheduled consultation marked as completed - sends to both if available
      case (ConsultationStatus.Scheduled, ConsultationStatus.Completed) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Completed"
            val userBody = s"""
                              |Hello ${user.name},
                              |
                              |Your consultation with ${specialist.name} has been marked as completed.
                              |
                              |You can now rate and review your experience. Your feedback is valuable!
                              |
                              |Best regards,
                              |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationCompleted
            )
          }
        }

        specialistOpt.foreach { specialist =>
          userOpt.foreach { user =>
            val specialistSubject = "Consultation Completed"
            val specialistBody = s"""
                                    |Hello ${specialist.name},
                                    |
                                    |Your consultation with ${user.name} has been marked as completed.
                                    |
                                    |Best regards,
                                    |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              SpecialistRecipient(specialist.email),
              specialistSubject,
              specialistBody,
              NotificationType.ConsultationCompleted
            )
          }
        }

        notifications.toList

      // Scheduled consultation marked as missed - sends to both if available
      case (ConsultationStatus.Scheduled, ConsultationStatus.Missed) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Marked as Missed"
            val userBody =
              s"""
                 |Hello ${user.name},
                 |
                 |Your consultation with ${specialist.name} scheduled for ${consultation.scheduledAt} has been marked as missed.
                 |
                 |If you would like to reschedule, please contact the specialist.
                 |
                 |Best regards,
                 |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationMissed
            )
          }
        }

        specialistOpt.foreach { specialist =>
          userOpt.foreach { user =>
            val specialistSubject = "Consultation Marked as Missed"
            val specialistBody =
              s"""
                 |Hello ${specialist.name},
                 |
                 |Your consultation with ${user.name} scheduled for ${consultation.scheduledAt} has been marked as missed.
                 |
                 |Best regards,
                 |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              SpecialistRecipient(specialist.email),
              specialistSubject,
              specialistBody,
              NotificationType.ConsultationMissed
            )
          }
        }

        notifications.toList

      // Scheduled consultation cancelled - sends to both if available
      case (ConsultationStatus.Scheduled, ConsultationStatus.Cancelled) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Cancelled"
            val userBody =
              s"""
                 |Hello ${user.name},
                 |
                 |Your consultation with ${specialist.name} scheduled for ${consultation.scheduledAt} has been cancelled.
                 |
                 |If you need further assistance, please feel free to request another consultation.
                 |
                 |Best regards,
                 |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationCancelled
            )
          }
        }

        specialistOpt.foreach { specialist =>
          userOpt.foreach { user =>
            val specialistSubject = "Consultation Cancelled"
            val specialistBody =
              s"""
                 |Hello ${specialist.name},
                 |
                 |Your consultation with ${user.name} scheduled for ${consultation.scheduledAt} has been cancelled.
                 |
                 |Best regards,
                 |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              SpecialistRecipient(specialist.email),
              specialistSubject,
              specialistBody,
              NotificationType.ConsultationCancelled
            )
          }
        }

        notifications.toList

      // In progress to other statuses - sends to user only if available
      case (ConsultationStatus.InProgress, ConsultationStatus.Completed) =>
        val notifications = scala.collection.mutable.ListBuffer[NotificationToSend]()

        userOpt.foreach { user =>
          specialistOpt.foreach { specialist =>
            val userSubject = "Consultation Completed"
            val userBody = s"""
                              |Hello ${user.name},
                              |
                              |Your consultation with ${specialist.name} has been completed.
                              |
                              |Thank you for using Consultant!
                              |
                              |Best regards,
                              |Consultant Team
              """.stripMargin
            notifications += NotificationToSend(
              ClientRecipient(user.email, user.id),
              userSubject,
              userBody,
              NotificationType.ConsultationCompleted
            )
          }
        }

        notifications.toList

      // Default: no notification for other transitions
      case _ => List()

    // Check preferences and send only if enabled (with error handling)
    val emailsToSend: List[IO[Unit]] = notificationsToSend.flatMap { notification =>
      notification.recipient match
        case ClientRecipient(email, userId) =>
          // For user notifications, check user preferences
          List(
            notificationPreferenceRepo
              .findByUserAndType(userId, notification.notificationType)
              .flatMap {
                case Some(pref) if !pref.emailEnabled =>
                  // User explicitly disabled this notification type, skip sending
                  IO.unit
                case _ =>
                  // Either no preference record exists (treat as default: enabled)
                  // or preference exists and is enabled. Send the email.
                  notificationService
                    .sendEmail(email, notification.subject, notification.body)
                    .attempt
                    .flatMap {
                      case Right(_) => IO.unit
                      case Left(error) =>
                        IO.println(
                          s"[WARNING] Failed to send email to $email for consultation status change: ${error.getMessage}"
                        )
                    }
              }
          )
        case SpecialistRecipient(email) =>
          // For specialist notifications, always send (specialists don't have preferences yet)
          // In future, can add specialist preferences similarly
          // Wrap with error handling: log failures but don't fail the operation
          List(
            notificationService
              .sendEmail(email, notification.subject, notification.body)
              .attempt
              .flatMap {
                case Right(_) => IO.unit
                case Left(error) =>
                  IO.println(
                    s"[WARNING] Failed to send email to $email for consultation status change: ${error.getMessage}"
                  )
              }
          )
    }

    emailsToSend.sequence.void
