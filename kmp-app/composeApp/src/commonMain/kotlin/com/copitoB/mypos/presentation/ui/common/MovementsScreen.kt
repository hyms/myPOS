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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.valueobjects.TransactionType
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.accumulator_movements_empty_title
import com.copitoB.mypos.generated.resources.accumulator_movements_filter_all
import com.copitoB.mypos.generated.resources.accumulator_movements_filter_expense
import com.copitoB.mypos.generated.resources.accumulator_movements_filter_purchase
import com.copitoB.mypos.generated.resources.accumulator_movements_filter_sale
import com.copitoB.mypos.generated.resources.accumulator_movements_load_more
import com.copitoB.mypos.generated.resources.accumulator_movements_search
import com.copitoB.mypos.generated.resources.accumulator_movements_total
import com.copitoB.mypos.generated.resources.screen_movements
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
  movements: List<SaleTransaction>,
  filter: TransactionType?,
  search: String,
  total: Long,
  hasMore: Boolean,
  loading: Boolean,
  onFilterChange: (TransactionType?) -> Unit,
  onSearchChange: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  onMovementClick: (SaleTransaction) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.screen_movements)) }
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          null to Res.string.accumulator_movements_filter_all,
          TransactionType.SALE to Res.string.accumulator_movements_filter_sale,
          TransactionType.PURCHASE to Res.string.accumulator_movements_filter_purchase,
          TransactionType.EXPENSE to Res.string.accumulator_movements_filter_expense
        ).forEach { (type, label) ->
          FilterChip(
            selected = filter == type,
            onClick = { onFilterChange(type) },
            label = { Text(stringResource(label)) }
          )
        }
      }
      OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        label = { Text(stringResource(Res.string.accumulator_movements_search)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
      )
      if (total > 0) {
        Text(
          text = stringResource(Res.string.accumulator_movements_total, total),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
      }

      if (movements.isEmpty()) {
        Text(
          text = stringResource(Res.string.accumulator_movements_empty_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(24.dp)
        )
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(12.dp)
        ) {
          items(movements, key = { it.id ?: 0L }, contentType = { "movement" }) { movement ->
            Card(
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              onClick = { onMovementClick(movement) }
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = movement.detail ?: movement.type,
                    style = MaterialTheme.typography.titleMedium
                  )
                  Text(
                    text = "${formatTimestampOrRaw(movement.registeredAt)} · ${paymentMethodLabel(movement.paymentMethod)}",
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                Text(text = movement.totalAmount.toString(), style = MaterialTheme.typography.titleMedium)
              }
            }
          }
          if (hasMore) {
            item(key = "load-more", contentType = { "footer" }) {
              TextButton(onClick = onLoadNextPage, enabled = !loading) {
                Text(stringResource(Res.string.accumulator_movements_load_more))
              }
            }
          }
        }
      }
    }
  }
}