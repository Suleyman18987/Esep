package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DebtActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt)

        val etPayments = findViewById<EditText>(R.id.etDebtPayments)
        val etIncome = findViewById<EditText>(R.id.etMonthlyIncome)
        val btnDTI = findViewById<Button>(R.id.btnCalculateDebt)
        val tvDTI = findViewById<TextView>(R.id.tvDTIResult)

        btnDTI.setOnClickListener {
            val payments = etPayments.text.toString().toDoubleOrNull() ?: 0.0
            val income = etIncome.text.toString().toDoubleOrNull() ?: 0.0

            // Вычисляем DTI в процентах: (Долги / Доход) * 100%
            val dti = if (income != 0.0) {
                (payments / income) * 100.0
            } else {
                0.0
            }

            tvDTI.text = "Қарыз/Табыс қатынасы (ҚТҚ): ${"%.2f".format(dti)} %"
        }
    }
}