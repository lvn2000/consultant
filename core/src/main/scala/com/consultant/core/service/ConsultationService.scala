package com.consultant.core.service

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.domain.*
import com.consultant.core.ports.*

class ConsultationService(
  consultationRepo: ConsultationRepository,
  specialistRepo: SpecialistRepository,
  userRepo: UserRepository,
  notificationService: NotificationService
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
    consultationRepo.updateStatus(id, status).map(Right(_))

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
          consultationRepo.update(updated).map(_ => Right(()))
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
