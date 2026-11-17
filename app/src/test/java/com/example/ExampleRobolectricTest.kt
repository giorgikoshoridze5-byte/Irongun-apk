package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import com.example.data.IronfrontDatabase
import com.example.data.SettingsStore
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.WeaponCamo
import com.example.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ironfront", appName)
  }

  @Test
  fun `room loadout persistence stores primary secondary and attachments`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, IronfrontDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = GameRepository(db.dao(), SettingsStore(context))
      repo.ensureInitialized()

      val seeded = repo.loadoutsFlow.first()
      assertEquals(3, seeded.size)
      assertEquals("ASSAULT", seeded.first().slotName)

      repo.selectActiveLoadoutSlot(2)
      repo.updateLoadout(2) {
        it.copy(
          slotName = "SNIPER-PRO",
          primaryWeapon = WeaponType.AX50.name,
          secondaryWeapon = WeaponType.DEAGLE.name,
          primaryOptic = OpticAttachment.MARKSMAN_OPTIC.name,
          primaryMuzzle = MuzzleAttachment.SUPPRESSOR.name,
          primaryCamo = WeaponCamo.GOLD_ELITE.name,
          secondaryCamo = WeaponCamo.ARCTIC_WHITE.name
        )
      }

      val active = repo.activeLoadoutFlow.first()
      assertEquals(2, active.slotIndex)
      assertEquals("SNIPER-PRO", active.slotName)
      assertEquals(WeaponType.AX50.name, active.primaryWeapon)
      assertEquals(WeaponType.DEAGLE.name, active.secondaryWeapon)
      assertEquals(OpticAttachment.MARKSMAN_OPTIC.name, active.primaryOptic)
      assertEquals(MuzzleAttachment.SUPPRESSOR.name, active.primaryMuzzle)
      assertEquals(WeaponCamo.GOLD_ELITE.name, active.primaryCamo)
      assertEquals(WeaponCamo.ARCTIC_WHITE.name, active.secondaryCamo)
    } finally {
      db.close()
    }
  }
}
