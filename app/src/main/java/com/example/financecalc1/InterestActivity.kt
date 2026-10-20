package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class InterestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interest)

        // Элементы интерфейса для простых процентов
        val etP_simple = findViewById<EditText>(R.id.etP_simple)
        val etRate_simple = findViewById<EditText>(R.id.etRate_simple)
        val etTime_simple = findViewById<EditText>(R.id.etTime_simple)
        val btnSimple = findViewById<Button>(R.id.btnCalculateSimple)
        val tvSimpleResult = findViewById<TextView>(R.id.tvSimpleResult)

        // Элементы интерфейса для сложных процентов
        val etP_compound = findViewById<EditText>(R.id.etP_compound)
        val etRate_compound = findViewById<EditText>(R.id.etRate_compound)
        val etN_compound = findViewById<EditText>(R.id.etN_compound)
        val etTime_compound = findViewById<EditText>(R.id.etTime_compound)
        val btnCompound = findViewById<Button>(R.id.btnCalculateCompound)
        val tvCompoundResult = findViewById<TextView>(R.id.tvCompoundResult)

        // Кнопка: расчет простых процентов (I = P * r * t)
        btnSimple.setOnClickListener {
            val P = etP_simple.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate_simple.text.toString().toDoubleOrNull() ?: 0.0
            val t = etTime_simple.text.toString().toDoubleOrNull() ?: 0.0

            val r = rate / 100.0  // ставка в десятичном виде

            // Вычисляем сумму процентов
            val I = P * r * t

            tvSimpleResult.text = "Пайыздар (I): ${"%.2f".format(I)} ₸"
        }

        // Кнопка: расчет сложных процентов (A = P * (1 + r/n)^(n*t))
        btnCompound.setOnClickListener {
            val P = etP_compound.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate_compound.text.toString().toDoubleOrNull() ?: 0.0
            val n = etN_compound.text.toString().toDoubleOrNull() ?: 1.0
            val t = etTime_compound.text.toString().toDoubleOrNull() ?: 0.0

            val r = rate / 100.0

            // Вычисляем итоговую сумму после начисления сложных процентов
            val A = P * (1 + r / n).pow(n * t)

            tvCompoundResult.text = "Қорытынды сома (A): ${"%.2f".format(A)} ₸"
        }
    }
}