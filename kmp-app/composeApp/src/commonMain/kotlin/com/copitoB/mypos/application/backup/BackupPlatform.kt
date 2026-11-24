package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.serialization.json.Json

/**
 * Platform file I/O for backup export/import.
 * `context` is an Android `Context` (passed as `Any?` to stay platform-agnostic
 * in `commonMain`); ignored on other platforms.
 *
 * Android actual:
 *  - [save] writes bytes to `<filesDir>/backups/<fileName>` and returns the path.
 *  - [load] reads a picked `Uri` (passed as `Any?`), returning a [BackupSource].
 *    A `.db` file (SQLite magic header / `.db` suffix) is copied to a cache
 *    temp file and returned as [BackupSource.LegacySqliteFile]; otherwise the
 *    raw bytes are returned as [BackupSource.Json].
 *  - [openDriver] opens an existing SQLite database file as a [SqlDriver].
 *  - [createFreshDriver] creates a fresh empty SQLite database as a [SqlDriver].
 *  - [createValidator] creates a [BackupDbValidator] for inspecting .db files.
 *  - [stageCopy] copies the selected .db to a staging cache file for validation.
 *  - [snapshotActiveDb] creates a rollback snapshot of the active database.
 *  - [restoreActiveDb] restores the active database from a snapshot path.
 *  - [acquireWriteLock] / [releaseWriteLock] serialize concurrent writes during import.
 */
expect interface BackupFileIo {
  fun save(fileName: String, bytes: ByteArray): String?
  fun load(uri: Any?): BackupSource?
  fun openDriver(path: String): SqlDriver?
  fun createFreshDriver(): SqlDriver?
  fun createValidator(): BackupDbValidator
  fun stageCopy(path: String): String?
  fun snapshotActiveDb(): String?
  fun restoreActiveDb(snapshotPath: String?): Boolean
  fun acquireWriteLock(): Boolean
  fun releaseWriteLock()
}

expect fun createBackupFileIo(context: Any?): BackupFileIo

/**
 * Inspects a SQLite `.db` file and classifies it for import.
 * Android actual opens the file via `SQLiteDatabase.openDatabase` and uses
 * raw SQL for PRAGMA introspection and v1 legacy table reads.
 */
expect interface BackupDbValidator {
  fun open(path: String)
  fun classify(): BackupDbClassification
  fun stageInto(target: TpvDatabase): StagingResult
  fun close()
}

/**
 * Serializes/deserializes the JSON v2 backup envelope.
 * Pure `commonMain` implementation (no platform API needed).
 */
class JsonCodec {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun encode(envelope: BackupEnvelopeDto): ByteArray =
    json.encodeToString(BackupEnvelopeDto.serializer(), envelope).encodeToByteArray()

  fun decode(bytes: ByteArray): BackupEnvelopeDto =
    json.decodeFromString(BackupEnvelopeDto.serializer(), bytes.decodeToString())
}