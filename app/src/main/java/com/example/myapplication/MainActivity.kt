package com.example.myapplication
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var bellStart: MediaPlayer
    private lateinit var bellStop: MediaPlayer
    private lateinit var countdownTimer: CountDownTimer
    private var timeInMillis: Long = 0
    private var remainingMillis: Long = timeInMillis

    private var totalSets: Int = 0
    private var currentSetIndex: Int = 0
    private var isPaused: Boolean = false
    private var timerStarted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bellStart = MediaPlayer.create(this, R.raw.bellstart)
        bellStop = MediaPlayer.create(this, R.raw.bellstop)
        try {
            findViewById<Button>(R.id.startButton).setOnClickListener { startLogic() }
            findViewById<Button>(R.id.pauseButton).setOnClickListener {
                countdownTimer.cancel()
                timerStarted = false
            }
            findViewById<Button>(R.id.resetButton).setOnClickListener { resetTimer() }
        }catch (e: Exception){
            e.printStackTrace()
        }
        findViewById<Button>(R.id.plusButton).setOnClickListener {setManager("add")}
        findViewById<Button>(R.id.minusButton).setOnClickListener {setManager("sub")}
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

    fun timeLogic() {
        if (timerStarted) {
            // resume logic (your current app only supports pause via cancel)
            // easiest: restart with remainingMillis and continue sets
            countdownTimer?.cancel()
            isPaused = true
            return
        }

        val input = findViewById<EditText>(R.id.inputTime).text.toString()
        val setInput = findViewById<EditText>(R.id.setNumber).text.toString()

        val secondsPerSet = input.toLongOrNull() ?: return
        totalSets = setInput.toIntOrNull() ?: 1
        if (totalSets < 1) totalSets = 1

        currentSetIndex = 0
        timerStarted = true

        timeInMillis = secondsPerSet * 1000L

        startNextSet()
    }

    private fun startNextSet() {
        if (currentSetIndex >= totalSets) {
            timerStarted = false
            return
        }

        // If you want a bell at the start of every set:
        bellStart.start()

        countdownTimer = object : CountDownTimer(timeInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                findViewById<TextView>(R.id.timer).text =
                    String.format("%02d:%02d", minutes, remainingSeconds)

                remainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                findViewById<TextView>(R.id.timer).text = "00:00"
                bellStop.start()

                currentSetIndex += 1
                startNextSet()
            }
        }.start()
    }

    fun resetTimer() {
        findViewById<TextView>(R.id.timer).text = "00:00"
        timerStarted = false
    }

    fun setManager(action: String){
        val editText = findViewById<EditText>(R.id.editTextNumber)
        if (action == "add") {
            val currentText = editText.text.toString()
            val newText = (currentText.toIntOrNull() ?: 0) + 1
            editText.setText(newText.toString())
        } else if (action == "sub") {
            val currentText = editText.text.toString()
            val newText = (currentText.toIntOrNull() ?: 0) - 1
            editText.setText(newText.toString())
        }
    }
}