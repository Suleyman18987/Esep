package com.example.financecalc1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ArtworkThemeTest {
    @Test fun artworkChangesWithEveryInterfaceAndAccent() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val prefs=inst.targetContext.getSharedPreferences("esep_preferences",Context.MODE_PRIVATE)
        val allHashes=mutableSetOf<Int>()
        val folder=File(inst.targetContext.getExternalFilesDir(null),"artwork-themes").apply {mkdirs()}
        for(style in InterfaceStyle.entries){
            prefs.edit().clear().putString("interface",style.id).putString("language","ru").commit()
            inst.startActivitySync(Intent(inst.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            inst.waitForIdleSync()
            onView(withContentDescription("Настройки")).perform(click())
            val familyHashes=mutableSetOf<Int>()
            for(id in listOf("wave","mint","graphite")){
                onView(withTagValue(`is`("palette_$id"))).perform(scrollTo(),click())
                Espresso.pressBack()
                assertEquals(style.id,prefs.getString("interface",null))
                var hash=0
                onView(allOf(isAssignableFrom(ArtworkIconView::class.java),isDescendantOfA(withTagValue(`is`("tool_BUDGET"))))).perform(object:ViewAction{
                    override fun getConstraints():Matcher<View> = isDisplayed()
                    override fun getDescription()="Capture the actual rendered wallet pixels"
                    override fun perform(ui:UiController,view:View){
                        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                        view.draw(Canvas(b));val pixels=IntArray(b.width*b.height);b.getPixels(pixels,0,b.width,0,0,b.width,b.height)
                        assertTrue(pixels.any {android.graphics.Color.alpha(it)>200})
                        assertTrue(pixels.any {android.graphics.Color.alpha(it)==0})
                        hash=pixels.contentHashCode();b.recycle()
                    }
                })
                assertTrue("Icons must visibly change with the selected accent: ${style.id}/$id",familyHashes.add(hash))
                allHashes.add(hash)
                inst.waitForIdleSync();val image=inst.uiAutomation.takeScreenshot()!!
                File(folder,"${style.id}-$id.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
                onView(withContentDescription("Настройки")).perform(click())
            }
        }
        assertEquals("Three distinct art families with three colors each",9,allHashes.size)
    }
}
