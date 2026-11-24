package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.copitoB.mypos.application.security.Sha256
import com.copitoB.mypos.application.security.VerifyPin
import com.copitoB.mypos.data.repository.SqliteBackupRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportImportRoundTripTest {

  private fun repo(driver: SqlDriver) = SqliteBackupRepository(driver)

  private fun importUseCase(driver: SqlDriver): ImportBackupUseCase {
    val pinStorage = FakePinHashStorage()
    return ImportBackupUseCase(
      repository = repo(driver),
      codec = JsonCodec(),
      backupFileIo = TestBackupFileIo(driver),
      verifyPin = VerifyPin(pinStorage)
    )
  }

  private fun seed(driver: SqlDriver) {
    val data = BackupDataDto(
      settings = BackupSettingsDto("Bs", 15, false),
      categories = listOf(BackupCategoryDto(1L, "General", "2025-01-15T10:00:00Z")),
      expenseCategories = listOf(BackupExpenseCategoryDto(1L, "General", "2025-01-15T10:00:00Z")),
      products = listOf(
        BackupProductDto(
          id = 1L,
          categoryId = 1L,
          sku = null,
          name = "Widget",
          sellingPrice = 25.0,
          purchasePrice = 10.0,
          currentStock = 10.0,
          minStock = 2.0,
          imageUri = null,
          registeredAt = "2025-01-15T10:00:00Z",
          updatedAt = "2025-01-15T10:00:00Z"
        )
      ),
      transactions = listOf(
        BackupTransactionDto(1L, "SALE", 100.0, "CASH", null, "2025-01-15T11:00:00Z", null),
        BackupTransactionDto(2L, "PURCHASE", 40.0, "CARD", null, "2025-01-15T12:00:00Z", null)
      ),
      transactionDetails = listOf(BackupTransactionDetailDto(1L, 1L, 1L, 4.0, 25.0)),
      accumulators = BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
    )
    val bytes = JsonCodec().encode(
      BackupEnvelopeDto("tpv-backup", 2, "2.0.0", "2025-01-15T13:00:00Z", "kmp", data)
    )
    importUseCase(driver)(BackupSource.Json(bytes), "")
  }

  @Test
  fun `export then import into fresh database reproduces identical state`() {
    val sourceDriver = TestDb.createDriver()
    seed(sourceDriver)
    val sourceRepo = repo(sourceDriver)

    val exported = sourceRepo.exportAll()
    val bytes = JsonCodec().encode(
      BackupEnvelopeDto(
        format = "tpv-backup",
        schemaVersion = 2,
        appVersion = "2.0.0",
        exportedAt = "2025-01-15T13:00:00Z",
        source = "kmp",
        data = exported
      )
    )

    val targetDriver = TestDb.createDriver()
    val report = importUseCase(targetDriver)(BackupSource.Json(bytes), "")
    assertTrue(report.inserted > 0)

    val targetRepo = repo(targetDriver)
    assertEquals(exported.categories, targetRepo.exportAll().categories)
    assertEquals(exported.products, targetRepo.exportAll().products)
    assertEquals(exported.transactions, targetRepo.exportAll().transactions)
    assertEquals(exported.transactionDetails, targetRepo.exportAll().transactionDetails)
  }

  @Test
  fun `accumulator equals recomputed sum after round trip`() {
    val sourceDriver = TestDb.createDriver()
    seed(sourceDriver)
    val exported = repo(sourceDriver).exportAll()

    val targetDriver = TestDb.createDriver()
    val bytes = JsonCodec().encode(
      BackupEnvelopeDto("tpv-backup", 2, "2.0.0", "2025-01-15T13:00:00Z", "kmp", exported)
    )
    importUseCase(targetDriver)(BackupSource.Json(bytes), "")

    val acc = repo(targetDriver).exportAll().accumulators
    assertEquals(100.0, acc.totalSales, 0.0001)
    assertEquals(40.0, acc.totalPurchases, 0.0001)
    assertEquals(0.0, acc.totalExpenses, 0.0001)
    assertEquals(60.0, acc.netBalance, 0.0001)
  }

  @Test
  fun `preserves soft-deleted rows across round trip`() {
    val sourceDriver = TestDb.createDriver()
    val repository = repo(sourceDriver)
    val data = BackupDataDto(
      settings = BackupSettingsDto("Bs", 15, false),
      categories = listOf(
        BackupCategoryDto(1L, "General", "2025-01-15T10:00:00Z"),
        BackupCategoryDto(2L, "Old", "2025-01-15T10:00:00Z", deletedAt = "2025-02-01T00:00:00Z")
      ),
      expenseCategories = listOf(BackupExpenseCategoryDto(1L, "General", "2025-01-15T10:00:00Z")),
      products = emptyList(),
      transactions = emptyList(),
      transactionDetails = emptyList(),
      accumulators = BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
    )
    repository.importAll(data)

    val bytes = JsonCodec().encode(
      BackupEnvelopeDto("tpv-backup", 2, "2.0.0", "2025-01-15T13:00:00Z", "kmp", repository.exportAll())
    )

    val targetDriver = TestDb.createDriver()
    importUseCase(targetDriver)(BackupSource.Json(bytes), "")

    val categories = repo(targetDriver).exportAll().categories
    assertEquals(2, categories.size)
    assertEquals("2025-02-01T00:00:00Z", categories[1].deletedAt)
  }
}