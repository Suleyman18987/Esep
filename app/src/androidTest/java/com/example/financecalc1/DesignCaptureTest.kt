package com.example.financecalc1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android frame captures for human review; not a replacement for functional assertions. */
@RunWith(AndroidJUnit4::class)
class DesignCaptureTest {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private fun capture(name: String) {
        inst.waitForIdleSync()
        val image = inst.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        val folder = File(inst.targetContext.getExternalFilesDir(null), "design-qa").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @Test fun captureThreeThemesAndCalculatorScreens() {
        for (theme in listOf("wave", "mint", "graphite")) {
            inst.targetContext.getSharedPreferences("esep_preferences", Context.MODE_PRIVATE).edit().clear()
                .putString("theme", theme).putString("language", "ru").commit()
            inst.startActivitySync(Intent(inst.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            capture("$theme-home")
            for (tool in Tool.entries) {
                onView(withContentDescription("${tool.title.ru}. ${tool.description.ru}")).perform(scrollTo(), click())
                onView(withText("Пример")).perform(click())
                capture("$theme-${tool.name.lowercase()}")
                Espresso.pressBack()
            }
            onView(withContentDescription("Настройки")).perform(click())
            capture("$theme-settings")
        }
    }
}
