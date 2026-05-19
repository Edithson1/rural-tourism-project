package upch.mluque.final_project.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.max
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.ServiceOption
import upch.mluque.final_project.ui.components.ServiceSelectorGrid
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    
    var businessName by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var editingUri by remember { mutableStateOf<Uri?>(null) }
    
    // Estado para la imagen pendiente de guardar
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val context = LocalContext.current

    LaunchedEffect(settings) {
        if (!initialized && settings != null) {
            businessName = settings?.businessName ?: ""
            selectedService = settings?.businessCategory ?: ""
            initialized = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { editingUri = it }
    }

    val services = listOf(
        ServiceOption("Hospedaje", Icons.Default.Bed),
        ServiceOption("Alimentación", Icons.Default.Restaurant),
        ServiceOption("Artesanía", Icons.Default.Checkroom),
        ServiceOption("Varios", Icons.Default.Storefront)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                // Priorizar previsualización local
                if (pendingBitmap != null) {
                    AsyncImage(
                        model = pendingBitmap,
                        contentDescription = "Previsualización",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (settings?.profilePicture != null) {
                    AsyncImage(
                        model = settings?.profilePicture,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Overlay icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nombre del emprendimiento",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sector o Rubro",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            ServiceSelectorGrid(
                services = services,
                selectedServices = if (selectedService.isEmpty()) emptySet() else setOf(selectedService),
                onServiceToggle = { selectedService = it }
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val byteArray = pendingBitmap?.let { bitmap ->
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.toByteArray()
                    }
                    viewModel.saveProfile(businessName, selectedService, byteArray)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = businessName.isNotBlank() && selectedService.isNotBlank()
            ) {
                Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        editingUri?.let { uri ->
            ImageEditorDialog(
                uri = uri,
                onDismiss = { editingUri = null },
                onConfirm = { bitmap ->
                    // Solo actualizamos el estado local para previsualización
                    pendingBitmap = bitmap
                    editingUri = null
                }
            )
        }

        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Cambiar foto de perfil") },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Galería") },
                            leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                            modifier = Modifier.clickable {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                showImageSourceDialog = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Cámara") },
                            leadingContent = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showImageSourceDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("CANCELAR")
                    }
                }
            )
        }
    }
}

@Composable
fun ImageEditorDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it)
        }
    }

    if (bitmap == null) {
        onDismiss()
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val cropSizePx = with(LocalDensity.current) { 200.dp.toPx() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val density = LocalDensity.current.density

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offset += pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                        val imageWidth = bitmap.width.toFloat()
                        val imageHeight = bitmap.height.toFloat()
                        val baseScale = (size.minDimension / max(imageWidth, imageHeight))
                        val finalScale = baseScale * scale
                            
                            translate(offset.x, offset.y) {
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    dstOffset = Offset(
                                        (size.width - imageWidth * finalScale) / 2,
                                        (size.height - imageHeight * finalScale) / 2
                                    ).toIntOffset(),
                                    dstSize = Size(imageWidth * finalScale, imageHeight * finalScale).toIntSize(),
                                    filterQuality = FilterQuality.High
                                )
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = cropSizePx / 2
                            val rectPath = Path().apply {
                                addRect(
                                    Rect(
                                        center.x - radius,
                                        center.y - radius,
                                        center.x + radius,
                                        center.y + radius
                                    )
                                )
                            }
                            clipPath(rectPath, clipOp = ClipOp.Difference) {
                                drawRect(Color.Black.copy(alpha = 0.7f))
                            }
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(cropSizePx, cropSizePx),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Text("CANCELAR")
                        }
                        Button(
                            onClick = {
                                val result = cropBitmap(bitmap, scale, offset, cropSizePx, Size(canvasWidth, canvasHeight - 100 * density))
                                onConfirm(result)
                            },
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("CONFIRMAR")
                        }
                    }
                }
            }
        }
    }
}

fun cropBitmap(source: Bitmap, scale: Float, offset: Offset, cropSizePx: Float, canvasSize: Size): Bitmap {
    val imageWidth = source.width.toFloat()
    val imageHeight = source.height.toFloat()
    val baseScale = canvasSize.minDimension / max(imageWidth, imageHeight)
    val finalScale = baseScale * scale

    val centerX = (imageWidth / 2f) - (offset.x / finalScale)
    val centerY = (imageHeight / 2f) - (offset.y / finalScale)
    
    val sizeInBitmap = cropSizePx / finalScale
    
    val left = (centerX - sizeInBitmap / 2f).toInt().coerceIn(0, source.width)
    val top = (centerY - sizeInBitmap / 2f).toInt().coerceIn(0, source.height)
    val width = sizeInBitmap.toInt().coerceAtMost(source.width - left)
    val height = sizeInBitmap.toInt().coerceAtMost(source.height - top)
    
    if (width <= 0 || height <= 0) return source

    val cropped = Bitmap.createBitmap(source, left, top, width, height)
    
    return if (width > 128 || height > 128) {
        Bitmap.createScaledBitmap(cropped, 128, 128, true)
    } else {
        cropped
    }
}

fun Offset.toIntOffset() = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
fun Size.toIntSize() = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
