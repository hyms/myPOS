package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductSort
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_confirm
import com.copitoB.mypos.generated.resources.action_delete
import com.copitoB.mypos.generated.resources.action_edit
import com.copitoB.mypos.generated.resources.catalog_category_all
import com.copitoB.mypos.generated.resources.catalog_delete_confirm_msg
import com.copitoB.mypos.generated.resources.catalog_delete_confirm_title
import com.copitoB.mypos.generated.resources.catalog_empty
import com.copitoB.mypos.generated.resources.catalog_low_stock_filter
import com.copitoB.mypos.generated.resources.catalog_search_hint
import com.copitoB.mypos.generated.resources.catalog_sort_label
import com.copitoB.mypos.generated.resources.catalog_sort_name
import com.copitoB.mypos.generated.resources.catalog_sort_popularity
import com.copitoB.mypos.generated.resources.catalog_stock_adjust_new_value
import com.copitoB.mypos.generated.resources.catalog_stock_adjust_reason
import com.copitoB.mypos.generated.resources.catalog_stock_adjust_title
import com.copitoB.mypos.generated.resources.content_desc_options
import com.copitoB.mypos.generated.resources.product_low_stock
import com.copitoB.mypos.generated.resources.product_sku
import com.copitoB.mypos.generated.resources.product_sku_missing
import com.copitoB.mypos.generated.resources.product_stock
import com.copitoB.mypos.generated.resources.screen_products
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
  products: List<Product>,
  categories: List<Category>,
  search: String,
  selectedCategory: Long?,
  sort: ProductSort,
  lowStockOnly: Boolean,
  loading: Boolean,
  hasMore: Boolean,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (Long?) -> Unit,
  onSortChange: (ProductSort) -> Unit,
  onLowStockChange: (Boolean) -> Unit,
  onLoadNextPage: () -> Unit,
  onProductClick: (Product) -> Unit,
  onEdit: (Product) -> Unit,
  onDelete: (Long) -> Unit,
  onAdjustStock: (Long, String, String) -> Unit
) {
  var pendingDelete by remember { mutableStateOf<Product?>(null) }
  var pendingAdjust by remember { mutableStateOf<Product?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.screen_products)) }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding)
    ) {
      OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        placeholder = { Text(stringResource(Res.string.catalog_search_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
      )

      FilterRow(
        categories = categories,
        selectedCategory = selectedCategory,
        sort = sort,
        lowStockOnly = lowStockOnly,
        onCategoryChange = onCategoryChange,
        onSortChange = onSortChange,
        onLowStockChange = onLowStockChange
      )

      if (products.isEmpty() && !loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(stringResource(Res.string.catalog_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return@Column
      }

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        gridItems(products, key = { it.id.toString() }) { product ->
          ProductCard(
            product = product,
            onClick = { onProductClick(product) },
            onEdit = { onEdit(product) },
            onDelete = { pendingDelete = product },
            onAdjustStock = { pendingAdjust = product }
          )
        }
      }
    }
  }

  pendingDelete?.let { product ->
    ConfirmDialog(
      title = stringResource(Res.string.catalog_delete_confirm_title),
      message = stringResource(Res.string.catalog_delete_confirm_msg),
      confirmLabel = stringResource(Res.string.action_delete),
      dismissLabel = stringResource(Res.string.action_cancel),
      onConfirm = {
        onDelete(product.id!!)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null }
    )
  }

  pendingAdjust?.let { product ->
    StockAdjustDialog(
      currentStock = product.currentStock.toDisplayString(),
      onConfirm = { newStock, reason ->
        onAdjustStock(product.id!!, newStock, reason)
        pendingAdjust = null
      },
      onDismiss = { pendingAdjust = null }
    )
  }
}

@Composable
private fun FilterRow(
  categories: List<Category>,
  selectedCategory: Long?,
  sort: ProductSort,
  lowStockOnly: Boolean,
  onCategoryChange: (Long?) -> Unit,
  onSortChange: (ProductSort) -> Unit,
  onLowStockChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CategoryChipsRow(
      categories = categories,
      selectedCategory = selectedCategory,
      onCategoryChange = onCategoryChange
    )
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    var sortMenu by remember { mutableStateOf(false) }
    Box {
      TextButton(onClick = { sortMenu = true }) {
        Text(
          text = stringResource(Res.string.catalog_sort_label) + ": " +
            (if (sort == ProductSort.NOMBRE_ASC) stringResource(Res.string.catalog_sort_name)
            else stringResource(Res.string.catalog_sort_popularity))
        )
      }
      DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.catalog_sort_name)) },
          onClick = { onSortChange(ProductSort.NOMBRE_ASC); sortMenu = false }
        )
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.catalog_sort_popularity)) },
          onClick = { onSortChange(ProductSort.POPULARIDAD_DESC); sortMenu = false }
        )
      }
    }
    FilterChip(
      selected = lowStockOnly,
      onClick = { onLowStockChange(!lowStockOnly) },
      label = { Text(stringResource(Res.string.catalog_low_stock_filter)) }
    )
  }
}

@Composable
private fun CategoryChipsRow(
  categories: List<Category>,
  selectedCategory: Long?,
  onCategoryChange: (Long?) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilterChip(
      selected = selectedCategory == null,
      onClick = { onCategoryChange(null) },
      label = { Text(stringResource(Res.string.catalog_category_all)) }
    )
    categories.forEach { category ->
      FilterChip(
        selected = selectedCategory == category.id,
        onClick = { onCategoryChange(category.id) },
        label = { Text(category.name) }
      )
    }
  }
}

@Composable
fun ProductCard(
  product: Product,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onAdjustStock: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      ProductPhotoPreview(
        path = product.imageUri,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
      )
      Spacer(modifier = Modifier.padding(4.dp))
      var menu by remember(product.id) { mutableStateOf(false) }
      Box {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(
              text = stringResource(
                Res.string.product_sku,
                product.sku?.value ?: stringResource(Res.string.product_sku_missing)
              ),
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = stringResource(
                Res.string.product_stock,
                product.currentStock.toDisplayString(),
                product.minStock.toDisplayString()
              ),
              style = MaterialTheme.typography.bodySmall,
              color = if (product.isBelowMinStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (product.isBelowMinStock()) {
              Text(
                text = stringResource(Res.string.product_low_stock),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
              )
            }
          }
          IconButton(onClick = { menu = true }) {
            Icon(
              Icons.Default.MoreVert,
              contentDescription = stringResource(Res.string.content_desc_options)
            )
          }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.action_edit)) },
            onClick = {
              menu = false
              onEdit()
            }
          )
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.catalog_stock_adjust_title)) },
            onClick = {
              menu = false
              onAdjustStock()
            }
          )
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.action_delete)) },
            onClick = {
              menu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun StockAdjustDialog(
  currentStock: String,
  onConfirm: (String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var value by remember { mutableStateOf(currentStock) }
  var reason by remember { mutableStateOf("") }
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.catalog_stock_adjust_title)) },
    text = {
      Column {
        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text(stringResource(Res.string.catalog_stock_adjust_new_value)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text(stringResource(Res.string.catalog_stock_adjust_reason)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(value, reason) }) {
        Text(stringResource(Res.string.action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.action_cancel))
      }
    }
  )
}

