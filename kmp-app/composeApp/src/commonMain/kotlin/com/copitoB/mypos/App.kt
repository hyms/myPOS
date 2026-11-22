package com.copitoB.mypos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.copitoB.mypos.config.AppConfig
import com.copitoB.mypos.domain.entities.CartItemFactory
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_close
import com.copitoB.mypos.generated.resources.content_desc_add_expense
import com.copitoB.mypos.generated.resources.catalog_stock_adjusted
import com.copitoB.mypos.generated.resources.catalog_toast_product_deleted
import com.copitoB.mypos.generated.resources.catalog_toast_product_saved
import com.copitoB.mypos.generated.resources.checkout_toast_purchase_created
import com.copitoB.mypos.generated.resources.checkout_toast_sale_created
import com.copitoB.mypos.generated.resources.error_insufficient_stock
import com.copitoB.mypos.generated.resources.nav_cart
import com.copitoB.mypos.generated.resources.nav_cash
import com.copitoB.mypos.generated.resources.nav_movements
import com.copitoB.mypos.generated.resources.nav_expenses
import com.copitoB.mypos.generated.resources.nav_products
import com.copitoB.mypos.generated.resources.nav_settings
import com.copitoB.mypos.generated.resources.product_low_stock
import com.copitoB.mypos.presentation.ui.common.CartMode
import com.copitoB.mypos.presentation.ui.common.CashSummaryScreen
import com.copitoB.mypos.presentation.ui.common.MovementsScreen
import com.copitoB.mypos.presentation.ui.common.PlatformBackHandler
import com.copitoB.mypos.presentation.ui.common.ReportsScreen
import com.copitoB.mypos.presentation.ui.common.TransactionDetailSheet
import com.copitoB.mypos.presentation.ui.common.ExpenseListScreen
import com.copitoB.mypos.presentation.ui.common.ProductFormScreen
import com.copitoB.mypos.presentation.ui.common.ProductListScreen
import com.copitoB.mypos.presentation.ui.common.RegistroScreen
import com.copitoB.mypos.presentation.ui.common.SettingsScreen
import com.copitoB.mypos.presentation.ui.security.PinLockOverlay
import com.copitoB.mypos.presentation.theme.AppTheme
import com.copitoB.mypos.presentation.viewmodel.BackupViewModel
import com.copitoB.mypos.presentation.viewmodel.CartViewModel
import com.copitoB.mypos.presentation.viewmodel.CashSummaryViewModel
import com.copitoB.mypos.presentation.viewmodel.MovementsViewModel
import com.copitoB.mypos.presentation.viewmodel.ReportsViewModel
import com.copitoB.mypos.presentation.viewmodel.ReportPeriod
import com.copitoB.mypos.presentation.viewmodel.RevertState
import com.copitoB.mypos.presentation.viewmodel.CategoryViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseViewModel
import com.copitoB.mypos.presentation.viewmodel.PinViewModel
import com.copitoB.mypos.presentation.viewmodel.ProductViewModel
import com.copitoB.mypos.presentation.viewmodel.PurchaseCartViewModel
import com.copitoB.mypos.presentation.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

private enum class Destination(val label: StringResource, val icon: ImageVector) {
  Products(Res.string.nav_products, Icons.AutoMirrored.Filled.List),
  Cart(Res.string.nav_cart, Icons.Default.ShoppingCart),
  Movements(Res.string.nav_movements, Icons.Default.List),
  Expenses(Res.string.nav_expenses, Icons.Default.Warning),
  Cash(Res.string.nav_cash, Icons.Default.Home),
  Settings(Res.string.nav_settings, Icons.Default.Settings)
}

@Composable
private inline fun <reified T : Any> rememberFromKoin(): T =
  remember { GlobalContext.get().get<T>() }

@Composable
fun App() {
  val productViewModel: ProductViewModel = rememberFromKoin()
  val categoryViewModel: CategoryViewModel = rememberFromKoin()
  val cartViewModel: CartViewModel = rememberFromKoin()
  val purchaseCartViewModel: PurchaseCartViewModel = rememberFromKoin()
  val expenseViewModel: ExpenseViewModel = rememberFromKoin()
  val cashViewModel: CashSummaryViewModel = rememberFromKoin()
  val movementsViewModel: MovementsViewModel = rememberFromKoin()
  val reportsViewModel: ReportsViewModel = rememberFromKoin()
  val settingsViewModel: SettingsViewModel = rememberFromKoin()
  val pinViewModel: PinViewModel = rememberFromKoin()
  val backupViewModel: BackupViewModel = rememberFromKoin()

  var destination by remember { mutableStateOf(Destination.Products) }
  var showProductForm by remember { mutableStateOf(false) }
  var editingProduct by remember { mutableStateOf<com.copitoB.mypos.domain.entities.Product?>(null) }
  var pin by remember { mutableStateOf("") }
  var mode by remember { mutableStateOf(CartMode.SALE) }
  var openExpenseDialog by remember { mutableStateOf(false) }

  val products by productViewModel.products.collectAsState()
  val categories by categoryViewModel.categories.collectAsState()
  val search by productViewModel.search.collectAsState()
  val selectedCategory by productViewModel.categoryId.collectAsState()
  val sort by productViewModel.sort.collectAsState()
  val lowStockOnly by productViewModel.lowStockOnly.collectAsState()
  val productsLoading by productViewModel.loading.collectAsState()
  val productsHasMore by productViewModel.hasMore.collectAsState()
  val lowStockAlerts by productViewModel.lowStockAlerts.collectAsState()
  val saleItems by cartViewModel.items.collectAsState()
  val purchaseItems by purchaseCartViewModel.items.collectAsState()
  val discount by cartViewModel.discount.collectAsState()
  val cartError by cartViewModel.error.collectAsState()
  val purchaseCartError by purchaseCartViewModel.error.collectAsState()
  val productError by productViewModel.error.collectAsState()
  val expenses by expenseViewModel.expenses.collectAsState()
  val expenseCategories by expenseViewModel.categories.collectAsState()
  val expenseMonths by expenseViewModel.availableMonths.collectAsState()
  val expenseMonth by expenseViewModel.month.collectAsState()
  val expenseAmount by expenseViewModel.amountText.collectAsState()
  val expenseDescription by expenseViewModel.description.collectAsState()
  val expenseCategoryId by expenseViewModel.selectedCategoryId.collectAsState()
  val expensePaymentMethod by expenseViewModel.selectedPaymentMethod.collectAsState()
  val expenseFormError by expenseViewModel.formError.collectAsState()
  val expenseHasMore by expenseViewModel.hasMore.collectAsState()
  val expenseLoading by expenseViewModel.loading.collectAsState()
  val movements by movementsViewModel.movements.collectAsState()
  val movementsFilter by movementsViewModel.filter.collectAsState()
  val movementsSearch by movementsViewModel.search.collectAsState()
  val movementsTotal by movementsViewModel.total.collectAsState()
  val movementsHasMore by movementsViewModel.hasMore.collectAsState()
  val movementsLoading by movementsViewModel.loading.collectAsState()
  val selectedMovement by movementsViewModel.selected.collectAsState()
  val movementDetail by movementsViewModel.detail.collectAsState()
  val movementProductNames by movementsViewModel.productNames.collectAsState()
  val revertState by movementsViewModel.revertState.collectAsState()
  val reportState by reportsViewModel.report.collectAsState()
  val reportPeriod by reportsViewModel.period.collectAsState()
  val reportFrom by reportsViewModel.from.collectAsState()
  val reportTo by reportsViewModel.to.collectAsState()
  val reportCustomError by reportsViewModel.customError.collectAsState()
  val reportLoading by reportsViewModel.loading.collectAsState()
  val cashSummary by cashViewModel.summary.collectAsState()
  val settings by settingsViewModel.settings.collectAsState()
  val hasPin by pinViewModel.hasPin.collectAsState()
  val pinStateLoaded by pinViewModel.pinStateLoaded.collectAsState()
  val unlocked by pinViewModel.unlocked.collectAsState()
  val pinError by pinViewModel.error.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val appScope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val feedback = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

  val lowStockAlert = lowStockAlerts.firstOrNull()
  val lowStockMessage = lowStockAlert?.let { alert ->
    stringResource(
      Res.string.product_low_stock,
      alert.product.name
    )
  }

  val insufficientStockMessage = stringResource(Res.string.error_insufficient_stock)
  val saleCreatedMessage = stringResource(Res.string.checkout_toast_sale_created)
  val purchaseCreatedMessage = stringResource(Res.string.checkout_toast_purchase_created)
  val productSavedMessage = stringResource(Res.string.catalog_toast_product_saved)
  val productDeletedMessage = stringResource(Res.string.catalog_toast_product_deleted)
  val stockAdjustedMessage = stringResource(Res.string.catalog_stock_adjusted)

  LaunchedEffect(Unit) {
    pinViewModel.refresh()
    categoryViewModel.loadCategories()
    productViewModel.loadProducts()
    settings?.let { productViewModel.setPageSize(it.pageSize) }
  }

  LaunchedEffect(settings?.pageSize) {
    settings?.let { productViewModel.setPageSize(it.pageSize) }
  }

  LaunchedEffect(lowStockMessage) {
    if (lowStockMessage != null) {
      snackbarHostState.showSnackbar(lowStockMessage)
    }
  }

  LaunchedEffect(productError) {
    val e = productError
    if (e != null) {
      snackbarHostState.showSnackbar(e)
      productViewModel.consumeError()
    }
  }

  LaunchedEffect(cartError, purchaseCartError) {
    val e = cartError ?: purchaseCartError
    if (e == "stock_error") {
      snackbarHostState.showSnackbar(insufficientStockMessage)
      cartViewModel.consumeError()
      purchaseCartViewModel.consumeError()
    } else if (e != null) {
      snackbarHostState.showSnackbar(e)
      cartViewModel.consumeError()
      purchaseCartViewModel.consumeError()
    }
  }

  val locked = !pinStateLoaded || (hasPin && !unlocked)

  PlatformBackHandler(
    enabled = locked || showProductForm || openExpenseDialog || selectedMovement != null ||
      revertState !is RevertState.Idle || destination != Destination.Products
  ) {
    when {
      locked -> Unit
      showProductForm -> {
        editingProduct = null
        showProductForm = false
      }
      selectedMovement != null -> movementsViewModel.closeDetail()
      revertState !is RevertState.Idle -> movementsViewModel.cancelRevert()
      openExpenseDialog -> openExpenseDialog = false
      destination != Destination.Products -> {
        showProductForm = false
        destination = Destination.Products
      }
    }
  }

  AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
          if (destination == Destination.Expenses && !showProductForm) {
            FloatingActionButton(onClick = { openExpenseDialog = true }) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.content_desc_add_expense)
              )
            }
          }
        }
      ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
          Box(modifier = Modifier.weight(1f)) {
          if (showProductForm) {
            ProductFormScreen(
              product = editingProduct,
              categories = categories,
              onSave = { input ->
                editingProduct?.let { existing ->
                  productViewModel.updateProduct(existing.id!!, input) {
                    editingProduct = null
                    showProductForm = false
                    appScope.launch { snackbarHostState.showSnackbar(productSavedMessage) }
                  }
                } ?: run {
                  productViewModel.createProduct(input) {
                    showProductForm = false
                    appScope.launch { snackbarHostState.showSnackbar(productSavedMessage) }
                  }
                }
              },
              onCancel = {
                editingProduct = null
                showProductForm = false
              },
              onCreateCategory = { name -> categoryViewModel.createCategory(name) }
            )
          } else {
            when (destination) {
              Destination.Products -> ProductListScreen(
                products = products,
                categories = categories,
                search = search,
                selectedCategory = selectedCategory,
                sort = sort,
                lowStockOnly = lowStockOnly,
                loading = productsLoading,
                hasMore = productsHasMore,
                onSearchChange = { productViewModel.setSearch(it) },
                onCategoryChange = { productViewModel.setCategory(it) },
                onSortChange = { productViewModel.setSort(it) },
                onLowStockChange = { productViewModel.setLowStockOnly(it) },
                onLoadNextPage = { productViewModel.loadNextPage() },
                onProductClick = { product ->
                  if (mode == CartMode.SALE) {
                    cartViewModel.addItem(
                      CartItemFactory.create(
                        productId = product.id ?: 0L,
                        productName = product.name,
                        quantity = 1.0,
                        unitPrice = product.sellingPrice.amount.toDouble()
                      )
                    )
                  } else {
                    purchaseCartViewModel.addItem(
                      CartItemFactory.create(
                        productId = product.id ?: 0L,
                        productName = product.name,
                        quantity = 1.0,
                        unitPrice = product.purchasePrice.amount.toDouble()
                      )
                    )
                  }
                  destination = Destination.Cart
                },
                onAddProduct = {
                  editingProduct = null
                  showProductForm = true
                },
                onEdit = { product ->
                  editingProduct = product
                  showProductForm = true
                },
                onDelete = { id ->
                  productViewModel.deleteProduct(id) {
                    appScope.launch { snackbarHostState.showSnackbar(productDeletedMessage) }
                  }
                },
                onAdjustStock = { id, newStock, reason ->
                  productViewModel.adjustStock(id, newStock, reason) { ok ->
                    if (ok) appScope.launch { snackbarHostState.showSnackbar(stockAdjustedMessage) }
                  }
                }
              )

              Destination.Cart -> RegistroScreen(
                mode = mode,
                saleItems = saleItems,
                purchaseItems = purchaseItems,
                onModeChange = { mode = it },
                onAddItem = { destination = Destination.Products },
                onRemoveSaleItem = { index -> cartViewModel.removeItem(index) },
                onRemovePurchaseItem = { index -> purchaseCartViewModel.removeItem(index) },
                onEditSaleItem = { index, item -> cartViewModel.replaceItem(index, item) },
                onEditPurchaseItem = { index, item -> purchaseCartViewModel.replaceItem(index, item) },
                onClearSale = { cartViewModel.clearCart() },
                onClearPurchase = { purchaseCartViewModel.clearCart() },
                onCheckoutSale = { method, received, appliedDiscount ->
                  cartViewModel.setDiscount(appliedDiscount.toString())
                  cartViewModel.checkout(method, received) { ok ->
                    if (ok) feedback()
                    productViewModel.refreshLowStock()
                    cashViewModel.loadSummary()
                    productViewModel.loadProducts()
                  }
                },
                onCheckoutPurchase = { method, appliedDiscount ->
                  purchaseCartViewModel.setDiscount(appliedDiscount.toString())
                  purchaseCartViewModel.checkout(method) { ok ->
                    if (ok) feedback()
                    productViewModel.refreshLowStock()
                    cashViewModel.loadSummary()
                    productViewModel.loadProducts()
                  }
                },
                discount = if (mode == CartMode.SALE) discount else purchaseCartViewModel.discount.value,
                onDiscountChange = { applied ->
                  if (mode == CartMode.SALE) cartViewModel.setDiscount(applied.toString())
                  else purchaseCartViewModel.setDiscount(applied.toString())
                }
              )

              Destination.Expenses -> ExpenseListScreen(
                expenses = expenses,
                categories = expenseCategories,
                months = expenseMonths,
                selectedMonth = expenseMonth,
                amountText = expenseAmount,
                description = expenseDescription,
                selectedCategoryId = expenseCategoryId,
                selectedPaymentMethod = expensePaymentMethod,
                formError = expenseFormError,
                hasMore = expenseHasMore,
                loading = expenseLoading,
                onRefresh = { expenseViewModel.loadExpenses() },
                onMonthChange = { expenseViewModel.setMonth(it) },
                onAmountChange = { expenseViewModel.setAmount(it) },
                onDescriptionChange = { expenseViewModel.setDescription(it) },
                onCategoryChange = { expenseViewModel.setCategory(it) },
                onPaymentMethodChange = { expenseViewModel.setPaymentMethod(it) },
                onRegister = { _, _ ->
                  expenseViewModel.registerExpense { ok ->
                    if (ok) cashViewModel.loadSummary()
                  }
                },
                onCreateCategory = { name ->
                  expenseViewModel.createCategory(name)
                },
                onLoadNextPage = { expenseViewModel.loadNextPage() },
                openAddDialog = openExpenseDialog,
                onDialogShown = { openExpenseDialog = false }
              )

              Destination.Movements -> {
                selectedMovement?.let { selected ->
                  TransactionDetailSheet(
                    transaction = selected,
                    details = movementDetail,
                    productNames = movementProductNames,
                    showPinGate = revertState is RevertState.AwaitingPin,
                    pin = pin,
                    pinError = pinError,
                    showConfirm = revertState is RevertState.Confirming,
                    reverting = revertState is RevertState.Reverting,
                    onDismiss = { movementsViewModel.closeDetail() },
                    onRevert = { movementsViewModel.requestRevert(hasPin) },
                    onPinChange = { pin = it },
                    onPinConfirm = {
                      pinViewModel.unlock(pin) { ok ->
                        if (ok) {
                          feedback()
                          movementsViewModel.confirmRevert { }
                        } else movementsViewModel.cancelRevert()
                      }
                    },
                    onPinCancel = { movementsViewModel.cancelRevert() },
                    onConfirmRevert = {
                      movementsViewModel.confirmRevert { ok ->
                        if (ok) cashViewModel.loadSummary()
                      }
                    },
                    onCancelConfirm = { movementsViewModel.cancelRevert() }
                  )
                }
                MovementsScreen(
                  movements = movements,
                  filter = movementsFilter,
                  search = movementsSearch,
                  total = movementsTotal,
                  hasMore = movementsHasMore,
                  loading = movementsLoading,
                  onFilterChange = { movementsViewModel.setFilter(it) },
                  onSearchChange = { movementsViewModel.setSearch(it) },
                  onLoadNextPage = { movementsViewModel.loadNextPage() },
                  onMovementClick = { movementsViewModel.openDetail(it) }
                )
              }

              Destination.Cash -> {
                ReportsScreen(
                  summary = cashSummary,
                  periodSummary = reportState.totals,
                  payment = reportState.payment,
                  monthly = reportState.monthly,
                  annual = reportState.annual,
                  range = when (reportPeriod) {
                    ReportPeriod.TODAY -> com.copitoB.mypos.presentation.ui.common.ReportRange.TODAY
                    ReportPeriod.MONTH -> com.copitoB.mypos.presentation.ui.common.ReportRange.MONTH
                    ReportPeriod.CUSTOM -> com.copitoB.mypos.presentation.ui.common.ReportRange.CUSTOM
                  },
                  customFrom = reportFrom,
                  customTo = reportTo,
                  customError = reportCustomError,
                  onRangeChange = { reportsViewModel.setPeriod(ReportPeriod.valueOf(it.name)) },
                  onApplyCustom = { from, to -> reportsViewModel.applyCustom(from, to) },
                  onRefresh = { cashViewModel.loadSummary(); reportsViewModel.load() }
                )
              }

              Destination.Settings -> SettingsScreen(
                settings = settings,
                pinViewModel = pinViewModel,
                backupViewModel = backupViewModel,
                onCurrencyChange = { code ->
                  settingsViewModel.updateSettings(UpdateSettingsInput(currencyCode = code))
                },
                onPageSizeChange = { size ->
                  settingsViewModel.updateSettings(UpdateSettingsInput(pageSize = size))
                  productViewModel.setPageSize(size)
                },
                onBiometricChange = { enabled ->
                  settingsViewModel.updateSettings(UpdateSettingsInput(biometricEnabled = enabled))
                },
                onRefresh = { settingsViewModel.loadSettings() },
                onImportFilePicked = { source, importPin ->
                  if (source != null) {
                    backupViewModel.import(source, importPin) {
                      settingsViewModel.loadSettings()
                      productViewModel.loadProducts()
                      productViewModel.refreshLowStock()
                    }
                  }
                }
              )
            }
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
                  Destination.Movements -> movementsViewModel.load()
                  Destination.Cash -> {
                    cashViewModel.loadSummary()
                    reportsViewModel.load()
                  }
                  Destination.Settings -> settingsViewModel.loadSettings()
                }
              },
              icon = { Icon(imageVector = dest.icon, contentDescription = stringResource(dest.label)) },
              label = { Text(stringResource(dest.label)) }
            )
          }
        }
      }

      }

      PinLockOverlay(
        visible = locked,
        pin = pin,
        onPinChange = { pin = it },
        error = pinError,
        onUnlock = {
          pinViewModel.unlock(pin) { ok ->
            if (ok) {
              feedback()
              pin = ""
            }
          }
        }
      )
    }
  }
}