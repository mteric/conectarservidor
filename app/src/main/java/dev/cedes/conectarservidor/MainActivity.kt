package dev.cedes.conectarservidor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.cedes.conectarservidor.ui.theme.ConectarservidorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.random.Random

// Modelo de datos que coincide con tu JSON
data class Usuario(
    val id_usuario: Int,
    val nombre: String,
    val email: String
)

// Clase que representa una estrella para el fondo
data class Star(
    var x: Float,
    var y: Float,
    var size: Float,
    var speed: Float
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConectarservidorTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Lista de Usuarios") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        StarryBackground()
                        ListaUsuariosScreen(Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

// Composable para el fondo de pantalla estrellado
@Composable
fun StarryBackground() {
    val numStars = 200
    val stars = remember {
        List(numStars) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2 + 1,
                speed = Random.nextFloat() * 10 + 5
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "star_transition")
    val starYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "star_y_offset_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D0D2A), Color(0xFF1B263B))
            )
        )

        stars.forEach { star ->
            star.y += starYOffset * star.speed / size.height
            if (star.y > 1f) {
                star.y = 0f
                star.x = Random.nextFloat()
                star.size = Random.nextFloat() * 2 + 1
            }

            drawCircle(
                color = Color.White,
                radius = star.size,
                center = Offset(size.width * star.x, size.height * star.y)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaUsuariosScreen(modifier: Modifier = Modifier) {
    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val refreshData: () -> Unit = {
        isLoading = true
        scope.launch {
            try {
                usuarios = obtenerUsuarios()
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val filteredUsuarios = remember(usuarios, searchText) {
        if (searchText.isEmpty()) {
            usuarios
        } else {
            usuarios.filter {
                it.nombre.contains(searchText, ignoreCase = true) ||
                        it.email.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar por nombre o correo", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ícono de búsqueda",
                        tint = Color.Black
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                )
            )
            IconButton(
                onClick = refreshData,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recargar lista",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar usuario",
                    tint = Color.White
                )
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage ?: "Error desconocido", color = Color.White)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredUsuarios) { usuario ->
                        UsuarioCard(usuario = usuario)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismissRequest = { showAddDialog = false },
            onAddUser = { nombre, email ->
                scope.launch {
                    try {
                        insertarUsuario(nombre, email)
                        showAddDialog = false
                        refreshData()
                    } catch (e: Exception) {
                        errorMessage = "Error al insertar: ${e.message}"
                    }
                }
            }
        )
    }
}

@Composable
fun UsuarioCard(usuario: Usuario) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "cardScaleAnimation")

    val cardBackgroundColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        label = "cardBackgroundColorAnimation"
    )

    val cardBorder = if (isPressed) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                onClick = { },
                interactionSource = interactionSource,
                indication = null
            )
            .scale(scale),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Ícono de usuario",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = usuario.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Ícono de correo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = usuario.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    onDismissRequest: () -> Unit,
    onAddUser: (nombre: String, email: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Agregar Nuevo Usuario") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank() && email.isNotBlank()) {
                        onAddUser(nombre, email)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}

suspend fun obtenerUsuarios(): List<Usuario> {
    return withContext(Dispatchers.IO) {
        val url = URL("http://200.57.149.25:8079/pruebas/web_service_usr.php")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val json = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val listType = object : TypeToken<List<Usuario>>() {}.type
                Gson().fromJson(json, listType)
            } else {
                emptyList()
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun insertarUsuario(nombre: String, email: String) {
    return withContext(Dispatchers.IO) {
        val url = URL("http://200.57.149.25:8079/pruebas/insertar_usuario.php")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonInputString = "{\"nombre\": \"$nombre\", \"email\": \"$email\"}"

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonInputString)
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                println("Respuesta del servidor: $response")
            } else {
                val error = connection.errorStream.bufferedReader().use { it.readText() }
                println("Error del servidor: $error")
                throw Exception("Error del servidor: ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}