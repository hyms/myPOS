package com.copitoB.mypos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.copitoB.mypos.domain.entities.CartItemFactory
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.presentation.ui.common.CartScreen
import com.copitoB.mypos.presentation.ui.common.CashSummaryScreen
import com.copitoB.mypos.presentation.ui.common.ExpenseListScreen
import com.copitoB.mypos.presentation.ui.common.ProductFormScreen
import com.copitoB.mypos.presentation.ui.common.ProductListScreen
import com.copitoB.mypos.presentation.ui.common.SettingsScreen
import com.copitoB.mypos.presentation.viewmodel.CartViewModel
import com.copitoB.mypos.presentation.viewmodel.CashSummaryViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseViewModel
import com.copitoB.mypos.presentation.viewmodel.ProductViewModel
import com.copitoB.mypos.presentation.viewmodel.SettingsViewModel
import org.koin.core.context.GlobalContext

private enum class Destination(val label: String, val icon: ImageVector) {
  Products("Productos", Icons.AutoMirrored.Filled.List),
  Cart("Carrito", Icons.Default.ShoppingCart),
  Expenses("Gastos", Icons.Default.Warning),
  Cash("Caja", Icons.Default.Home),
  Settings("Ajustes", Icons.Default.Settings)
}

@Composable
private inline fun <reified T : Any> rememberFromKoin(): T =
  remember { GlobalContext.get().get<T>() }

@Composable
fun App() {
  val productViewModel: ProductViewModel = rememberFromKoin()
  val cartViewModel: CartViewModel = rememberFromKoin()
  val expenseViewModel: ExpenseViewModel = rememberFromKoin()
  val cashViewModel: CashSummaryViewModel = rememberFromKoin()
  val settingsViewModel: SettingsViewModel = rememberFromKoin()

  var destination by remember { mutableStateOf(Destination.Products) }
  var showProductForm by remember { mutableStateOf(false) }

  val products by productViewModel.products.collectAsState()
  val cartItems by cartViewModel.items.collectAsState()
  val expenses by expenseViewModel.expenses.collectAsState()
  val cashSummary by cashViewModel.summary.collectAsState()
  val settings by settingsViewModel.settings.collectAsState()

  LaunchedEffect(Unit) {
    productViewModel.loadProducts()
  }

  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
          if (showProductForm) {
            ProductFormScreen(
              onSave = { input ->
                productViewModel.createProduct(input)
                showProductForm = false
              },
              onCancel = { showProductForm = false }
            )
          } else {
            when (destination) {
              Destination.Products -> ProductListScreen(
                products = products,
                onProductClick = { id ->
                  products.firstOrNull { it.id == id }?.let { product ->
                    cartViewModel.addItem(
                      CartItemFactory.create(
                        productId = product.id ?: 0L,
                        productName = product.name,
                        quantity = 1.0,
                        unitPrice = product.sellingPrice.amount.toDouble()
                      )
                    )
                    destination = Destination.Cart
                  }
                },
                onAddProduct = { showProductForm = true },
                onDelete = { id -> productViewModel.deleteProduct(id) }
              )

              Destination.Cart -> CartScreen(
                items = cartItems,
                onAddItem = { destination = Destination.Products },
                onRemoveItem = { index -> cartViewModel.removeItem(index) },
                onCheckout = {
                  cartViewModel.checkout(PaymentMethod.CASH, null)
                  productViewModel.loadProducts()
                  cashViewModel.loadSummary()
                },
                onClear = { cartViewModel.clearCart() }
              )

              Destination.Expenses -> ExpenseListScreen(
                expenses = expenses,
                onAdd = { amount, description ->
                  expenseViewModel.registerExpense(
                    categoryId = 1L,
                    amount = amount,
                    description = description,
                    paymentMethod = PaymentMethod.CASH
                  )
                  cashViewModel.loadSummary()
                },
                onRefresh = { expenseViewModel.loadExpenses() }
              )

              Destination.Cash -> CashSummaryScreen(
                summary = cashSummary,
                onRefresh = { cashViewModel.loadSummary() }
              )

              Destination.Settings -> SettingsScreen(
                settings = settings,
                onAdEnabledChange = { enabled ->
                  settingsViewModel.updateSettings(UpdateSettingsInput(adEnabled = enabled))
                },
                onBiometricChange = { enabled ->
                  settingsViewModel.updateSettings(UpdateSettingsInput(biometricEnabled = enabled))
                },
                onRefresh = { settingsViewModel.loadSettings() }
              )
            }
          }
        }

        NavigationBar {
          Destination.entries.forEach { dest ->
            NavigationBarItem(
              selected = destination == dest && !showProductForm,
              onClick = {
                showProductForm = false
                destination = dest
                when (dest) {
                  Destination.Products -> productViewModel.loadProducts()
                  Destination.Cart -> Unit
                  Destination.Expenses -> expenseViewModel.loadExpenses()
                  Destination.Cash -> cashViewModel.loadSummary()
                  Destination.Settings -> settingsViewModel.loadSettings()
                }
              },
              icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
              label = { Text(dest.label) }
            )
          }
        }
      }
    }
  }
}
