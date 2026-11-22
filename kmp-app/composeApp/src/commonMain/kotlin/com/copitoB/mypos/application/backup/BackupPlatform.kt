package com.copitoB.mypos.application.backup

import kotlinx.serialization.json.Json

/**
 * Reads a legacy `rn-app` SQLite `.db` file into backup DTOs.
 * Android actual opens the file read-only via `SQLiteDatabase.openDatabase`.
 * iOS/other actuals throw `UnsupportedOperationException`.
 */
expect class LegacyDbReader() {
  fun read(path: String): BackupDataDto
}

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
 */
expect class BackupFileIo(context: Any?) {

  fun save(fileName: String, bytes: ByteArray): String?

  fun load(uri: Any?): BackupSource?
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