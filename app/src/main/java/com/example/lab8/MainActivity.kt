@file:Suppress("UNREACHABLE_CODE", "CAST_NEVER_SUCCEEDS")

package com.example.lab8

import WeatherResponse
import android.app.Activity
import android.os.Bundle
import android.util.Log
//import android.util.Size
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
//import fetchWeather
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random
//import fetchWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

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

    var playerX by remember { mutableStateOf(200f) }
    var playerY by remember { mutableStateOf(screenHeightPx / 2) }
    var xTilt by tiltSensor.xTilt
    var a by remember { mutableStateOf(0f) }
    var v by remember { mutableStateOf(0f) }

    var jumpV by remember { mutableStateOf(0f) }
    var jumpA by remember { mutableStateOf(270f) }
    val g by remember { mutableStateOf(9.8f) }
    var yCord = screenHeightPx / 2 + 60f
    var curPlatf by remember { mutableStateOf(yCord - 600f) }
    var record by remember { mutableStateOf(0) }

    var isGamePaused by remember { mutableStateOf(false) }

    var weatherResponse by remember { mutableStateOf<WeatherResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var color by remember { mutableStateOf(Color(57, 112, 196, 255)) }
    var showAddHighScoreDialog by remember { mutableStateOf(true) }


    fun addRandomPlatform(y: Float) {
        var randomX = Random.nextFloat() * screenWidthPx
        randomX = randomX.coerceIn(0f, screenWidthPx - 200f)
        platforms.addLast(Pair(randomX, y))
    }

    fun removeButtonPlatform() {
        if (platforms.isNotEmpty()) {
            platforms.removeFirst()
        } else {
            println("Стек пуст.")
        }
    }

    LaunchedEffect(Unit) {
        platforms.clear()
        curPlatf = yCord - 600f

        try {
            val result = fetchWeather("Minsk", "35f8f1c7f56f0fdf44f56c489b1dd616")
            weatherResponse = result
            isLoading = false
            color = if ((result?.main?.temp?.minus(273.15)?.toInt() ?: 0) > 15) {
                Color.Yellow
            } else {
                Color.Blue
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
        (abs(playerX - platforms.elementAt(0).first - 75f) <= 75f
                && abs(yCord - platforms.elementAt(0).second - playerY) <= 20f
                && jumpA < 0)
        ||
        (abs(playerX - platforms.elementAt(1).first - 75f) <= 75f
                && abs(yCord - platforms.elementAt(1).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(2).first - 75f) <= 75f
                && abs(yCord - platforms.elementAt(2).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(3).first - 75f) <= 75f
                && abs(yCord - platforms.elementAt(3).second - playerY) <= 20f
                && jumpA < 0)
        || (abs(playerX - platforms.elementAt(4).first - 75f) <= 75f
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
            .background(color)
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
            drawRect(color = Color(69, 182, 4, 255),
                topLeft = Offset(x = 0f, y = yCord))
            drawCircle(color = Color.Cyan,
                radius = 50f,
                center = androidx.compose.ui.geometry.Offset(playerX - screenWidthPx, playerY))
            drawCircle(color = Color.Cyan,
                radius = 50f,
                center = androidx.compose.ui.geometry.Offset(playerX + screenWidthPx, playerY))
            drawCircle(
                color = Color.Cyan,
                radius = 50f,
                center = androidx.compose.ui.geometry.Offset(playerX, playerY)
            )
            for (i in 0..4) {
                drawRect(color = Color.White,
                    topLeft = Offset(
                        platforms.elementAt(i).first,
                        yCord - platforms.elementAt(i).second),
                    size = androidx.compose.ui.geometry.Size(width = 150f, height = 30f))
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
                Button(
                    onClick = {
                        val records = dbHelper.getTop5Records()
                        if (records.size == 5) {
                            dbHelper.removeLowestRecord()
                        }
                        dbHelper.insertRecord(score, name.text)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Save")
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
