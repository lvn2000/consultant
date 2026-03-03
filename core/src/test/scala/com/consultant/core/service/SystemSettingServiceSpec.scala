package com.consultant.core.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.effect.IO
import com.consultant.core.domain._
import com.consultant.core.ports.SystemSettingRepository
import cats.effect.unsafe.implicits.global
import java.util.UUID
import java.time.Instant

class SystemSettingServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  "SystemSettingService" should "get system setting by key successfully" in {
    val systemSettingRepo = mock[SystemSettingRepository]
    val service           = new SystemSettingService(systemSettingRepo)

    val settingKey         = "test.setting"
    val settingId          = UUID.randomUUID()
    val settingValue       = "test value"
    val settingType        = SettingType.String
    val settingDescription = Some("Test setting description")

    val expectedSetting = SystemSetting(
      id = settingId,
      key = settingKey,
      value = settingValue,
      settingType = settingType,
      description = settingDescription,
      isPublic = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    systemSettingRepo.findByKey.expects(settingKey).returning(IO.pure(Some(expectedSetting)))

    val result = service.getSetting(settingKey).unsafeRunSync()

    result shouldBe Some(expectedSetting)
  }

  it should "return None when system setting by key not found" in {
    val systemSettingRepo = mock[SystemSettingRepository]
    val service           = new SystemSettingService(systemSettingRepo)

    val settingKey = "nonexistent.setting"

    systemSettingRepo.findByKey.expects(settingKey).returning(IO.pure(None))

    val result = service.getSetting(settingKey).unsafeRunSync()

    result shouldBe None
  }

  it should "update system setting successfully" in {
    val systemSettingRepo = mock[SystemSettingRepository]
    val service           = new SystemSettingService(systemSettingRepo)

    val settingKey         = "test.setting"
    val settingId          = UUID.randomUUID()
    val oldValue           = "old value"
    val newValue           = "new value"
    val settingType        = SettingType.String
    val settingDescription = Some("Test setting description")

    val existingSetting = SystemSetting(
      id = settingId,
      key = settingKey,
      value = oldValue,
      settingType = settingType,
      description = settingDescription,
      isPublic = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val updatedSetting = existingSetting.copy(value = newValue)

    systemSettingRepo.findByKey.expects(settingKey).returning(IO.pure(Some(existingSetting)))
    systemSettingRepo.update
      .expects(settingId, newValue, settingDescription, None)
      .returning(IO.pure(Some(updatedSetting)))

    val result = service.updateSetting(settingKey, newValue).unsafeRunSync()

    result shouldBe Right(Some(updatedSetting))
  }

  it should "create or update system setting successfully" in {
    val systemSettingRepo = mock[SystemSettingRepository]
    val service           = new SystemSettingService(systemSettingRepo)

    val settingKey         = "new.setting"
    val settingValue       = "new value"
    val settingType        = SettingType.String
    val settingDescription = Some("New setting description")
    val isPublic           = true

    val createdSetting = SystemSetting(
      id = UUID.randomUUID(),
      key = settingKey,
      value = settingValue,
      settingType = settingType,
      description = settingDescription,
      isPublic = isPublic,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    systemSettingRepo.upsert
      .expects(settingKey, settingValue, settingType, settingDescription, isPublic)
      .returning(IO.pure(createdSetting))

    val result =
      service.createOrUpdateSetting(settingKey, settingValue, settingType, settingDescription, isPublic).unsafeRunSync()

    result shouldBe Right(createdSetting)
  }
}
