package com.example.financecalc1

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeFlowTest {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private fun launch(theme: String = "wave", language: String = "ru") {
        inst.targetContext.getSharedPreferences("esep_preferences", Context.MODE_PRIVATE).edit().clear()
            .putString("theme", theme).putString("language", language).commit()
        inst.startActivitySync(Intent(inst.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        inst.waitForIdleSync()
    }
    @Test fun allEightCalculatorsProduceResults() {
        launch()
        Tool.entries.forEach { tool ->
            onView(withContentDescription("${tool.title.ru}. ${tool.description.ru}")).perform(scrollTo(), click())
            onView(withText("Пример")).perform(click())
            onView(withTagValue(`is`("result_value"))).check(matches(not(withText("—"))))
            onView(withText("Копировать")).check(matches(isDisplayed()))
            androidx.test.espresso.Espresso.pressBack()
        }
    }
    @Test fun keypadEditsAndValidatesRequiredFields() {
        launch()
        onView(withContentDescription("Бюджет. Доходы и расходы")).perform(scrollTo(), click())
        onView(withTagValue(`is`("calculate"))).perform(click())
        onView(withText("Введите значение")).check(matches(isDisplayed()))
        onView(withTagValue(`is`("key_1"))).perform(click())
        onView(withTagValue(`is`("key_0"))).perform(click())
        onView(withTagValue(`is`("field_0"))).check(matches(withText("10")))
        onView(withText("Далее")).perform(click())
        onView(withTagValue(`is`("field_1"))).check(matches(hasFocus()))
        onView(withTagValue(`is`("key_3"))).perform(click())
        onView(withTagValue(`is`("field_1"))).check(matches(withText("3")))
        onView(withTagValue(`is`("calculate"))).perform(click())
        onView(withTagValue(`is`("result_value"))).check(matches(withText("7 ₸")))
    }
    @Test fun languageAndThemePersistAfterActivityRestart() {
        launch()
        onView(withContentDescription("Настройки")).perform(click())
        onView(withTagValue(`is`("palette_graphite"))).perform(scrollTo(), click())
        onView(withText("Қазақша")).perform(scrollTo(), click())
        onView(withText("Безендіру")).check(matches(isDisplayed()))
        val prefs = inst.targetContext.getSharedPreferences("esep_preferences", Context.MODE_PRIVATE)
        org.junit.Assert.assertEquals("graphite", prefs.getString("palette_wave", null))
        org.junit.Assert.assertEquals("kk", prefs.getString("language", null))
        androidx.test.espresso.Espresso.pressBack()
        onView(withText("Қаржы калькуляторлары")).check(matches(isDisplayed()))
    }
}
