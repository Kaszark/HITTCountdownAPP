package com.example.myapplication

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var bellStart: MediaPlayer
    private lateinit var bellStop: MediaPlayer
    private lateinit var whistleSound: MediaPlayer
    private var countdownTimer: CountDownTimer? = null

    // Handles scheduling random whistle blasts during an active work set.
    private val whistleHandler = Handler(Looper.getMainLooper())
    private var whistleEnabled = false
    // Active-set time is split into fixed windows this long; one whistle fires
    // at a random moment inside each window - "random but evenly spaced".
    private val whistleWindowMillis = 8000L

    private var workMillis: Long = 0
    private var restMillis: Long = 0
    private var remainingMillis: Long = 0

    private var totalSets: Int = 0
    private var currentSetIndex: Int = 0
    private var remainingSets: Int = 0
    private var isResting: Boolean = false
    private var isPaused: Boolean = false
    private var timerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bellStart = MediaPlayer.create(this, R.raw.bellstart)
        bellStop = MediaPlayer.create(this, R.raw.bellstop)
        whistleSound = MediaPlayer.create(this, R.raw.whistle)
        findViewById<Switch>(R.id.whistleToggle).setOnCheckedChangeListener { _, isChecked ->
            whistleEnabled = isChecked
            if (isChecked) {
                // If a work set is already running (and not paused), start
                // scheduling whistles for whatever time is left in it.
                if (timerStarted && !isResting && !isPaused) {
                    scheduleWhistles(remainingMillis)
                }
            } else {
                whistleHandler.removeCallbacksAndMessages(null)
            }
        }
        try {
            findViewById<Button>(R.id.startButton).setOnClickListener { startLogic() }
            findViewById<Button>(R.id.pauseButton).setOnClickListener { togglePause() }
            findViewById<Button>(R.id.resetButton).setOnClickListener { resetTimer() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        findViewById<Button>(R.id.plusButton).setOnClickListener { setManager("add") }
        findViewById<Button>(R.id.minusButton).setOnClickListener { setManager("sub") }
    }

    fun startLogic() {
        val setInput = findViewById<EditText>(R.id.setNumber).text.toString()
        val sets = setInput.toIntOrNull() ?: 1
        if (sets < 1) {
            bellStart.start()
            return
        }
        timeLogic()
    }

    // Pause/resume the currently running countdown (work or rest phase).
    fun togglePause() {
        if (!timerStarted) return

        if (!isPaused) {
            countdownTimer?.cancel()
            whistleHandler.removeCallbacksAndMessages(null)
            isPaused = true
            findViewById<Button>(R.id.pauseButton).text = "Resume"
        } else {
            isPaused = false
            findViewById<Button>(R.id.pauseButton).text = "Pause"
            runCountdown(remainingMillis)
        }
    }

    fun timeLogic() {
        if (timerStarted) return // already running - use pause/resume instead

        val minutesInput = findViewById<EditText>(R.id.inputMinutes).text.toString()
        val secondsInput = findViewById<EditText>(R.id.inputSeconds).text.toString()
        val restInput = findViewById<EditText>(R.id.restTime).text.toString()
        val setInput = findViewById<EditText>(R.id.setNumber).text.toString()

        val minutes = minutesInput.toLongOrNull() ?: 0
        val seconds = secondsInput.toLongOrNull() ?: 0
        if (minutes == 0L && seconds == 0L) return // need some work time to start

        val restSeconds = restInput.toLongOrNull() ?: 0

        totalSets = setInput.toIntOrNull() ?: 1
        if (totalSets < 1) totalSets = 1

        workMillis = (minutes * 60 + seconds) * 1000L
        restMillis = restSeconds * 1000L

        currentSetIndex = 0
        remainingSets = totalSets
        isResting = false
        isPaused = false
        timerStarted = true
        findViewById<Button>(R.id.pauseButton).text = "Pause"
        updateSetNumberDisplay()

        startNextSet()
    }

    // Reflects how many active (work) sets are left in the setNumber field.
    private fun updateSetNumberDisplay() {
        findViewById<EditText>(R.id.setNumber).setText(remainingSets.toString())
    }

    private fun startNextSet() {
        if (currentSetIndex >= totalSets) {
            timerStarted = false
            findViewById<TextView>(R.id.timer).text = "Done!"
            return
        }

        isResting = false
        bellStart.start()
        runCountdown(workMillis)
    }

    private fun startRest() {
        // No rest period after the final set
        if (currentSetIndex >= totalSets) {
            timerStarted = false
            findViewById<TextView>(R.id.timer).text = "Done!"
            return
        }

        if (restMillis <= 0) {
            startNextSet()
            return
        }

        isResting = true
        runCountdown(restMillis)
    }

    // Splits durationMillis into fixed-size windows and schedules one whistle
    // at a random moment inside each - gives an unpredictable-but-regular feel.
    private fun scheduleWhistles(durationMillis: Long) {
        whistleHandler.removeCallbacksAndMessages(null)
        if (durationMillis <= 0) return

        var elapsed = 0L
        while (elapsed < durationMillis) {
            val windowEnd = minOf(elapsed + whistleWindowMillis, durationMillis)
            val windowSize = windowEnd - elapsed
            if (windowSize > 0) {
                val fireAt = elapsed + Random.nextLong(windowSize)
                whistleHandler.postDelayed({
                    if (whistleEnabled && timerStarted && !isResting && !isPaused) {
                        whistleSound.seekTo(0)
                        whistleSound.start()
                    }
                }, fireAt)
            }
            elapsed = windowEnd
        }
    }

    private fun runCountdown(durationMillis: Long) {
        countdownTimer?.cancel()

        // Only the active work phase gets random whistles, never rest.
        whistleHandler.removeCallbacksAndMessages(null)
        if (!isResting && whistleEnabled) {
            scheduleWhistles(durationMillis)
        }

        countdownTimer = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = (millisUntilFinished / 1000).toInt()
                val minutes = totalSeconds / 60
                val secs = totalSeconds % 60

                findViewById<TextView>(R.id.timer).text =
                    String.format("%02d:%02d", minutes, secs)

                remainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                findViewById<TextView>(R.id.timer).text = "00:00"
                bellStop.start()

                if (isResting) {
                    // Rest just ended - move on to the next work set
                    startNextSet()
                } else {
                    // Work set just ended - count it and update the visible set counter
                    currentSetIndex += 1
                    remainingSets = (totalSets - currentSetIndex).coerceAtLeast(0)
                    updateSetNumberDisplay()
                    startRest()
                }
            }
        }.start()
    }

    fun resetTimer() {
        countdownTimer?.cancel()
        whistleHandler.removeCallbacksAndMessages(null)
        findViewById<TextView>(R.id.timer).text = "00:00"
        timerStarted = false
        isPaused = false
        isResting = false
        currentSetIndex = 0
        findViewById<Button>(R.id.pauseButton).text = "Pause"

        // Restore the set counter to the full count from the last session, if any.
        if (totalSets > 0) {
            remainingSets = totalSets
            updateSetNumberDisplay()
        }
    }

    fun setManager(action: String) {
        val editText = findViewById<EditText>(R.id.setNumber)
        if (action == "add") {
            val currentText = editText.text.toString()
            val newText = (currentText.toIntOrNull() ?: 0) + 1
            editText.setText(newText.toString())
        } else if (action == "sub") {
            val currentText = editText.text.toString()
            val newValue = ((currentText.toIntOrNull() ?: 0) - 1).coerceAtLeast(1)
            editText.setText(newValue.toString())
        }
    }
}