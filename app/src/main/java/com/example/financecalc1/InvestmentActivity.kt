package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InvestmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investment)

        val etInvestment = findViewById<EditText>(R.id.etInvestment)
        val etReturn = findViewById<EditText>(R.id.etReturn)
        val btnROI = findViewById<Button>(R.id.btnCalculateROI)
        val tvROI = findViewById<TextView>(R.id.tvROIResult)

        btnROI.setOnClickListener {
            val investment = etInvestment.text.toString().toDoubleOrNull() ?: 0.0
            val income = etReturn.text.toString().toDoubleOrNull() ?: 0.0

            // Вычисляем ROI в процентах: (Доход – Инвестиции) / Инвестиции * 100%
            val roi = if (investment != 0.0) {
                (income - investment) / investment * 100.0
            } else {
                0.0
            }

            tvROI.text = "Инвестициялық табыс(ИТ): ${"%.2f".format(roi)} %"
        }
    }
}