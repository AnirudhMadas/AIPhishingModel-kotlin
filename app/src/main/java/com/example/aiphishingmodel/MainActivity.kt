package com.example.aiphishingmodel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val enableButton =
            findViewById<Button>(
                R.id.enableButton
            )

        // Message input
        val messageInput =
            findViewById<EditText>(
                R.id.messageInput
            )

        // Analyze button
        val analyzeButton =
            findViewById<Button>(
                R.id.analyzeButton
            )

        // Result text
        val resultText =
            findViewById<TextView>(
                R.id.resultText
            )

        // Disable initially
        analyzeButton.isEnabled = false

        // Enable button only if text exists
        messageInput.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    analyzeButton.isEnabled =
                        s.toString().trim().isNotEmpty()

                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}

            }
        )

        // Analyze button click
        analyzeButton.setOnClickListener {

            val userMessage =
                messageInput.text.toString().trim()

            // Empty check
            if (userMessage.isEmpty()) {

                messageInput.error = "Enter a message"
                return@setOnClickListener

            }

            // Minimum length check
            if (userMessage.length < 5) {

                messageInput.error = "Message too short"
                return@setOnClickListener

            }

            // Prevent only symbols
            val validPattern =
                Regex(".*[a-zA-Z0-9].*")

            if (!validPattern.matches(userMessage)) {

                messageInput.error = "Invalid message"
                return@setOnClickListener

            }

            // Loading state
            resultText.text = "Analyzing..."
            resultText.setTextColor(Color.BLUE)

            // ====================================
            // TEMP DEMO RESULT
            // Replace this with backend response
            // ====================================

            if (
                userMessage.contains(
                    "bank",
                    ignoreCase = true
                ) ||
                userMessage.contains(
                    "otp",
                    ignoreCase = true
                ) ||
                userMessage.contains(
                    "click",
                    ignoreCase = true
                )
            ) {

                resultText.text =
                    "PHISHING DETECTED\nConfidence: 91%"

                resultText.setTextColor(Color.RED)

            } else {

                resultText.text =
                    "SAFE MESSAGE\nConfidence: 94%"

                resultText.setTextColor(Color.GREEN)

            }

        }

        // Notification permission button
        enableButton.setOnClickListener {

            startActivity(
                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )

        }

    }

}