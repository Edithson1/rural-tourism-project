package upch.mluque.final_project.ui.features.visits

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.ui.components.ServiceOption
import upch.mluque.final_project.ui.components.ServiceSelectorGrid
import upch.mluque.final_project.ui.theme.BrownPrimary
import upch.mluque.final_project.ui.theme.Final_projectTheme
import upch.mluque.final_project.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitScreen(
    visitCount: Int,
    language: String,
    onBack: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var nationalitySearch by remember { mutableStateOf("") }
    var selectedNationality by remember { mutableStateOf<Country?>(null) }
    var isNationalityExpanded by remember { mutableStateOf(false) }

    var selectedPriceType by remember { mutableStateOf("Rango") }
    var isPriceTypeExpanded by remember { mutableStateOf(false) }
    
    var selectedPriceValue by remember { mutableStateOf("") }
    var isPriceValueExpanded by remember { mutableStateOf(false) }
    
    var selectedCurrency by remember { mutableStateOf("S/") }
    var isCurrencyExpanded by remember { mutableStateOf(false) }

    var selectedServices by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current

    val countries = listOf(
        Country("Perú", "🇵🇪"),
        Country("Argentina", "🇦🇷"),
        Country("Brasil", "🇧🇷"),
        Country("Chile", "🇨🇱"),
        Country("Colombia", "🇨🇴"),
        Country("Ecuador", "🇪🇨"),
        Country("México", "🇲🇽"),
        Country("España", "🇪🇸"),
        Country("Estados Unidos", "🇺🇸"),
        Country("Francia", "🇫🇷"),
        Country("Alemania", "🇩🇪"),
        Country("Reino Unido", "🇬🇧")
    )

    val filteredCountries = countries.filter { 
        it.name.contains(nationalitySearch, ignoreCase = true) 
    }

    val priceTypes = listOf("Rango", "Fijo", "Personalizado")
    
    val rangeValues = listOf("0-10", "11-50", "51-100", "101-200", "201-500", "501+")
    val fixedValues = listOf("0", "10", "20", "50", "100", "200", "500")
    val currencies = listOf("S/", "$", "€")

    val availableServices = listOf(
        ServiceOption("Hospedaje", Icons.Default.Bed),
        ServiceOption("Alimentación", Icons.Default.Restaurant),
        ServiceOption("Artesanía", Icons.Default.LocalMall)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp > 600

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Nationality and Expense
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = UiTranslations.getString(context, "add_visit_title", language),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Nationality Selector
                    Text(
                        text = UiTranslations.getString(context, "add_visit_country_label", language) + " *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isNationalityExpanded,
                        onExpandedChange = { isNationalityExpanded = !isNationalityExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedNationality != null && !isNationalityExpanded) 
                                        "${selectedNationality?.flag} ${selectedNationality?.name}" 
                                    else nationalitySearch,
                            onValueChange = { 
                                nationalitySearch = it
                                isNationalityExpanded = true
                            },
                            placeholder = { Text(UiTranslations.getString(context, "select_option", language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNationalityExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isNationalityExpanded,
                            onDismissRequest = { isNationalityExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            filteredCountries.forEach { country ->
                                DropdownMenuItem(
                                    text = { 
                                        Row {
                                            Text(text = country.flag)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = country.name)
                                        }
                                    },
                                    onClick = {
                                        selectedNationality = country
                                        nationalitySearch = ""
                                        isNationalityExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Expense Selector (Refactored)
                    Text(
                        text = UiTranslations.getString(context, "add_visit_price_label", language) + " *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Type Selector
                        ExposedDropdownMenuBox(
                            expanded = isPriceTypeExpanded,
                            onExpandedChange = { isPriceTypeExpanded = !isPriceTypeExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = when(selectedPriceType) {
                                    "Rango" -> UiTranslations.getString(context, "price_type_range", language)
                                    "Fijo" -> UiTranslations.getString(context, "price_type_fixed", language)
                                    "Personalizado" -> UiTranslations.getString(context, "price_type_custom", language)
                                    else -> selectedPriceType
                                },
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceTypeExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isPriceTypeExpanded,
                                onDismissRequest = { isPriceTypeExpanded = false }
                            ) {
                                priceTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(when(type) {
                                                "Rango" -> UiTranslations.getString(context, "price_type_range", language)
                                                "Fijo" -> UiTranslations.getString(context, "price_type_fixed", language)
                                                "Personalizado" -> UiTranslations.getString(context, "price_type_custom", language)
                                                else -> type
                                            }) 
                                        },
                                        onClick = {
                                            selectedPriceType = type
                                            selectedPriceValue = ""
                                            isPriceTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Currency Selector
                        ExposedDropdownMenuBox(
                            expanded = isCurrencyExpanded,
                            onExpandedChange = { isCurrencyExpanded = !isCurrencyExpanded },
                            modifier = Modifier.width(90.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedCurrency,
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isCurrencyExpanded,
                                onDismissRequest = { isCurrencyExpanded = false }
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            selectedCurrency = curr
                                            isCurrencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Value Selector / Input
                    if (selectedPriceType == "Personalizado") {
                        OutlinedTextField(
                            value = selectedPriceValue,
                            onValueChange = { selectedPriceValue = it },
                            placeholder = { Text(UiTranslations.getString(context, "setup_business_name_hint", language)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = isPriceValueExpanded,
                            onExpandedChange = { isPriceValueExpanded = !isPriceValueExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPriceValue,
                                onValueChange = { },
                                readOnly = true,
                                placeholder = { Text(UiTranslations.getString(context, "select_option", language)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceValueExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isPriceValueExpanded,
                                onDismissRequest = { isPriceValueExpanded = false }
                            ) {
                                val values = if (selectedPriceType == "Rango") rangeValues else fixedValues
                                values.forEach { valStr ->
                                    DropdownMenuItem(
                                        text = { Text(valStr) },
                                        onClick = {
                                            selectedPriceValue = valStr
                                            isPriceValueExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Services and Save Button
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Services
                    Text(
                        text = UiTranslations.getString(context, "add_visit_services_label", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ServiceSelectorGrid(
                        services = availableServices.map { it.copy(name = UiTranslations.translateService(it.name, language, context)) },
                        selectedServices = selectedServices.map { UiTranslations.translateService(it, language, context) }.toSet(),
                        onServiceToggle = { serviceNameTranslated ->
                            // Map back to original name
                            val originalName = availableServices.find { UiTranslations.translateService(it.name, language, context) == serviceNameTranslated }?.name ?: serviceNameTranslated
                            if (selectedServices.contains(originalName)) {
                                selectedServices = selectedServices - originalName
                            } else {
                                selectedServices = selectedServices + originalName
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val nationality = selectedNationality?.name ?: ""
                            val flag = selectedNationality?.flag ?: ""
                            val servicesString = selectedServices.joinToString(", ")
                            onSave(nationality, flag, selectedPriceType, selectedPriceValue, selectedCurrency, servicesString)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        enabled = selectedNationality != null && selectedPriceValue.isNotEmpty() && selectedServices.isNotEmpty()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = UiTranslations.getString(context, "add_visit_save", language),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = UiTranslations.getString(context, "add_visit_title", language),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${UiTranslations.getString(context, "record_label", language)} #${visitCount + 1}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Nationality Selector
                Text(
                    text = UiTranslations.getString(context, "add_visit_country_label", language) + " *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isNationalityExpanded,
                    onExpandedChange = { isNationalityExpanded = !isNationalityExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedNationality != null && !isNationalityExpanded) 
                                    "${selectedNationality?.flag} ${selectedNationality?.name}" 
                                else nationalitySearch,
                        onValueChange = { 
                            nationalitySearch = it
                            isNationalityExpanded = true
                        },
                        placeholder = { Text(UiTranslations.getString(context, "select_option", language)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNationalityExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isNationalityExpanded,
                        onDismissRequest = { isNationalityExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        filteredCountries.forEach { country ->
                            DropdownMenuItem(
                                text = { 
                                    Row {
                                        Text(text = country.flag)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = country.name)
                                    }
                                },
                                onClick = {
                                    selectedNationality = country
                                    nationalitySearch = ""
                                    isNationalityExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Expense Selector (Refactored)
                Text(
                    text = UiTranslations.getString(context, "add_visit_price_label", language) + " *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Type Selector
                    ExposedDropdownMenuBox(
                        expanded = isPriceTypeExpanded,
                        onExpandedChange = { isPriceTypeExpanded = !isPriceTypeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = when(selectedPriceType) {
                                "Rango" -> UiTranslations.getString(context, "price_type_range", language)
                                "Fijo" -> UiTranslations.getString(context, "price_type_fixed", language)
                                "Personalizado" -> UiTranslations.getString(context, "price_type_custom", language)
                                else -> selectedPriceType
                            },
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceTypeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isPriceTypeExpanded,
                            onDismissRequest = { isPriceTypeExpanded = false }
                        ) {
                            priceTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(type) {
                                            "Rango" -> UiTranslations.getString(context, "price_type_range", language)
                                            "Fijo" -> UiTranslations.getString(context, "price_type_fixed", language)
                                            "Personalizado" -> UiTranslations.getString(context, "price_type_custom", language)
                                            else -> type
                                        }) 
                                    },
                                    onClick = {
                                        selectedPriceType = type
                                        selectedPriceValue = ""
                                        isPriceTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Currency Selector
                    ExposedDropdownMenuBox(
                        expanded = isCurrencyExpanded,
                        onExpandedChange = { isCurrencyExpanded = !isCurrencyExpanded },
                        modifier = Modifier.width(90.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isCurrencyExpanded,
                            onDismissRequest = { isCurrencyExpanded = false }
                        ) {
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr) },
                                    onClick = {
                                        selectedCurrency = curr
                                        isCurrencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Value Selector / Input
                if (selectedPriceType == "Personalizado") {
                    OutlinedTextField(
                        value = selectedPriceValue,
                        onValueChange = { selectedPriceValue = it },
                        placeholder = { Text(UiTranslations.getString(context, "setup_business_name_hint", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = isPriceValueExpanded,
                        onExpandedChange = { isPriceValueExpanded = !isPriceValueExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPriceValue,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = { Text(UiTranslations.getString(context, "select_option", language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceValueExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isPriceValueExpanded,
                            onDismissRequest = { isPriceValueExpanded = false }
                        ) {
                            val values = if (selectedPriceType == "Rango") rangeValues else fixedValues
                            values.forEach { valStr ->
                                DropdownMenuItem(
                                    text = { Text(valStr) },
                                    onClick = {
                                        selectedPriceValue = valStr
                                        isPriceValueExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Services
                Text(
                    text = UiTranslations.getString(context, "add_visit_services_label", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                ServiceSelectorGrid(
                    services = availableServices.map { it.copy(name = UiTranslations.translateService(it.name, language, context)) },
                    selectedServices = selectedServices.map { UiTranslations.translateService(it, language, context) }.toSet(),
                    onServiceToggle = { serviceNameTranslated ->
                        val originalName = availableServices.find { UiTranslations.translateService(it.name, language, context) == serviceNameTranslated }?.name ?: serviceNameTranslated
                        if (selectedServices.contains(originalName)) {
                            selectedServices = selectedServices - originalName
                        } else {
                            selectedServices = selectedServices + originalName
                        }
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Save Button
                Button(
                    onClick = {
                        val nationality = selectedNationality?.name ?: ""
                        val flag = selectedNationality?.flag ?: ""
                        val servicesString = selectedServices.joinToString(", ")
                        onSave(nationality, flag, selectedPriceType, selectedPriceValue, selectedCurrency, servicesString)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    enabled = selectedNationality != null && selectedPriceValue.isNotEmpty() && selectedServices.isNotEmpty()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = UiTranslations.getString(context, "add_visit_save", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class Country(val name: String, val flag: String)

@Preview(showBackground = true)
@Composable
fun AddVisitPreview() {
    Final_projectTheme {
        AddVisitScreen(visitCount = 11, language = "Español", onBack = {}, onSave = { _, _, _, _, _, _ -> })
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddVisitDarkPreview() {
    Final_projectTheme {
        AddVisitScreen(visitCount = 11, language = "Español", onBack = {}, onSave = { _, _, _, _, _, _ -> })
    }
}

