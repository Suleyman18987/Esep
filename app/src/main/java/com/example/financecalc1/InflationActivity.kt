package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InflationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inflation)

        val etNominal = findViewById<EditText>(R.id.etNominal)
        val etInflation = findViewById<EditText>(R.id.etInflation)
        val btnReal = findViewById<Button>(R.id.btnCalculateReal)
        val tvReal = findViewById<TextView>(R.id.tvRealResult)

        btnReal.setOnClickListener {
            val nominal = etNominal.text.toString().toDoubleOrNull() ?: 0.0
            val inflation = etInflation.text.toString().toDoubleOrNull() ?: 0.0

            // Реальная доходность = Номинальная доходность – Инфляция
            val real = nominal - inflation

            tvReal.text = "Нақты табыстылық: ${"%.2f".format(real)} %"
        }
    }
}