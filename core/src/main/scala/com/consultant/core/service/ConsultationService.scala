package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*

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
                _ <- notificationService.sendEmail(
                  user.email,
                  "Consultation Request Created",
                  s"Your consultation with ${specialist.name} has been requested."
                )
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
            _          <- consultationRepo.updateStatus(id, status)
            user       <- userRepo.findById(consultation.userId)
            specialist <- specialistRepo.findById(consultation.specialistId)
            _          <- sendStatusChangeNotifications(consultation, status, user, specialist)
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
            _          <- sendStatusChangeNotifications(consultation, ConsultationStatus.Scheduled, user, specialist)
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

  private def sendStatusChangeNotifications(
    consultation: Consultation,
    newStatus: ConsultationStatus,
    userOpt: Option[User],
    specialistOpt: Option[Specialist]
  ): IO[Unit] =
    val oldStatus = consultation.status
    (userOpt, specialistOpt) match
      case (Some(user), Some(specialist)) =>
        val notificationsToSend: List[(RecipientType, String, String, String, NotificationType)] = (oldStatus, newStatus) match
          // User requested, specialist approves
          case (ConsultationStatus.Requested, ConsultationStatus.Scheduled) =>
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
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationApproved),
              (RecipientType.Specialist, specialist.email, specialistSubject, specialistBody, NotificationType.ConsultationApproved)
            )

          // User requested, specialist declines
          case (ConsultationStatus.Requested, ConsultationStatus.Cancelled) =>
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
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationDeclined)
            )

          // Scheduled consultation marked as completed
          case (ConsultationStatus.Scheduled, ConsultationStatus.Completed) =>
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
            val specialistSubject = "Consultation Completed"
            val specialistBody = s"""
                                    |Hello ${specialist.name},
                                    |
                                    |Your consultation with ${user.name} has been marked as completed.
                                    |
                                    |Best regards,
                                    |Consultant Team
              """.stripMargin
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationCompleted),
              (RecipientType.Specialist, specialist.email, specialistSubject, specialistBody, NotificationType.ConsultationCompleted)
            )

          // Scheduled consultation marked as missed
          case (ConsultationStatus.Scheduled, ConsultationStatus.Missed) =>
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
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationMissed),
              (RecipientType.Specialist, specialist.email, specialistSubject, specialistBody, NotificationType.ConsultationMissed)
            )

          // Scheduled consultation cancelled
          case (ConsultationStatus.Scheduled, ConsultationStatus.Cancelled) =>
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
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationCancelled),
              (RecipientType.Specialist, specialist.email, specialistSubject, specialistBody, NotificationType.ConsultationCancelled)
            )

          // In progress to other statuses
          case (ConsultationStatus.InProgress, ConsultationStatus.Completed) =>
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
            List(
              (RecipientType.User, user.email, userSubject, userBody, NotificationType.ConsultationCompleted)
            )

          // Default: no notification for other transitions
          case _ => List()

        // Check preferences and send only if enabled
        val emailsToSend: List[IO[Unit]] = notificationsToSend.flatMap {
          case (recipientType, email, subject, body, notificationType) =>
            recipientType match
              case RecipientType.User =>
                // For user notifications, check user preferences
                List(
                  notificationPreferenceRepo
                    .findByUserAndType(user.id, notificationType)
                    .flatMap {
                      case Some(pref) if pref.emailEnabled =>
                        notificationService.sendEmail(email, subject, body)
                      case _ => IO.unit
                    }
                )
              case RecipientType.Specialist =>
                // For specialist notifications, always send (specialists don't have preferences yet)
                // In future, can add specialist preferences similarly
                List(notificationService.sendEmail(email, subject, body))
        }

        emailsToSend.sequence.void
      case _ => IO.unit
