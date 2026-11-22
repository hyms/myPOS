package com.copitoB.mypos.presentation.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.backup_error_export
import com.copitoB.mypos.generated.resources.backup_error_import
import com.copitoB.mypos.generated.resources.error_generic
import com.copitoB.mypos.generated.resources.error_insufficient_stock
import com.copitoB.mypos.generated.resources.expense_error_amount
import com.copitoB.mypos.generated.resources.pin_error_generic
import com.copitoB.mypos.generated.resources.pin_incorrect
import org.jetbrains.compose.resources.stringResource

/**
 * Returns a plain (non-composable) mapper for raw error keys, safe to call
 * inside coroutines such as snackbar effects.
 */
@Composable
fun rememberErrorMessage(): (String?) -> String? {
  val insufficientStock = stringResource(Res.string.error_insufficient_stock)
  val invalidAmount = stringResource(Res.string.expense_error_amount)
  val incorrectPin = stringResource(Res.string.pin_incorrect)
  val pinError = stringResource(Res.string.pin_error_generic)
  val exportError = stringResource(Res.string.backup_error_export)
  val importError = stringResource(Res.string.backup_error_import)
  val generic = stringResource(Res.string.error_generic)
  return remember(insufficientStock, invalidAmount, incorrectPin, pinError, exportError, importError, generic) {
    { raw: String? ->
      when (raw) {
        null -> null
        "stock_error" -> insufficientStock
        "invalid_amount", "expense_error_amount" -> invalidAmount
        "incorrect_pin" -> incorrectPin
        "pin_error" -> pinError
        "export_error" -> exportError
        "import_error" -> importError
        else -> generic
      }
    }
  }
}

@Composable
fun errorMessage(raw: String?): String? = rememberErrorMessage()(raw)