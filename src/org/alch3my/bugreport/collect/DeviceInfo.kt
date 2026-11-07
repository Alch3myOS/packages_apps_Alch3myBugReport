package org.alch3my.bugreport.collect

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.SystemProperties
import org.json.JSONObject
import java.io.File

object DeviceInfo {
    private val PROPS = listOf(
        "ro.product.model",
        "ro.product.device",
        "ro.build.display.id",
        "ro.build.date",
        "ro.build.date.utc",
        "ro.build.version.release",
        "ro.build.version.security_patch",
        "ro.boot.slot_suffix",
        "ro.boot.verifiedbootstate",
        "ro.alch3my.version",
        "ro.alch3my.build.type",
        "gsm.version.baseband",
        "sys.boot.reason",
    )

    fun json(context: Context): JSONObject {
        val props = JSONObject()
        PROPS.forEach { props.put(it, SystemProperties.get(it, "")) }
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return JSONObject()
            .put("device", SystemProperties.get("ro.product.marketname", Build.MODEL))
            .put("model", Build.MODEL)
            .put("props", props)
            .put("kernel", firstLine("/proc/version"))
            .put("uptimeMs", SystemClock.elapsedRealtime())
            .put("batteryLevel", battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1)
            .put("batteryTempC", (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0)
            .put("charging", battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
            .put("dataFreeBytes", StatFs(Environment.getDataDirectory().path).availableBytes)
    }

    private fun firstLine(path: String): String = try {
        File(path).bufferedReader().use { it.readLine() ?: "" }
    } catch (e: Exception) {
        ""
    }
}