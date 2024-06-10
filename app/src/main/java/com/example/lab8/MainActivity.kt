package com.example.lab8

import WeatherResponse
import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.lab8.ui.theme.SimpleGameTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val platforms = ArrayDeque<Pair<Float, Float>>()
var jumpAp = 0f

@Composable
fun GameScreen(tiltSensor: TiltSensor,
               onPause: () -> Unit,
               onGameOver: () -> Unit,
               dbHelper: RecordDatabaseHelper) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics

    val screenWidthPx = configuration.screenWidthDp * displayMetrics.density
    val screenHeightPx = configuration.screenHeightDp * displayMetrics.density

    var playerX by remember { mutableFloatStateOf(200f) }
    var playerY by remember { mutableFloatStateOf(screenHeightPx / 2) }
    var xTilt by tiltSensor.xTilt
    var a by remember { mutableFloatStateOf(0f) }
    var v by remember { mutableFloatStateOf(0f) }

    var jumpV by remember { mutableFloatStateOf(0f) }
    var jumpA by remember { mutableFloatStateOf(270f) }
    val g by remember { mutableFloatStateOf(9.8f) }
    var yCord = screenHeightPx / 2 + 60f
    var curPlatf by remember { mutableFloatStateOf(yCord - 600f) }
    var record by remember { mutableIntStateOf(0) }

    var isGamePaused by remember { mutableStateOf(false) }

    var weatherResponse by remember { mutableStateOf<WeatherResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var color by remember { mutableStateOf(Color(57, 112, 196, 255)) }
    val backgroundWarmPainter: Painter = painterResource(id = R.drawable.background_warm)
    val backgroundColdPainter: Painter = painterResource(id = R.drawable.background_clod)
    var showAddHighScoreDialog by remember { mutableStateOf(true) }
    var isRainy by remember { mutableStateOf(false) }
    var isWarm by remember { mutableStateOf(true) }


    fun addRandomPlatform(y: Float) {
        var randomX = Random.nextFloat() * screenWidthPx
        randomX = randomX.coerceIn(0f, screenWidthPx - 200f)
        platforms.addLast(Pair(randomX, y))
    }

    fun removeButtonPlatform() {
        if (platforms.isNotEmpty()) {
            platforms.removeFirst()
        } else {
            println("Stack is empty")
        }
    }

    LaunchedEffect(Unit) {
        platforms.clear()
        curPlatf = yCord - 600f

        try {
            val result = fetchWeather("Minsk", "35f8f1c7f56f0fdf44f56c489b1dd616")
            weatherResponse = result
            isLoading = false
            isWarm = (result?.main?.temp?.minus(273.15)?.toInt() ?: 0) > 15
            if (result?.main?.humidity!! > 70) {
                isRainy = true
            }
        } catch (e: Exception) {
            Log.e("GameScreen", "Error fetching weather", e)
        }
    }

    while (platforms.size < 5) {
        addRandomPlatform(curPlatf)
        curPlatf += 600f
    }

    jumpV += jumpA * 0.1f
    yCord += jumpV

    record = max(record, ((yCord - 300) / 600).toInt() - 2)

    if (yCord < (screenHeightPx / 2 + 60f) ||
        (abs(playerX - platforms.elementAt(0).first - 80f) <= 130f
                && abs(yCord - platforms.elementAt(0).second - playerY) <= 20f
                && jumpA < 0)
        ||
        (abs(playerX - platforms.elementAt(1).first - 80f) <= 130f
                && abs(yCord - platforms.elementAt(1).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(2).first - 80f) <= 130f
                && abs(yCord - platforms.elementAt(2).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(3).first - 80f) <= 130f
                && abs(yCord - platforms.elementAt(3).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(4).first - 80f) <= 130f
                && abs(yCord - platforms.elementAt(4).second - playerY) <= 20f
                && jumpA < 0)
    ){
        jumpA = 270f
    } else {
        jumpA -= g * 0.5f
    }


    if (isGamePaused) {
        xTilt = 0f
        jumpA = 0f
    }

//    a = xTilt / 2
//    v += a
//
//    playerX -= v - a / 2
    playerX -= xTilt * 5

    v = v.coerceIn(-50f, 50f)

    if (playerX >= 1080f) {
        playerX -= 1080f
    }
    if(playerX <= 0) {
        playerX += 1080f
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
//            .background(color)
            .paint(
                painter = if(isWarm) {backgroundWarmPainter} else {backgroundColdPainter},
                contentScale = ContentScale.FillHeight)
    ) {
        Text(text = "$record",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        )

        if (playerY < yCord - platforms.elementAt(3).second) {
            removeButtonPlatform()
            addRandomPlatform(curPlatf)
            curPlatf += 600f
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(129, 192, 93, 255),
                topLeft = Offset(x = 0f, y = yCord - 10))
            drawCircle(color = Color.Cyan,
                radius = 50f,
                center = Offset(playerX - screenWidthPx, playerY))
            drawCircle(color = Color.Cyan,
                radius = 50f,
                center = Offset(playerX + screenWidthPx, playerY))
            drawCircle(
                color = Color.Cyan,
                radius = 50f,
                center = Offset(playerX, playerY)
            )
            for (i in 0..4) {

                val cloudBitmap = BitmapFactory.decodeResource(context.resources,
                    R.drawable.cloud).asImageBitmap()
                val rainBitmap = BitmapFactory.decodeResource(context.resources,
                    R.drawable.rain).asImageBitmap()

                if(isRainy) {
                    drawImage(
                        image = rainBitmap,
                        topLeft = Offset(
                            platforms.elementAt(i).first,
                            yCord - platforms.elementAt(i).second
                        )
                    )
                } else {
                    drawImage(
                        image = cloudBitmap,
                        topLeft = Offset(
                            platforms.elementAt(i).first,
                            yCord - platforms.elementAt(i).second
                        )
                    )
                }
            }
        }
        Canvas(modifier = Modifier
            .size(50.dp, 50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        Log.d("Pause", "$isGamePaused")
                        if (isGamePaused) {
                            jumpA = jumpAp
                        } else {
                            jumpAp = jumpA
                        }
                        isGamePaused = !isGamePaused
                    }
                )
            }
        ) {
            if (!isGamePaused) {
                drawRect(
                    color = Color.DarkGray,
                    topLeft = Offset(20f, 20f),
                    size = Size(width = 30f, height = 90f)
                )
                drawRect(
                    color = Color.DarkGray,
                    topLeft = Offset(60f, 20f),
                    size = Size(width = 30f, height = 90f)
                )
            } else {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val x = 20f
                    val y = 20f
                    val width = 70f
                    val height = 90f
                    moveTo(x, y)
                    lineTo(x + width, y + height / 2)
                    lineTo(x, y + height)
                    lineTo(x, y)
                    close()
                }
                drawPath(path, Color.DarkGray)
            }
        }

        if (((record + 4) * 600f) - yCord > 2500) {
            if(yCord - playerY < 80) {
                jumpA = 0f;
            }

            if (showAddHighScoreDialog) {
                val records by remember { mutableStateOf(dbHelper.getTop5Records()) }
                showAddHighScoreDialog = false
                if (records.isEmpty() || record > records.first().first) {
                    showAddHighScoreDialog = true
                }

                if (showAddHighScoreDialog) {
                    AddHighScoreDialog(
                        score = record,
                        dbHelper = dbHelper,
                        onDismiss = { showAddHighScoreDialog = false })
                }
            }



            Column(modifier = Modifier
                .align(Alignment.Center)) {
                Text(text = "You lose",
                    fontSize = 50.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Button(onClick = {
                    jumpA = 200f
                    jumpV = 0f
                    yCord = screenHeightPx / 2 + 60f
                    curPlatf = yCord - 600f
                    platforms.clear()
                    record = 0
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = "Play again", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Button(onClick = { onGameOver() },
                       modifier = Modifier
                           .align(Alignment.CenterHorizontally)) {
                    Text(text = "Exit")
                }
            }
        }

        if (isGamePaused) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color(75, 75, 75, 141))) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Button(onClick = {
                        isGamePaused = !isGamePaused
                        jumpA = jumpAp },
                           modifier = Modifier.width(screenWidthPx.dp / 4)
                    ) {
                        Text(text = "Continue")
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Button(onClick = {
                        isGamePaused = !isGamePaused
                        jumpA = 200f
                        jumpV = 0f
                        yCord = screenHeightPx / 2 + 60f
                        curPlatf = yCord - 600f
                        platforms.clear()
                        record = 0
                    }, modifier = Modifier.width(screenWidthPx.dp / 4)) {
                        Text(text = "Restart")
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Button(onClick = { onGameOver() },
                        modifier = Modifier.width(screenWidthPx.dp / 4)) {
                        Text(text = "Exit")
                    }
                }
            }
        }
    }
}

suspend fun fetchWeather(city: String, apiKey: String): WeatherResponse? {
    return withContext(Dispatchers.IO) {
        try {
            RetrofitInstance.api.getWeather(city, apiKey)
        } catch (e: Exception) {
            Log.e("Weather", "Error fetching weather", e)
            null
        }
    }
}

@Composable
fun GameMenu(onStart: () -> Unit, onShowScores: () -> Unit) {
    val activity = LocalContext.current as? Activity
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics

    val screenWidthPx = configuration.screenWidthDp * displayMetrics.density
    val screenHeightPx = configuration.screenHeightDp * displayMetrics.density

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.align(Alignment.Center)){
            Button(onClick = onStart, modifier = Modifier.width(screenWidthPx.dp / 4)) {
                Text(text = "Start Game")
            }
            Button(onClick = onShowScores, modifier = Modifier.width(screenWidthPx.dp / 4)) {
                Text(text = "Records")
            }
            Button(onClick = {
                activity?.finish()
            }, modifier = Modifier.width(screenWidthPx.dp / 4)) {
                Text(text = "Exit")
            }
        }
    }
}

@Composable
fun PauseMenu(onResume: () -> Unit, onQuit: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onResume, modifier = Modifier.padding(16.dp)) {
                Text("Resume")
            }
            Button(onClick = onQuit, modifier = Modifier.padding(16.dp)) {
                Text("Quit to Menu")
            }
        }
    }
}

@Composable
fun HighScoreDialog(dbHelper: RecordDatabaseHelper, onDismiss: () -> Unit) {
    val highScores by remember { mutableStateOf(dbHelper.getTop5Records()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("High Scores", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                highScores.forEachIndexed { index, score ->
                    Text("${index + 1}. ${score.second}: ${score.first}", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row (modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(3.dp)
                            .weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                                    while (dbHelper.getTop5Records().isNotEmpty()) {
                                        dbHelper.removeLowestRecord()
                                    }
                                    onDismiss()
                                  },
                        modifier = Modifier
                            .padding(3.dp)
                            .weight(1f)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
fun AddHighScoreDialog(score: Int, dbHelper: RecordDatabaseHelper, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue("")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("New High Score!", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Score: $score", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enter your name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row (modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Button(
                        onClick = {
                            val records = dbHelper.getTop5Records()
                            if (records.size == 5) {
                                dbHelper.removeLowestRecord()
                            }
                            dbHelper.insertRecord(score, name.text)
                            onDismiss()
                        },
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(text = "Save")
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(2f)) {
                        Text(text = "Cancel")
                    }
                }
            }
        }
    }
}


class MainActivity : ComponentActivity() {
    private lateinit var tiltSensor: TiltSensor
    private lateinit var dbHelper: RecordDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = RecordDatabaseHelper(this)
        tiltSensor = TiltSensor(this)
        setContent {
            SimpleGameTheme {
                var gameState by remember { mutableStateOf(GameState.MENU) }
                var showHighScores by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (gameState) {
                        GameState.MENU -> GameMenu(onStart = { gameState = GameState.PLAYING },
                                                   onShowScores = { showHighScores = true })
                        GameState.PLAYING -> GameScreen(
                            tiltSensor = tiltSensor,
                            onPause = { gameState = GameState.PAUSED },
                            onGameOver = { gameState = GameState.MENU },
                            dbHelper = dbHelper
                        )
                        GameState.PAUSED -> PauseMenu(
                            onResume = { gameState = GameState.PLAYING },
                            onQuit = { gameState = GameState.MENU }
                        )

                    }
                    if (showHighScores) {
                        HighScoreDialog(dbHelper = dbHelper, onDismiss = { showHighScores = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tiltSensor.start()
    }

    override fun onPause() {
        super.onPause()
        tiltSensor.stop()
    }
}
