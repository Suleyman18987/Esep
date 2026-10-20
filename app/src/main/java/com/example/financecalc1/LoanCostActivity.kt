package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class LoanCostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_cost)

        val etP = findViewById<EditText>(R.id.etLoanPrincipal)
        val etRate = findViewById<EditText>(R.id.etLoanRate)
        val etN = findViewById<EditText>(R.id.etLoanPeriods)
        val btnCalc = findViewById<Button>(R.id.btnCalculateLoan)
        val tvPayment = findViewById<TextView>(R.id.tvLoanPayment)
        val tvOverpayment = findViewById<TextView>(R.id.tvLoanOverpayment)

        btnCalc.setOnClickListener {
            val P = etP.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate.text.toString().toDoubleOrNull() ?: 0.0
            val n = etN.text.toString().toDoubleOrNull() ?: 1.0

            val r = rate / 100.0  // месячная ставка в десятичном виде

            // Аннуитетный платёж: A = P * [r(1 + r)^n] / [(1 + r)^n - 1]
            val A = if (r != 0.0) {
                P * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
            } else {
                // Если ставка = 0, платёж просто P/n
                P / n
            }

            // Вычисляем общую переплату: (A * n - P)
            val totalPaid = A * n
            val overpayment = totalPaid - P

            tvPayment.text = "Аннуитеттік төлем: ${"%.2f".format(A)} ₸"
            tvOverpayment.text = "Артық төлем: ${"%.2f".format(overpayment)} ₸"
        }
    }
}