package com.example.flappy

import android.annotation.SuppressLint
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.flappy.R
import kotlinx.coroutines.delay

data class Pipe(val x: Int, val y: Int, val width: Int, val height: Int)
@SuppressLint("RememberReturnType")
@Composable
fun GameWithSplashScreen() {
    var isGameStarted by remember { mutableStateOf(false) }
    if (!isGameStarted) {
        SplashScreen(onStartGame = { isGameStarted = true })
    } else {
        Game()
    }
}
@Composable
fun SplashScreen(onStartGame: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tap to Start",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
            color = Color.White
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onStartGame() }
                }
        )
    }
}
@Composable
fun Game() {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            .build()
    }
    val jumpSoundId = remember {
        soundPool.load(context, R.raw.flappy_jump, 1)
    }
    val gameOverId = remember {
        soundPool.load(context, R.raw.game_over, 1)
    }
    val coinId = remember {
        soundPool.load(context, R.raw.coin, 1)
    }
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    val screenHeight = config.screenHeightDp
    val bgNormal = R.drawable.bg
    val bgSpecial = R.drawable.bg2
    val birdFramesRight = listOf(R.drawable.bird1, R.drawable.bird2, R.drawable.bird3)
    val birdFramesLeft = listOf(R.drawable.bird1l, R.drawable.bird2l, R.drawable.bird3l)
    val birdFramesRightNew = listOf(R.drawable.bird1lll, R.drawable.bird2lll, R.drawable.bird2lll)
    val birdFramesLeftNew = listOf(R.drawable.bird1ll, R.drawable.bird2ll, R.drawable.bird3ll)
    val pipeWidth = 60
    val pipeHeight = 350
    val birdSize = 60
    var birdFrame by remember { mutableStateOf(0) }
    var gameRunning by remember { mutableStateOf(true) }
    var bird_x by remember { mutableIntStateOf(screenWidth / 2) }
    var bird_y by remember { mutableStateOf(screenHeight / 2) }
    var velocityY by remember { mutableStateOf(0f) }
    var directionX by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var highScore by remember { mutableStateOf(0) }
    val pipesList = remember { mutableStateListOf<Pipe>() }
    fun resetGame() {
        bird_x = screenWidth / 2
        bird_y = screenHeight / 2
        velocityY = 0f
        directionX = 1
        birdFrame = 0
        gameRunning = true
        score = 0
        showDialog = false
        pipesList.clear()

    }
    fun generatePipes(): List<Pipe> {
        val pipeX = if (directionX == -1) 0 else screenWidth - pipeWidth
        val minGap = when {
            score >= 20 -> 90
            score >= 10 -> 110
            else -> 140
        }
        val minY = 50
        val maxY = screenHeight - pipeHeight - 50
        val minTubes = 2
        val maxTubes = when {
            score >= 40 -> 6
            score >= 20 -> 5
            else -> 3
        }
        val numberOfPipes = (minTubes..maxTubes).random()
        val usedYPositions = mutableListOf<Int>()
        val pipes = mutableListOf<Pipe>()
        repeat(numberOfPipes) {
            var y: Int
            var attempts = 0
            do {
                y = (minY..maxY).random()
                attempts++
            } while (
                usedYPositions.any { existingY -> kotlin.math.abs(existingY - y) < pipeHeight + minGap }
                && attempts < 20
            )
            usedYPositions.add(y)
            pipes.add(Pipe(pipeX, y, pipeWidth, pipeHeight))
        }
        return pipes
    }
    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            birdFrame = (birdFrame + 1) % 3
            velocityY += 5f
            bird_y += velocityY.toInt()
            bird_x += directionX * 10
            val birdRect = Rect(bird_x, bird_y, bird_x + birdSize, bird_y + birdSize)
            for (pipe in pipesList) {
                val pipeRect = Rect(
                    pipe.x + 10,
                    pipe.y + 140,
                    pipe.x + pipe.width - 10,
                    pipe.y + pipe.height - 140
                )
                if (Rect.intersects(birdRect, pipeRect)) {
                    gameRunning = false
                    showDialog = true
                    break
                }
            }
            if (bird_y <= -20 || bird_y >= screenHeight - 40) {
                gameRunning = false
                showDialog = true
            }
            if (bird_x <= 0 || bird_x >= screenWidth - birdSize) {
                directionX *= -1
                soundPool.play(coinId, 1f, 1f, 0, 0, 1f) // leftVol, rightVol, priority, loop, rate

                score++
                pipesList.clear()
                pipesList.addAll(generatePipes())
            }
            when (score) {
                in 10..20 -> {
                    velocityY += 1f
                    bird_x += 3 * directionX
                }
                in 21..30 -> {
                    velocityY += 1f
                    bird_x += 2 * directionX
                }
                in 31..40 -> {
                    velocityY += 2f
                    bird_x += 4 * directionX
                }
                in 41..60 -> {
                    velocityY += 2f
                    bird_x += 13 * directionX
                }
                in 61..Int.MAX_VALUE -> {
                    velocityY += 5f
                    bird_x += 13 * directionX
                }
            }
            if (score > highScore) highScore = score
            delay(30)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (gameRunning) {
                        velocityY = -25f
                        soundPool.play(jumpSoundId, 1f, 1f, 0, 0, 1f) // leftVol, rightVol, priority, loop, rate
                    }
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bg = if (score >= 10) bgSpecial else bgNormal
            Image(
                painter = painterResource(id = bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(id = R.drawable.footer),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            pipesList.forEach { pipe ->
                Image(
                    painter = painterResource(
                        id = if (pipe.x == 0) R.drawable.pipe else R.drawable.pipe_r
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(pipe.width.dp, pipe.height.dp)
                        .offset(pipe.x.dp, pipe.y.dp)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.footer),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .rotate(180f),
                contentScale = ContentScale.Crop
            )
            val birdFrames = if (score >= 10) {
                if (directionX == 1) birdFramesRightNew else birdFramesLeftNew
            } else {
                if (directionX == 1) birdFramesRight else birdFramesLeft
            }
            Image(
                painter = painterResource(id = birdFrames[birdFrame]),
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        start = bird_x.coerceIn(0, screenWidth - birdSize).dp,
                        top = bird_y.coerceAtLeast(0).dp
                    )
                    .size(birdSize.dp)
            )
            Text(
                text = "$score",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 76.sp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            )
        }
        if (showDialog) {
            soundPool.play(gameOverId, 1f, 1f, 0, 0, 1f) // leftVol, rightVol, priority, loop, rate
            Dialog(onDismissRequest = { resetGame() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(300.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Hashiv", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Счёт: $score", color = Color.White)
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = { resetGame() }) {
                                Text("Ayo")
                            }
                            Button(onClick = {
                                println("Рекорд: $highScore")
                            }) {
                                Text("Rekordner")
                            }
                        }
                    }
                }
            }
        }
    }
}