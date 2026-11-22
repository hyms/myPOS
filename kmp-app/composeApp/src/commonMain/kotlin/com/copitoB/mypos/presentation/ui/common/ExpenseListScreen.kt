package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_refresh
import com.copitoB.mypos.generated.resources.action_save
import com.copitoB.mypos.generated.resources.content_desc_add_expense
import com.copitoB.mypos.generated.resources.content_desc_create_expense_category
import com.copitoB.mypos.generated.resources.dialog_new_expense
import com.copitoB.mypos.generated.resources.expense_all_months
import com.copitoB.mypos.generated.resources.expense_category_label
import com.copitoB.mypos.generated.resources.expense_date
import com.copitoB.mypos.generated.resources.expense_empty_desc
import com.copitoB.mypos.generated.resources.expense_empty_title
import com.copitoB.mypos.generated.resources.expense_error_amount
import com.copitoB.mypos.generated.resources.expense_error_category
import com.copitoB.mypos.generated.resources.expense_error_description
import com.copitoB.mypos.generated.resources.expense_label_category
import com.copitoB.mypos.generated.resources.expense_load_more
import com.copitoB.mypos.generated.resources.expense_payment_label
import com.copitoB.mypos.generated.resources.label_amount
import com.copitoB.mypos.generated.resources.label_category
import com.copitoB.mypos.generated.resources.label_description
import com.copitoB.mypos.generated.resources.payment_method_card
import com.copitoB.mypos.generated.resources.payment_method_cash
import com.copitoB.mypos.generated.resources.payment_method_qr
import com.copitoB.mypos.generated.resources.payment_method_transfer
import com.copitoB.mypos.generated.resources.quick_category_placeholder
import com.copitoB.mypos.generated.resources.quick_category_title
import com.copitoB.mypos.generated.resources.screen_expenses
import com.copitoB.mypos.presentation.viewmodel.ExpenseFormError
import org.jetbrains.compose.resources.stringResource
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
  expenses: List<Expense>,
  categories: List<ExpenseCategory>,
  months: List<String>,
  selectedMonth: String?,
  amountText: String,
  description: String,
  selectedCategoryId: Long?,
  selectedPaymentMethod: PaymentMethod,
  formError: ExpenseFormError?,
  hasMore: Boolean,
  loading: Boolean,
  onRefresh: () -> Unit,
  onMonthChange: (String?) -> Unit,
  onAmountChange: (String) -> Unit,
  onDescriptionChange: (String) -> Unit,
  onCategoryChange: (Long?) -> Unit,
  onPaymentMethodChange: (PaymentMethod) -> Unit,
  onRegister: (Money, String) -> Unit,
  onCreateCategory: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  openAddDialog: Boolean,
  onDialogShown: () -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }

  LaunchedEffect(openAddDialog) {
    if (openAddDialog) showDialog = true
  }

  LaunchedEffect(categories.size, formError) {
    if (selectedCategoryId == null && categories.isNotEmpty()) {
      onCategoryChange(categories.firstOrNull()?.id)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
      if (months.size > 1 || selectedMonth != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedMonth == null,
            onClick = { onMonthChange(null) },
            label = { Text(stringResource(Res.string.expense_all_months)) }
          )
          months.forEach { month ->
            FilterChip(
              selected = selectedMonth == month,
              onClick = { onMonthChange(month) },
              label = { Text(month) }
            )
          }
        }
      }

      if (expenses.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
          Text(text = stringResource(Res.string.expense_empty_title), style = MaterialTheme.typography.titleMedium)
          Text(
            text = stringResource(Res.string.expense_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(12.dp)
        ) {
          items(expenses, key = { it.id ?: 0L }, contentType = { "expense" }) { expense ->
            Card(
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = expense.description, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = stringResource(Res.string.expense_date, formatTimestampOrRaw(expense.registeredAt)),
                    style = MaterialTheme.typography.bodySmall
                  )
                  Text(
                    text = stringResource(
                      Res.string.expense_label_category,
                      categoryName(categories, expense.categoryId),
                      paymentMethodLabel(expense.paymentMethod)
                    ),
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                Text(text = expense.amount.toString(), style = MaterialTheme.typography.titleMedium)
              }
            }
          }
          if (hasMore) {
            item(key = "load-more", contentType = { "footer" }) {
              TextButton(onClick = onLoadNextPage, enabled = !loading) {
                Text(stringResource(Res.string.expense_load_more))
              }
            }
          }
        }
      }
    }

  if (showDialog) {
    AddExpenseDialog(
      amountText = amountText,
      description = description,
      categories = categories,
      selectedCategoryId = selectedCategoryId,
      selectedPaymentMethod = selectedPaymentMethod,
      formError = formError,
      onDismiss = { showDialog = false },
      onAmountChange = onAmountChange,
      onDescriptionChange = onDescriptionChange,
      onCategoryChange = onCategoryChange,
      onPaymentMethodChange = onPaymentMethodChange,
      onCreateCategory = onCreateCategory,
      onConfirm = { amount, descriptionText ->
        onRegister(amount, descriptionText)
        showDialog = false
      }
    )
  }

  LaunchedEffect(showDialog) {
    if (!showDialog) onDialogShown()
  }
}

@Composable
private fun AddExpenseDialog(
  amountText: String,
  description: String,
  categories: List<ExpenseCategory>,
  selectedCategoryId: Long?,
  selectedPaymentMethod: PaymentMethod,
  formError: ExpenseFormError?,
  onDismiss: () -> Unit,
  onAmountChange: (String) -> Unit,
  onDescriptionChange: (String) -> Unit,
  onCategoryChange: (Long?) -> Unit,
  onPaymentMethodChange: (PaymentMethod) -> Unit,
  onCreateCategory: (String) -> Unit,
  onConfirm: (Money, String) -> Unit
) {
  var showCategoryDialog by remember { mutableStateOf(false) }

  val amount = runCatching { Money(BigDecimal(amountText.trim()).setScale(2, RoundingMode.HALF_UP), Currency.Bs) }.getOrNull()
  val canConfirm = amount != null && description.trim().isNotEmpty() && selectedCategoryId != null

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.dialog_new_expense)) },
    text = {
      Column {
        OutlinedTextField(
          value = amountText,
          onValueChange = onAmountChange,
          label = { Text(stringResource(Res.string.label_amount)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (formError is ExpenseFormError.Amount) {
          Text(
            stringResource(Res.string.expense_error_amount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        OutlinedTextField(
          value = description,
          onValueChange = onDescriptionChange,
          label = { Text(stringResource(Res.string.label_description)) },
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        if (formError is ExpenseFormError.Description) {
          Text(
            stringResource(Res.string.expense_error_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        Text(
          stringResource(Res.string.expense_category_label),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(top = 8.dp)
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          categories.forEach { category ->
            FilterChip(
              selected = selectedCategoryId == category.id,
              onClick = { onCategoryChange(category.id) },
              label = { Text(category.name) }
            )
          }
          IconButton(onClick = { showCategoryDialog = true }) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(Res.string.content_desc_create_expense_category)
            )
          }
        }
        if (formError is ExpenseFormError.Category) {
          Text(
            stringResource(Res.string.expense_error_category),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        Text(
          stringResource(Res.string.expense_payment_label),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(top = 8.dp)
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = selectedPaymentMethod == method,
              onClick = { onPaymentMethodChange(method) },
              label = { Text(paymentMethodLabel(method)) }
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { amount?.let { onConfirm(it, description) } },
        enabled = canConfirm
      ) {
        Text(stringResource(Res.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
    }
  )

  if (showCategoryDialog) {
    QuickExpenseCategoryDialog(
      onDismiss = { showCategoryDialog = false },
      onConfirm = { name ->
        onCreateCategory(name)
        showCategoryDialog = false
      }
    )
  }
}

@Composable
private fun QuickExpenseCategoryDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.quick_category_title)) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.label_category)) },
        placeholder = { Text(stringResource(Res.string.quick_category_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) {
        Text(stringResource(Res.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
    }
  )
}

private fun categoryName(categories: List<ExpenseCategory>, categoryId: Long?): String =
  categories.firstOrNull { it.id == categoryId }?.name ?: ""

@Composable
fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
  PaymentMethod.CASH -> stringResource(Res.string.payment_method_cash)
  PaymentMethod.CARD -> stringResource(Res.string.payment_method_card)
  PaymentMethod.TRANSFER -> stringResource(Res.string.payment_method_transfer)
  PaymentMethod.QR -> stringResource(Res.string.payment_method_qr)
}