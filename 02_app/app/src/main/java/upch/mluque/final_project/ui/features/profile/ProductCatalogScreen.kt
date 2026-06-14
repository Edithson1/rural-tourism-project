package upch.mluque.final_project.ui.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.data.local.Product
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen(
    viewModel: MainViewModel,
    language: String,
    isSetupFlow: Boolean = false,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onNavigateToEditor: (Int?) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        products.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > 600

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = UiTranslations.getString(context, "catalog_title", language),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isSetupFlow) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            )
        },
        bottomBar = {
            if (isSetupFlow) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = products.isNotEmpty()
                    ) {
                        Text(UiTranslations.getString(context, "catalog_finish_setup", language))
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isSetupFlow) {
                Text(
                    text = UiTranslations.getString(context, "catalog_setup_desc", language),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Buscador (Igual al de AddVisitScreen)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(UiTranslations.getString(context, "search_product_hint", language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } }
                } else null,
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = UiTranslations.getString(context, "catalog_empty", language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                if (isLandscape) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { onNavigateToEditor(it.id) },
                                onDelete = { productToDelete = it }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { onNavigateToEditor(it.id) },
                                onDelete = { productToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(UiTranslations.getString(context, "catalog_delete_confirm_title", language)) },
            text = { Text(UiTranslations.getString(context, "catalog_delete_confirm_desc", language, productToDelete!!.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(productToDelete!!)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(UiTranslations.getString(context, "btn_confirm", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(UiTranslations.getString(context, "btn_cancel", language))
                }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    val activePrice = product.getActivePrice()
    val hasDiscount = activePrice < product.basePrice

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasDiscount) {
                        Text(
                            text = "${product.currency} ${String.format("%.2f", product.basePrice)}", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${product.currency} ${String.format("%.2f", activePrice)}", 
                            color = Color(0xFFE53935), // Rojo vibrante para oferta
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalOffer, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp).padding(2.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Text(
                            text = "${product.currency} ${String.format("%.2f", product.basePrice)}", 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = product.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { onEdit(product) }) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit", 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete(product) }) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
