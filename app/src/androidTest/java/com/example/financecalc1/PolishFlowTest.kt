package com.example.financecalc1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PolishFlowTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val prefs get()=inst.targetContext.getSharedPreferences("esep_preferences",Context.MODE_PRIVATE)
    private fun launch(style: InterfaceStyle=InterfaceStyle.WAVE) {
        prefs.edit().clear().putString("interface",style.id).putString("language","ru").commit()
        inst.startActivitySync(Intent(inst.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        inst.waitForIdleSync()
    }
    private fun shot(name:String) {
        inst.waitForIdleSync()
        val image=inst.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        val folder=File(inst.targetContext.getExternalFilesDir(null),"polish-qa").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
    }
    @Test fun allEightCalculatorsInAllThreeInterfacesAndFormulaSheets() {
        for(style in InterfaceStyle.entries){
            launch(style);shot("${style.id}-home")
            for(tool in Tool.entries){
                onView(withTagValue(`is`("tool_${tool.name}"))).perform(scrollTo(),click())
                onView(withText("Пример")).perform(click())
                onView(withTagValue(`is`("result_value"))).check(matches(not(withText("—"))))
                shot("${style.id}-${tool.name.lowercase()}")
                if(tool==Tool.BUDGET){
                    onView(withContentDescription("Формула и обозначения")).perform(click())
                    onView(withText("B = D − E")).check(matches(isDisplayed()));shot("${style.id}-formula")
                    onView(withContentDescription("Закрыть формулы")).perform(click())
                }
                Espresso.pressBack()
            }
            onView(withContentDescription("Настройки")).perform(click());shot("${style.id}-settings")
        }
    }
    @Test fun palettesPreserveInterfaceFamilyAndAuthorLinksAreVisible() {
        for(style in InterfaceStyle.entries){
            val base=Palette.forStyle(style,style.defaultPalette)
            for(id in listOf("wave","mint","graphite")){
                val p=Palette.forStyle(style,id)
                assertEquals(base.background,p.background);assertEquals(base.surface,p.surface);assertEquals(base.ink,p.ink)
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(p.ink,p.background)>=4.5)
            }
            launch(style);onView(withContentDescription("Настройки")).perform(click())
            for(id in listOf("wave","mint","graphite")){
                onView(withTagValue(`is`("palette_$id"))).perform(scrollTo(),click())
                assertEquals(style.id,prefs.getString("interface",null))
                shot("${style.id}-settings-$id")
                Espresso.pressBack()
                onView(withTagValue(`is`("tool_BUDGET"))).perform(scrollTo(),click())
                onView(withText("Пример")).perform(click())
                onView(withTagValue(`is`("result_value"))).check(matches(not(withText("—"))))
                shot("${style.id}-budget-$id")
                Espresso.pressBack();onView(withContentDescription("Настройки")).perform(click())
            }
        }
        onView(withTagValue(`is`("about_project"))).perform(scrollTo(),click())
        onView(withTagValue(`is`("author_instagram"))).check(matches(isDisplayed()))
        onView(withTagValue(`is`("author_tiktok"))).check(matches(isDisplayed()))
        shot("author-links");onView(withContentDescription("Закрыть")).perform(click())
    }
    @Test fun formulaSheetKeepsSmallRatePrecision() {
        launch(InterfaceStyle.ORBIT);onView(withTagValue(`is`("tool_SAVINGS"))).perform(scrollTo(),click())
        onView(withTagValue(`is`("field_0"))).perform(scrollTo(),replaceText("1000"))
        onView(withTagValue(`is`("field_1"))).perform(scrollTo(),replaceText("0,1"))
        onView(withTagValue(`is`("field_2"))).perform(scrollTo(),replaceText("2"))
        onView(withContentDescription("Формула и обозначения")).perform(click())
        onView(withText("0,001")).check(matches(isDisplayed()))
        onView(withContentDescription("Закрыть формулы")).perform(click())
    }
    @Test fun groupingAndDeletingAndSwitchingFields() {
        launch();onView(withTagValue(`is`("tool_BUDGET"))).perform(scrollTo(),click())
        onView(withTagValue(`is`("key_1"))).perform(click())
        repeat(5){onView(withTagValue(`is`("key_0"))).perform(click())}
        onView(withTagValue(`is`("field_0"))).check(matches(withText("100 000")))
        onView(withContentDescription("Удалить цифру")).perform(click())
        onView(withTagValue(`is`("field_0"))).check(matches(withText("10 000")))
        onView(withTagValue(`is`("field_0"))).perform(replaceText("1234567,89"))
        onView(withTagValue(`is`("field_0"))).check(matches(withText("1 234 567,89")))
        onView(withText("Далее")).perform(click())
        onView(withTagValue(`is`("key_3"))).perform(click())
        onView(withTagValue(`is`("field_1"))).check(matches(withText("3")))
        onView(withTagValue(`is`("calculate"))).perform(click())
        onView(withTagValue(`is`("result_value"))).check(matches(not(withText("—"))))
    }
    @Test fun stylesKeepTheirOwnPalettesAndLauncherIconReallyChanges() {
        launch();onView(withContentDescription("Настройки")).perform(click())
        onView(withTagValue(`is`("interface_orbit"))).perform(scrollTo(),click())
        onView(withTagValue(`is`("palette_wave"))).perform(scrollTo(),click())
        assertEquals("orbit",prefs.getString("interface",null));assertEquals("wave",prefs.getString("palette_orbit",null))
        onView(withTagValue(`is`("interface_prism"))).perform(scrollTo(),click())
        onView(withTagValue(`is`("palette_mint"))).perform(scrollTo(),click())
        assertEquals("mint",prefs.getString("palette_prism",null));assertEquals("wave",prefs.getString("palette_orbit",null))
        try {
            onView(withTagValue(`is`("launcher_picker"))).perform(scrollTo(),click())
            shot("launcher-icons")
            onView(withTagValue(`is`("launcher_orbit"))).perform(scrollTo(),click())
            assertEquals("orbit",LauncherIcons.selected(inst.targetContext))
            assertTrue(inst.targetContext.packageManager.getLaunchIntentForPackage(inst.targetContext.packageName)!!.component!!.className.endsWith("LauncherOrbit"))
            onView(withTagValue(`is`("launcher_picker"))).perform(scrollTo(),click())
            onView(withTagValue(`is`("launcher_prism"))).perform(scrollTo(),click())
            assertEquals("prism",LauncherIcons.selected(inst.targetContext))
        } finally {LauncherIcons.select(inst.targetContext,"wave")}
    }
}
