package dev.cedes.conectarservidor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import androidx.compose.foundation.BorderStroke

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

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                usuarios = obtenerUsuarios()
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
            isLoading = false
        }
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
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Buscar por nombre o correo") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ícono de búsqueda"
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.onSurface
            )
        )

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
                        UsuarioCard(usuario)
                    }
                }
            }
        }
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
                onClick = { /* Acción al hacer clic en la tarjeta */ },
                interactionSource = interactionSource,
                indication = null
            )
            .scale(scale),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = usuario.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
                val json = connection.inputStream.bufferedReader().use { it.readText() }
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

@Composable
@Preview(showBackground = true)
fun PreviewUsuarioCard() {
    ConectarservidorTheme {
        UsuarioCard(usuario = Usuario(1, "Carlos Sedeño", "carlos@example.com"))
    }
}