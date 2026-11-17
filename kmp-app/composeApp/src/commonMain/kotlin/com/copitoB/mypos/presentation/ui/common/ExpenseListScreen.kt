package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
  expenses: List<Expense>,
  onAdd: (Money, String) -> Unit,
  onRefresh: () -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Gastos") },
        actions = {
          TextButton(onClick = onRefresh) { Text("Actualizar") }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showDialog = true }) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar gasto")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp)
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
                text = expense.registeredAt?.take(10) ?: "",
                style = MaterialTheme.typography.bodySmall
              )
            }
            Text(text = expense.amount.toString(), style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    }
  }

  if (showDialog) {
    AddExpenseDialog(
      onDismiss = { showDialog = false },
      onConfirm = { amount, description ->
        onAdd(amount, description)
        showDialog = false
      }
    )
  }
}

@Composable
private fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onConfirm: (Money, String) -> Unit
) {
  var amountText by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val amount = parseAmount(amountText)
  val canConfirm = amount != null && description.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Nuevo gasto") },
    text = {
      Column {
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Monto") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción") },
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { if (canConfirm && amount != null) onConfirm(amount, description) },
        enabled = canConfirm
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    }
  )
}

private fun parseAmount(text: String): Money? = try {
  Money(BigDecimal(text.trim()).setScale(2, RoundingMode.HALF_UP), Currency.Bs)
} catch (_: Exception) {
  null
}
