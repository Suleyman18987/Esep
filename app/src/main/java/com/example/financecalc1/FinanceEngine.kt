package com.example.financecalc1

import kotlin.math.expm1
import kotlin.math.ln1p
import kotlin.math.pow

data class Words(val ru: String, val kk: String) {
    fun inLanguage(language: String) = if (language == "kk") kk else ru
}

enum class Tool(val title: Words, val description: Words, val icon: String) {
    BUDGET(Words("Бюджет", "Бюджет"), Words("Доходы и расходы", "Кірістер мен шығындар"), "budget"),
    SAVINGS(Words("Накопления", "Жинақ"), Words("Будущая сумма", "Болашақтағы сома"), "savings"),
    INTEREST(Words("Проценты", "Пайыздар"), Words("Простые и сложные", "Жай және күрделі"), "interest"),
    LOAN(Words("Кредит", "Несие"), Words("Платёж и переплата", "Төлем және артық төлем"), "loan"),
    DEBT(Words("Долговая нагрузка", "Қарыз жүктемесі"), Words("Доля платежей в доходе", "Төлемдердің кірістегі үлесі"), "debt"),
    INVESTMENT(Words("Инвестиции", "Инвестициялар"), Words("Доходность вложений", "Салым табыстылығы"), "investment"),
    INFLATION(Words("Инфляция", "Инфляция"), Words("Разница ставок", "Мөлшерлемелер айырмасы"), "inflation"),
    GOAL(Words("Финансовая цель", "Қаржылық мақсат"), Words("Сколько нужно сегодня", "Бүгін қанша қажет"), "goal")
}

data class InputSpec(
    val label: Words,
    val example: String,
    val min: Double = 0.0,
    val max: Double = 1e15,
    val strictlyPositive: Boolean = false,
    val integer: Boolean = false
)

data class Calculation(
    val label: Words,
    val value: Double,
    val unit: Words = Words("₸", "₸"),
    val details: List<Pair<Words, Double>> = emptyList(),
    val graphValues: List<Double> = emptyList(),
    val graphLabels: List<Words> = emptyList(),
    val note: Words? = null
)

class InputProblem(val index: Int, val reason: String) : IllegalArgumentException(reason)

/** Mathematical models are independent of Android UI, theme and language. */
object FinanceEngine {
    private fun money(ru: String, kk: String, example: String, positive: Boolean = false) =
        InputSpec(Words(ru, kk), example, strictlyPositive = positive)
    private fun rate(ru: String, kk: String, example: String, signed: Boolean = false) =
        InputSpec(Words(ru, kk), example, if (signed) -1000.0 else 0.0, 1000.0)
    private fun years() = InputSpec(Words("Срок, лет", "Мерзім, жыл"), "2", max = 1000.0, strictlyPositive = true)

    fun modes(tool: Tool): List<Words> = when (tool) {
        Tool.SAVINGS -> listOf(Words("Разовый взнос", "Бір реттік салым"), Words("Ежегодные взносы", "Жыл сайынғы салым"))
        Tool.INTEREST -> listOf(Words("Простые", "Жай"), Words("Сложные", "Күрделі"))
        else -> emptyList()
    }

    fun inputs(tool: Tool, mode: Int): List<InputSpec> = when (tool) {
        Tool.BUDGET -> listOf(money("Доход за месяц, ₸", "Айлық кіріс, ₸", "300000"), money("Расходы за месяц, ₸", "Айлық шығындар, ₸", "210000"))
        Tool.SAVINGS -> listOf(money(if (mode == 0) "Начальная сумма, ₸" else "Взнос в конце года, ₸", if (mode == 0) "Бастапқы сома, ₸" else "Жыл соңындағы салым, ₸", "100000"), rate("Ставка, % в год", "Мөлшерлеме, жылдық %", "10"), years())
        Tool.INTEREST -> listOf(money("Начальная сумма, ₸", "Бастапқы сома, ₸", "100000"), rate("Ставка, % в год", "Мөлшерлеме, жылдық %", "10"), years()) +
            if (mode == 1) listOf(InputSpec(Words("Начислений в год", "Жылдық есептеу саны"), "12", max = 365.0, strictlyPositive = true, integer = true)) else emptyList()
        Tool.LOAN -> listOf(money("Сумма кредита, ₸", "Несие сомасы, ₸", "1000000", true), rate("Ставка, % в месяц", "Мөлшерлеме, айлық %", "1,5"), InputSpec(Words("Срок, месяцев", "Мерзім, ай"), "24", max = 1200.0, strictlyPositive = true, integer = true))
        Tool.DEBT -> listOf(money("Платежи за месяц, ₸", "Айлық төлемдер, ₸", "60000"), money("Доход за месяц, ₸", "Айлық кіріс, ₸", "300000", true))
        Tool.INVESTMENT -> listOf(money("Вложено, ₸", "Салынған сома, ₸", "100000", true), money("Получено всего, ₸", "Барлық алынған сома, ₸", "120000"))
        Tool.INFLATION -> listOf(rate("Номинальная ставка, %", "Номиналды мөлшерлеме, %", "12", true), rate("Инфляция, %", "Инфляция, %", "8", true))
        Tool.GOAL -> listOf(money("Сумма цели, ₸", "Мақсат сомасы, ₸", "1000000", true), rate("Доходность, % в год", "Табыстылық, жылдық %", "10"), years())
    }

    fun parse(text: String): Double? {
        val normalized = text.trim().replace("\u00a0", "").replace("\u202f", "").replace(" ", "").replace(',', '.')
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun calculate(tool: Tool, mode: Int, raw: List<String>): Calculation {
        require(mode in 0..1 && (mode == 0 || modes(tool).isNotEmpty()))
        val specs = inputs(tool, mode)
        val v = specs.mapIndexed { i, f ->
            val text = raw.getOrNull(i).orEmpty()
            if (text.isBlank()) throw InputProblem(i, "required")
            val n = parse(text) ?: throw InputProblem(i, "number")
            if (n < f.min || n > f.max || (f.strictlyPositive && n <= 0.0)) throw InputProblem(i, "range")
            if (f.integer && n % 1.0 != 0.0) throw InputProblem(i, "integer")
            n
        }
        val initial = Words("Вложено", "Салым")
        val profit = Words("Проценты", "Пайыздар")
        val result = when (tool) {
            Tool.BUDGET -> {
                val balance = v[0] - v[1]
                Calculation(Words("Остаток бюджета", "Бюджет қалдығы"), balance,
                    details = listOf(Words("50% от остатка", "Қалдықтың 50%-ы") to balance * .5, Words("30% от остатка", "Қалдықтың 30%-ы") to balance * .3, Words("20% от остатка", "Қалдықтың 20%-ы") to balance * .2),
                    graphValues = listOf(v[0], v[1]), graphLabels = listOf(Words("Доход", "Кіріс"), Words("Расходы", "Шығындар")),
                    note = Words("Схема 50/30/20 рассчитана от остатка — как в исходной версии.", "50/30/20 үлестері бастапқы нұсқадағыдай қалдықтан есептеледі."))
            }
            Tool.SAVINGS -> {
                val r = v[1] / 100
                val contributed = if (mode == 0) v[0] else v[0] * v[2]
                val total = if (mode == 0) v[0] * (1 + r).pow(v[2]) else if (r == 0.0) contributed else v[0] * expm1(v[2] * ln1p(r)) / r
                Calculation(Words("Итоговая сумма", "Қорытынды сома"), total, details = listOf(initial to contributed, profit to total - contributed), graphValues = listOf(contributed, total - contributed), graphLabels = listOf(initial, profit),
                    note = Words(if (mode == 0) "Капитализация раз в год. Ставка в примере не является прогнозом." else "Одинаковые взносы в конце каждого года. Капитализация раз в год.", if (mode == 0) "Жыл сайын капиталданады. Мысалдағы мөлшерлеме болжам емес." else "Бірдей салым әр жылдың соңында түседі. Капиталдандыру жыл сайын."))
            }
            Tool.INTEREST -> {
                val total = if (mode == 0) v[0] * (1 + v[1] / 100 * v[2]) else v[0] * (1 + v[1] / 100 / v[3]).pow(v[3] * v[2])
                Calculation(if (mode == 0) Words("Сумма процентов", "Пайыз сомасы") else Words("Итоговая сумма", "Қорытынды сома"), if (mode == 0) total - v[0] else total,
                    details = listOf(initial to v[0], if (mode == 0) Words("Итого с процентами", "Пайызбен бірге") to total else profit to total - v[0]),
                    graphValues = listOf(v[0], total - v[0]), graphLabels = listOf(initial, profit))
            }
            Tool.LOAN -> {
                val r = v[1] / 100
                // This stable form avoids overflow in (1+r)^n and cancellation near zero.
                val payment = if (r == 0.0) v[0] / v[2] else v[0] * r / -expm1(-v[2] * ln1p(r))
                val overpayment = (payment * v[2] - v[0]).coerceAtLeast(0.0)
                Calculation(Words("Ежемесячный платёж", "Ай сайынғы төлем"), payment, Words("₸/мес", "₸/ай"),
                    details = listOf(Words("Переплата", "Артық төлем") to overpayment, Words("Всего к возврату", "Барлық қайтарылатын сома") to payment * v[2]),
                    graphValues = listOf(v[0], overpayment), graphLabels = listOf(Words("Основной долг", "Негізгі қарыз"), Words("Переплата", "Артық төлем")),
                    note = Words("Используется месячная ставка. Комиссии и страховка не включены.", "Айлық мөлшерлеме қолданылады. Комиссия мен сақтандыру есепке алынбайды."))
            }
            Tool.DEBT -> Calculation(Words("Доля платежей в доходе", "Төлемдердің кірістегі үлесі"), v[0] / v[1] * 100, Words("%", "%"),
                details = listOf(Words("Платежи", "Төлемдер") to v[0], Words("Доход", "Кіріс") to v[1]), graphValues = listOf(v[0], (v[1] - v[0]).coerceAtLeast(0.0)), graphLabels = listOf(Words("Платежи", "Төлемдер"), Words("Остаток дохода", "Кіріс қалдығы")),
                note = Words("Показатель может превышать 100%. Это расчёт доли, а не решение банка.", "Көрсеткіш 100%-дан асуы мүмкін. Бұл — үлес есебі, банктің шешімі емес."))
            Tool.INVESTMENT -> Calculation(Words("Доходность вложений", "Салым табыстылығы"), (v[1] - v[0]) / v[0] * 100, Words("%", "%"),
                details = listOf(Words("Прибыль / убыток", "Пайда / шығын") to v[1] - v[0], Words("Получено всего", "Барлық алынған сома") to v[1]), graphValues = listOf(v[0], v[1]), graphLabels = listOf(initial, Words("Получено", "Алынған")))
            Tool.INFLATION -> Calculation(Words("Разница ставок", "Мөлшерлемелер айырмасы"), v[0] - v[1], Words("п.п.", "п.т."),
                graphValues = listOf(v[0], v[1]), graphLabels = listOf(Words("Номинальная ставка", "Номиналды мөлшерлеме"), Words("Инфляция", "Инфляция")),
                note = Words("Упрощённая оценка: номинальная ставка минус инфляция. Не точная реальная доходность.", "Жеңілдетілген баға: номиналды мөлшерлемеден инфляция шегеріледі. Бұл нақты табыстылық емес."))
            Tool.GOAL -> Calculation(Words("Нужно вложить сегодня", "Бүгін салу қажет"), v[0] / (1 + v[1] / 100).pow(v[2]),
                details = listOf(Words("Цель в будущем", "Болашақ мақсат") to v[0]),
                note = Words("Разовый вклад сегодня, а не ежемесячный взнос. Капитализация раз в год; ставка — допущение расчёта.", "Бұл айлық салым емес, бүгінгі бір реттік салым. Капиталдандыру жыл сайын; мөлшерлеме — есеп шарты."))
        }
        if (!result.value.isFinite() || result.details.any { !it.second.isFinite() } || result.value > 1e18) throw InputProblem(0, "overflow")
        return result
    }

    fun formula(tool: Tool, mode: Int): String = when (tool) {
        Tool.BUDGET -> "B = D − E\n50% = B × 0,5 · 30% = B × 0,3 · 20% = B × 0,2"
        Tool.SAVINGS -> if (mode == 0) "FV = P × (1 + r)ᵗ" else "S = P × ((1 + r)ᵗ − 1) / r\nr = 0: S = P × t"
        Tool.INTEREST -> if (mode == 0) "I = P × r × t" else "A = P × (1 + r / n)ⁿᵗ"
        Tool.LOAN -> "A = P × r / (1 − (1 + r)⁻ⁿ)\nr = 0: A = P / n"
        Tool.DEBT -> "DTI = M / D × 100%"
        Tool.INVESTMENT -> "ROI = (V − P) / P × 100%"
        Tool.INFLATION -> "Δ = R − i"
        Tool.GOAL -> "PV = F / (1 + r)ᵗ"
    }

    fun symbols(tool: Tool, mode: Int): Words = when (tool) {
        Tool.BUDGET -> Words("D — доход; E — расходы; B — остаток.", "D — кіріс; E — шығындар; B — қалдық.")
        Tool.SAVINGS -> Words("P — ${if (mode == 0) "разовый вклад" else "взнос в конце года"}; r — годовая ставка / 100; t — годы.", "P — ${if (mode == 0) "бір реттік салым" else "жыл соңындағы салым"}; r — жылдық мөлшерлеме / 100; t — жыл.")
        Tool.INTEREST -> Words("P — вклад; r — годовая ставка / 100; t — годы; n — начислений в год.", "P — салым; r — жылдық мөлшерлеме / 100; t — жыл; n — жылдық есептеу саны.")
        Tool.LOAN -> Words("P — сумма кредита; r — месячная ставка / 100; n — число месяцев.", "P — несие сомасы; r — айлық мөлшерлеме / 100; n — ай саны.")
        Tool.DEBT -> Words("M — платежи за месяц; D — доход за месяц.", "M — айлық төлемдер; D — айлық кіріс.")
        Tool.INVESTMENT -> Words("P — вложено; V — вся полученная сумма, включая первоначальный вклад.", "P — салынған сома; V — бастапқы салыммен бірге алынған барлық сома.")
        Tool.INFLATION -> Words("R — номинальная ставка; i — инфляция. Обе величины в процентах.", "R — номиналды мөлшерлеме; i — инфляция. Екі шама да пайызбен.")
        Tool.GOAL -> Words("F — будущая цель; r — годовая ставка / 100; t — годы; PV — сумма сегодня.", "F — болашақ мақсат; r — жылдық мөлшерлеме / 100; t — жыл; PV — бүгінгі сома.")
    }
}
