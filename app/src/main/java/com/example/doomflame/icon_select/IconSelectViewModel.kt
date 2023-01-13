package com.example.doomflame.icon_select

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable

private const val ITEM_DEFAULT = 0
private const val ITEM_SPECIAL = 1

class IconSelectViewModel : ViewModel() {
    fun observeItems(): Observable<List<IconSelectItem>> = Observable.just(
        listOf(
            IconSelectItem(
                title = "Default",
                payload = ITEM_DEFAULT
            ),
            IconSelectItem(
                title = "Special",
                payload = ITEM_SPECIAL
            ),
        )
    )

    fun handleClick(context: Context, payload: Int) {
        context.startService(Intent(context, ChangeAppIconService::class.java))
//        val pm = context.packageManager!!
//        val old: String
//        val new: String
//        val pkg = "com.example.doomflame"
//        when (payload) {
//            ITEM_DEFAULT -> {
//                new = "com.example.doomflame.ModeSelectActivityDefault"
//                old = "com.example.doomflame.ModeSelectActivitySpecial"
//            }
//            ITEM_SPECIAL -> {
//                new = "com.example.doomflame.ModeSelectActivitySpecial"
//                old = "com.example.doomflame.ModeSelectActivityDefault"
//            }
//            else -> return
//        }
//
//
//        pm.setComponentEnabledSetting(
//            ComponentName(pkg, new),
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP,
//        )
//        pm.setComponentEnabledSetting(
//            ComponentName(pkg, old),
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP,
//        )
//        println("Handle payload $payload")
    }
}