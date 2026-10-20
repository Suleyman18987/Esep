package com.example.financecalc1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BudgetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Инициализация элементов интерфейса
        val etIncome = findViewById<EditText>(R.id.etIncome)
        val etExpenses = findViewById<EditText>(R.id.etExpenses)
        val btnCalculate = findViewById<Button>(R.id.btnCalculateBudget)
        val tvResult = findViewById<TextView>(R.id.tvBudgetResult)
        val tvBudget50Percent = findViewById<TextView>(R.id.tvBudget50Percent)
        val tvBudget30Percent = findViewById<TextView>(R.id.tvBudget30Percent)
        val tvBudget20Percent = findViewById<TextView>(R.id.tvBudget20Percent)

        btnCalculate.setOnClickListener {
            // Получаем введённые данные
            val income = etIncome.text.toString().toDoubleOrNull() ?: 0.0
            val expenses = etExpenses.text.toString().toDoubleOrNull() ?: 0.0

            // Вычисляем сальдо бюджета (Доходы – Расходы)
            val balance = income - expenses

            // Вычисляем 50%, 30% и 20% от сальдо
            val fiftyPercent = balance * 0.5
            val thirtyPercent = balance * 0.3
            val twentyPercent = balance * 0.2

            // Отображаем результаты с денежным знаком
            tvResult.text = "Сальдо бюджеты: ${"%.2f".format(balance)} ₸"
            tvBudget50Percent.text = "${"%.2f".format(fiftyPercent)} ₸"
            tvBudget30Percent.text = "${"%.2f".format(thirtyPercent)} ₸"
            tvBudget20Percent.text = "${"%.2f".format(twentyPercent)} ₸"
        }
    }
}