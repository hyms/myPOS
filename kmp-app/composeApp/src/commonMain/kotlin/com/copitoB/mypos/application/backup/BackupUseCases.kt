package com.copitoB.mypos.application.backup

class ExportDatabase(
  private val fileSystem: FileSystem
) {
  operator fun invoke(): String {
    return fileSystem.copyDatabase()
  }
}

class RestoreDatabase(
  private val fileSystem: FileSystem,
  private val pinGate: PinGate
) {
  operator fun invoke(backupPath: String, pin: String) {
    require(pinGate.verifyPin(pin)) { "PIN required for restore" }
    fileSystem.restoreDatabase(backupPath)
    fileSystem.invalidateStores()
  }
}

interface FileSystem {
  fun copyDatabase(): String
  fun restoreDatabase(backupPath: String)
  fun invalidateStores()
}

interface PinGate {
  fun verifyPin(pin: String): Boolean
}
