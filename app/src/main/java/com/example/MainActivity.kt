package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0C)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Snowfall / Particle Background Animation
                        SnowfallEffect()

                        DashboardScreen(
                            onStartGame = { packageName, iconSize, iconOpacity, cornerRadius ->
                                checkAndStartOverlay(packageName, iconSize, iconOpacity, cornerRadius)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndStartOverlay(packageName: String, iconSize: Float, iconOpacity: Float, cornerRadius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Lütfen 'Diğer uygulamalar üzerinde göster' iznini verin!", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            launchModMenuService(packageName, iconSize, iconOpacity, cornerRadius)
        }
    }

    private fun launchModMenuService(packageName: String, iconSize: Float, iconOpacity: Float, cornerRadius: Float) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Hedef oyun ($packageName) başlatılıyor...", Toast.LENGTH_SHORT).show()
        }

        val serviceIntent = Intent(this, FloatingModMenuService::class.java).apply {
            putExtra("TARGET_PACKAGE", packageName)
            putExtra("ICON_SIZE", iconSize)
            putExtra("ICON_OPACITY", iconOpacity)
            putExtra("CORNER_RADIUS", cornerRadius)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Anonymous Dumper Aktif!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "İzin verildi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay izni reddedildi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class Snowflake(var x: Float, var y: Float, val speed: Float, val radius: Float, val alpha: Float)

@Composable
fun SnowfallEffect() {
    val snowflakes = remember {
        List(60) {
            Snowflake(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * 2000f,
                speed = Random.nextFloat() * 2f + 1f,
                radius = Random.nextFloat() * 3f + 1.5f,
                alpha = Random.nextFloat() * 0.5f + 0.3f
            )
        }
    }

    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                tick = time
                snowflakes.forEach { flake ->
                    flake.y += flake.speed
                    if (flake.y > 2200f) {
                        flake.y = -50f
                        flake.x = Random.nextFloat() * 1000f
                    }
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        snowflakes.forEach { flake ->
            val drawX = (flake.x / 1000f) * width
            val drawY = (flake.y / 2000f) * height
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha),
                radius = flake.radius,
                center = Offset(drawX, drawY)
            )
        }
    }
}

data class GameTarget(val name: String, val packageName: String, val engine: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onStartGame: (String, Float, Float, Float) -> Unit) {
    val context = LocalContext.current
    val games = listOf(
        GameTarget("Garena Free Fire", "com.dts.freefireth", "IL2CPP / arm64"),
        GameTarget("PUBG Mobile", "com.tencent.ig", "Unreal Engine 4"),
        GameTarget("Standoff 2", "com.axlebolt.standoff2", "Unity IL2CPP"),
        GameTarget("Mobile Legends", "com.mobile.legends", "Unity Engine"),
        GameTarget("Subway Surfers", "com.kiloo.subwaysurf", "Unity IL2CPP")
    )

    var selectedGame by remember { mutableStateOf<GameTarget?>(null) }
    var customPackageInput by remember { mutableStateOf("") }
    
    var iconSize by remember { mutableStateOf(56f) }
    var iconOpacity by remember { mutableStateOf(100f) }
    var cornerRadius by remember { mutableStateOf(12f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Text(
                text = "Anonymous Master Dumper",
                color = Color(0xFF00FF00),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Pro", tint = Color(0xFFFFD700))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 0: USAGE & ROOT GUIDE (Nasıl Kullanılır & Root Gereksinimi)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1A1A22)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FF00))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📌 NASIL KULLANILIR & ROOT DURUMU",
                            color = Color(0xFF00FF00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Hedef Oyunu Seçin: Listeden oyun seçin veya paket adını yazın.\n" +
                                   "2. Başlat Butonuna Basın: Yüzen menü ekranda aktifleşir.\n" +
                                   "3. Dumper'ı Çalıştırın: Yüzen simgeye tıklayıp [ID: 9] START DUMPER'a basın.\n" +
                                   "4. Root Gereksinimi: Uygulama arayüzü ve /sdcard/Download/AnonymousDumper/offset.txt dosyasına export işlemi **ROOTSUZ** da çalışır. Ancak canlı başka oyun belleği okumak için **Root** gerekir.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Section 1: Target App Selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. HEDEF OYUN SEÇİMİ",
                        color = if (selectedGame == null && customPackageInput.isBlank()) Color(0xFFFF6B6B) else Color(0xFF00FF00),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedGame != null) {
                        TextButton(onClick = { selectedGame = null }) {
                            Text(text = "Temizle", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1E24)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        games.forEach { game ->
                            val isSelected = selectedGame?.packageName == game.packageName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF003300) else Color(0xFF2C2C35),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00FF00) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedGame = game
                                        customPackageInput = ""
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = game.name,
                                        color = if (isSelected) Color(0xFF00FF00) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = game.packageName,
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF00FF00))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customPackageInput,
                            onValueChange = {
                                customPackageInput = it
                                if (it.isNotBlank()) selectedGame = null
                            },
                            placeholder = { Text("Veya manuel paket adı girin (örn. com.game)", color = Color.DarkGray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF00),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            // Section 2: Floating Icon Customization Sliders
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1E24)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "2. YÜZEN SİMGE AYARLARI",
                            color = Color(0xFF00FF00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Boyut: ${iconSize.toInt()} dp", color = Color.LightGray, fontSize = 11.sp)
                        Slider(
                            value = iconSize,
                            onValueChange = { iconSize = it },
                            valueRange = 40f..90f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF00), activeTrackColor = Color(0xFF00FF00))
                        )

                        Text(text = "Opaklık: ${iconOpacity.toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                        Slider(
                            value = iconOpacity,
                            onValueChange = { iconOpacity = it },
                            valueRange = 20f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF00), activeTrackColor = Color(0xFF00FF00))
                        )

                        Text(text = "Köşe Yuvarlama: ${cornerRadius.toInt()} dp", color = Color.LightGray, fontSize = 11.sp)
                        Slider(
                            value = cornerRadius,
                            onValueChange = { cornerRadius = it },
                            valueRange = 0f..30f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF00), activeTrackColor = Color(0xFF00FF00))
                        )
                    }
                }
            }

            // Section 3: Master Dumper Module Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE2A1515)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF4D4D))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "3. KAPSAMLI MASTER DUMPER ENGINE",
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Can, zıplama, hız, koordinatlar, silahlar, mermi, recoil, ESP kemikleri, kamera FOV ve binlerce IL2CPP sınıf/metot offseti kategorize edilmiş olarak /sdcard/Download/AnonymousDumper/offset.txt dosyasına yazılır.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val activePackage = when {
            selectedGame != null -> selectedGame!!.packageName
            customPackageInput.isNotBlank() -> customPackageInput.trim()
            else -> ""
        }

        val isReady = activePackage.isNotBlank()

        // Bottom Action Button
        Button(
            onClick = {
                if (isReady) {
                    onStartGame(activePackage, iconSize, iconOpacity, cornerRadius)
                } else {
                    Toast.makeText(context, "Lütfen önce bir hedef oyun seçin veya paket adı girin!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReady) Color(0xFF00FF00) else Color.DarkGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isReady) "BAŞLAT & MASTER DUMP ET" else "LÜTFEN HEDEF OYUN SEÇİN",
                color = if (isReady) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
