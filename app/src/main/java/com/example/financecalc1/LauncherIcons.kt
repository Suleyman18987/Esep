package com.example.financecalc1

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object LauncherIcons {
    val ids = listOf("wave", "orbit", "prism")
    private fun component(context: Context, id: String) = ComponentName(context.packageName, "${context.packageName}.Launcher${id.replaceFirstChar { it.uppercase() }}")
    fun selected(context: Context): String = ids.firstOrNull {
        context.packageManager.getComponentEnabledSetting(component(context, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } ?: "wave"
    fun select(context: Context, id: String) {
        require(id in ids)
        val manager = context.packageManager
        val ordered = listOf(id) + ids.filter { it != id }
        if (Build.VERSION.SDK_INT >= 33) {
            manager.setComponentEnabledSettings(ordered.map {
                PackageManager.ComponentEnabledSetting(component(context, it), if (it == id) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            })
        } else {
            ordered.forEach {
                manager.setComponentEnabledSetting(component(context, it), if (it == id) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
