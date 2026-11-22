package com.copitoB.mypos.application.backup

import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.application.backup.LegacyDbReader

actual class LegacyDbReader actual constructor() {
  actual fun read(path: String): BackupDataDto =
    throw UnsupportedOperationException("Legacy .db import is not supported on iOS yet")
}

actual class BackupFileIo actual constructor(context: Any?) {
  actual fun save(fileName: String, bytes: ByteArray): String? =
    throw UnsupportedOperationException("Backup file I/O is not supported on iOS yet")

  actual fun load(uri: Any?): BackupSource? =
    throw UnsupportedOperationException("Backup file I/O is not supported on iOS yet")
}