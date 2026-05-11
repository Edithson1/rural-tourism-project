package upch.mluque.final_project.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.ui.components.ServiceOption
import upch.mluque.final_project.ui.components.ServiceSelectorGrid
import upch.mluque.final_project.ui.theme.DarkGreenText
import upch.mluque.final_project.ui.theme.BrownPrimary
import upch.mluque.final_project.ui.theme.Final_projectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitScreen(
    visitCount: Int,
    onBack: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var nationalitySearch by remember { mutableStateOf("") }
    var selectedNationality by remember { mutableStateOf<Country?>(null) }
    var isNationalityExpanded by remember { mutableStateOf(false) }

    var selectedRange by remember { mutableStateOf("") }
    var isRangeExpanded by remember { mutableStateOf(false) }

    var selectedServices by remember { mutableStateOf(setOf<String>()) }

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

    val ranges = listOf(
        "S/ 0 - S/ 50",
        "S/ 51 - S/ 100",
        "S/ 101 - S/ 200",
        "S/ 201 - S/ 500",
        "S/ 500+"
    )

    val availableServices = listOf(
        ServiceOption("Hospedaje", Icons.Default.Bed),
        ServiceOption("Alimentación", Icons.Default.Restaurant),
        ServiceOption("Artesanía", Icons.Default.LocalMall) // LocalMall is closer to shopping bag
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
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
                    text = "Nuevo Registro",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    color = if (isSystemInDarkTheme()) Color.DarkGray else Color(0xFFF0F0F0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Registro #${visitCount + 1}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nationality Selector
            Text(
                text = "Procedencia / Nacionalidad *",
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
                    placeholder = { Text("Seleccione una opción") },
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

            // Expense Selector
            Text(
                text = "Gasto aproximado *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isRangeExpanded,
                onExpandedChange = { isRangeExpanded = !isRangeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRange,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text("Seleccione rango") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRangeExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isRangeExpanded,
                    onDismissRequest = { isRangeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    ranges.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(text = range) },
                            onClick = {
                                selectedRange = range
                                isRangeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Services
            Text(
                text = "Servicios consumidos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            ServiceSelectorGrid(
                services = availableServices,
                selectedServices = selectedServices,
                onServiceToggle = { serviceName ->
                    if (selectedServices.contains(serviceName)) {
                        selectedServices = selectedServices - serviceName
                    } else {
                        selectedServices = selectedServices + serviceName
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
                    onSave(nationality, flag, selectedRange, servicesString)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrownPrimary,
                    disabledContainerColor = BrownPrimary.copy(alpha = 0.5f)
                ),
                enabled = selectedNationality != null && selectedRange.isNotEmpty() && selectedServices.isNotEmpty()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Registro",
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

data class Country(val name: String, val flag: String)

@Preview(showBackground = true)
@Composable
fun AddVisitPreview() {
    Final_projectTheme {
        AddVisitScreen(visitCount = 11, onBack = {}, onSave = { _, _, _, _ -> })
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddVisitDarkPreview() {
    Final_projectTheme {
        AddVisitScreen(visitCount = 11, onBack = {}, onSave = { _, _, _, _ -> })
    }
}
