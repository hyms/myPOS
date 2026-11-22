package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_save
import com.copitoB.mypos.generated.resources.catalog_category_all
import com.copitoB.mypos.generated.resources.catalog_form_category_label
import com.copitoB.mypos.generated.resources.catalog_form_category_required
import com.copitoB.mypos.generated.resources.catalog_modal_category_label
import com.copitoB.mypos.generated.resources.catalog_modal_category_placeholder
import com.copitoB.mypos.generated.resources.catalog_modal_category_title
import com.copitoB.mypos.generated.resources.content_desc_back
import com.copitoB.mypos.generated.resources.error_invalid_number
import com.copitoB.mypos.generated.resources.label_current_stock
import com.copitoB.mypos.generated.resources.label_min_stock
import com.copitoB.mypos.generated.resources.label_name
import com.copitoB.mypos.generated.resources.label_purchase_price
import com.copitoB.mypos.generated.resources.label_selling_price
import com.copitoB.mypos.generated.resources.label_sku
import com.copitoB.mypos.generated.resources.screen_edit_product
import com.copitoB.mypos.generated.resources.screen_new_product
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
  product: Product? = null,
  categories: List<Category> = emptyList(),
  onSave: (ProductInput) -> Unit,
  onCancel: () -> Unit,
  onCreateCategory: (String) -> Unit = {}
) {
  var name by remember { mutableStateOf(product?.name ?: "") }
  var sku by remember { mutableStateOf(product?.sku?.value ?: "") }
  var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.amount?.toString() ?: "") }
  var purchasePrice by remember { mutableStateOf(product?.purchasePrice?.amount?.toString() ?: "") }
  var currentStock by remember { mutableStateOf(product?.currentStock?.value?.toString() ?: "") }
  var minStock by remember { mutableStateOf(product?.minStock?.value?.toString() ?: "") }
  var categoryId by remember { mutableStateOf(product?.categoryId) }
  var categoryError by remember { mutableStateOf(false) }
  var showCategoryDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(if (product == null) Res.string.screen_new_product else Res.string.screen_edit_product)) },
        navigationIcon = {
          IconButton(onClick = onCancel) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(Res.string.content_desc_back)
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.label_name)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )
      TextField(
        value = sku,
        onValueChange = { sku = it },
        label = { Text(stringResource(Res.string.label_sku)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )
      TextField(
        value = sellingPrice,
        onValueChange = { sellingPrice = it },
        label = { Text(stringResource(Res.string.label_selling_price)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )
      TextField(
        value = purchasePrice,
        onValueChange = { purchasePrice = it },
        label = { Text(stringResource(Res.string.label_purchase_price)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )
      TextField(
        value = currentStock,
        onValueChange = { currentStock = it },
        label = { Text(stringResource(Res.string.label_current_stock)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )
      TextField(
        value = minStock,
        onValueChange = { minStock = it },
        label = { Text(stringResource(Res.string.label_min_stock)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      )

      CategorySelector(
        selectedId = categoryId,
        categories = categories,
        onSelect = { categoryId = it; categoryError = false },
        onCreateNew = { showCategoryDialog = true }
      )
      if (categoryError) {
        Text(
          text = stringResource(Res.string.catalog_form_category_required),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(onClick = {
          if (categoryId == null) {
            categoryError = true
          } else {
            onSave(
              ProductInput(
                categoryId = categoryId!!,
                sku = sku.takeIf { it.isNotEmpty() },
                name = name,
                sellingPrice = sellingPrice,
                purchasePrice = purchasePrice,
                currentStock = currentStock,
                minStock = minStock,
                imageUri = product?.imageUri
              )
            )
          }
        }) {
          Text(stringResource(Res.string.action_save))
        }
        TextButton(onClick = onCancel) {
          Text(stringResource(Res.string.action_cancel))
        }
      }
    }
  }

  if (showCategoryDialog) {
    QuickCategoryDialog(
      onCreate = { newName ->
        onCreateCategory(newName)
        showCategoryDialog = false
      },
      onDismiss = { showCategoryDialog = false }
    )
  }
}

@Composable
fun CategorySelector(
  selectedId: Long?,
  categories: List<Category>,
  onSelect: (Long) -> Unit,
  onCreateNew: () -> Unit
) {
  Text(text = stringResource(Res.string.catalog_form_category_label), style = MaterialTheme.typography.labelLarge)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilterChip(
      selected = selectedId == null,
      onClick = { },
      label = { Text(stringResource(Res.string.catalog_category_all)) }
    )
    categories.forEach { category ->
      FilterChip(
        selected = selectedId == category.id,
        onClick = { category.id?.let(onSelect) },
        label = { Text(category.name) }
      )
    }
    FilterChip(
      selected = false,
      onClick = onCreateNew,
      label = { Text("+ " + stringResource(Res.string.catalog_modal_category_label)) }
    )
  }
}

@Composable
fun QuickCategoryDialog(
  onCreate: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.catalog_modal_category_title)) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.catalog_modal_category_label)) },
        placeholder = { Text(stringResource(Res.string.catalog_modal_category_placeholder)) },
        isError = name.isBlank(),
        singleLine = true
      )
    },
    confirmButton = {
      TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }, enabled = name.isNotBlank()) {
        Text(stringResource(Res.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.action_cancel))
      }
    }
  )
}

@Composable
private fun TextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    keyboardOptions = keyboardOptions,
    singleLine = true,
    modifier = modifier
  )
}