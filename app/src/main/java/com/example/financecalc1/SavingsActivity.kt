package com.example.financecalc1


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class SavingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings)

        // Элементы интерфейса для будущей стоимости
        val etPV = findViewById<EditText>(R.id.etPV)
        val etRateFV = findViewById<EditText>(R.id.etRateFV)
        val etYearsFV = findViewById<EditText>(R.id.etYearsFV)
        val btnFV = findViewById<Button>(R.id.btnCalculateFV)
        val tvFVResult = findViewById<TextView>(R.id.tvFVResult)

        // Элементы интерфейса для ежемесячных накоплений
        val etP = findViewById<EditText>(R.id.etP)
        val etRateS = findViewById<EditText>(R.id.etRateS)
        val etYearsS = findViewById<EditText>(R.id.etYearsS)
        val btnS = findViewById<Button>(R.id.btnCalculateS)
        val tvSResult = findViewById<TextView>(R.id.tvSResult)

        // Кнопка: расчет будущей стоимости (FV = PV * (1 + r)^n)
        btnFV.setOnClickListener {
            val pv = etPV.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRateFV.text.toString().toDoubleOrNull() ?: 0.0
            val years = etYearsFV.text.toString().toDoubleOrNull() ?: 0.0

            // Переводим процентную ставку в десятичную (например, 10% -> 0.10)
            val r = rate / 100.0

            // Вычисляем будущую стоимость вложения
            val fv = pv * (1 + r).pow(years)

            tvFVResult.text = "Болашақтағы құн (FV): ${"%.2f".format(fv)} ₸"
        }

        // Кнопка: расчет накоплений при регулярных взносах
        // Формула: S = P * [((1 + r)^n - 1) / r]
        btnS.setOnClickListener {
            val p = etP.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRateS.text.toString().toDoubleOrNull() ?: 0.0
            val years = etYearsS.text.toString().toDoubleOrNull() ?: 0.0

            val r = rate / 100.0

            // Вычисляем сумму накоплений
            val s = if (r != 0.0) {
                p * (( (1 + r).pow(years) - 1) / r)
            } else {
                // Если процент равен 0, то накопления = P * n
                p * years
            }

            tvSResult.text = "Жинақ сомасы: ${"%.2f".format(s)} ₸"
        }
    }
}