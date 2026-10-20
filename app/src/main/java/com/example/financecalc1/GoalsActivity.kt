package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class GoalsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        val etGoal = findViewById<EditText>(R.id.etGoalAmount)
        val etRate = findViewById<EditText>(R.id.etGoalRate)
        val etYears = findViewById<EditText>(R.id.etGoalYears)
        val btnCalc = findViewById<Button>(R.id.btnCalculateGoal)
        val tvResult = findViewById<TextView>(R.id.tvGoalResult)

        btnCalc.setOnClickListener {
            val goalAmount = etGoal.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate.text.toString().toDoubleOrNull() ?: 0.0
            val years = etYears.text.toString().toDoubleOrNull() ?: 0.0

            val r = rate / 100.0

            // Необходимая сумма сейчас = Цель / (1 + r)^n
            val present = if (r != 0.0) {
                goalAmount / (1 + r).pow(years)
            } else {
                goalAmount
            }

            tvResult.text = "Қажетті сома: ${"%.2f".format(present)} ₸"
        }
    }
}