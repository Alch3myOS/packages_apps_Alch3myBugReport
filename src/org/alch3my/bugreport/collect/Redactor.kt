package org.alch3my.bugreport.collect

import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

class Redactor(context: Context) {
    enum class Kind(val token: String) {
        WIFI("<wifi>"),
        MAC("<mac>"),
        IP("<ip>"),
        EMAIL("<email>"),
        PHONE("<phone>"),
        ACCOUNT("<account>"),
        SERIAL("<serial>"),
        LOCATION("<loc>"),
        DEVICE_NAME("<device-name>"),
    }

    private val counts = IntArray(Kind.values().size)
    private val exact: List<Pair<String, Kind>>

    init {
        val values = mutableListOf<Pair<String, Kind>>()
        try {
            AccountManager.get(context).accounts.map { it.name }.filter { it.length >= 4 }
                .forEach { values += it to Kind.ACCOUNT }
        } catch (e: Exception) {}
        try {
            Build.getSerial().takeIf { it.length >= 4 && it != Build.UNKNOWN }?.let { values += it to Kind.SERIAL }
        } catch (e: Exception) {}
        try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            for (slot in 0 until (tm?.activeModemCount ?: 0)) {
                tm?.getImei(slot)?.takeIf { it.length >= 8 }?.let { values += it to Kind.SERIAL }
            }
        } catch (e: Exception) {}
        val name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        if (name != null && name.length >= 4 && name != Build.MODEL) values += name to Kind.DEVICE_NAME
        exact = values.distinctBy { it.first }.sortedByDescending { it.first.length }
    }

    fun counts(): Map<String, Int> = Kind.values().associate { it.name.lowercase() to counts[it.ordinal] }

    fun line(input: String): String {
        var out = input
        for ((value, kind) in exact) {
            if (!out.contains(value)) continue
            var i = out.indexOf(value)
            while (i >= 0) {
                hit(kind)
                i = out.indexOf(value, i + value.length)
            }
            out = out.replace(value, kind.token)
        }
        out = SSID.replace(out) { m ->
            if (m.groupValues[2].trim('"', ''') in KEEP_SSID) m.value else {
                hit(Kind.WIFI)
                m.groupValues[1] + Kind.WIFI.token
            }
        }
        out = MAC.replace(out) { m ->
            if (m.value.lowercase() in KEEP_MAC) m.value else {
                hit(Kind.MAC)
                Kind.MAC.token
            }
        }
        out = EMAIL.replace(out) {
            hit(Kind.EMAIL)
            Kind.EMAIL.token
        }
        out = IPV4.replace(out) { m ->
            if (m.value in KEEP_IP || m.value.startsWith("127.")) m.value else {
                hit(Kind.IP)
                Kind.IP.token
            }
        }
        out = IPV6.replace(out) {
            hit(Kind.IP)
            Kind.IP.token
        }
        out = PHONE.replace(out) {
            hit(Kind.PHONE)
            Kind.PHONE.token
        }
        out = LOCATION_KEY.replace(out) { m ->
            hit(Kind.LOCATION)
            m.groupValues[1] + Kind.LOCATION.token
        }
        out = LOCATION_FIX.replace(out) { m ->
            hit(Kind.LOCATION)
            m.groupValues[1] + Kind.LOCATION.token
        }
        return out
    }

    private fun hit(kind: Kind) {
        counts[kind.ordinal]++
    }

    private companion object {
        val SSID = Regex("""(?i)(\bssid["']?\s*[:=]\s*)("[^"]*"|'[^']*'|[^\s,;}\]]+)""")
        val MAC = Regex("""\b(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}\b""")
        val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        val IPV4 = Regex("""\b(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(?:\.(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}\b""")
        val IPV6 = Regex("""\b(?:2[0-9a-fA-F]{3}|fe80|fd[0-9a-fA-F]{2}):[0-9a-fA-F:]{4,}\b""")
        val PHONE = Regex("""(?<![\w+])\+[1-9]\d{7,14}\b""")
        val LOCATION_KEY = Regex("""(?i)(\b(?:lat|lng|lon|latitude|longitude)\s*[=:]\s*)-?\d{1,3}\.\d{3,}""")
        val LOCATION_FIX = Regex("""(Location\[\w+\s+)-?\d{1,3}\.\d+,\s*-?\d{1,3}\.\d+""")
        val KEEP_SSID = setOf("", "<unknown ssid>", "null", "0x", "*")
        val KEEP_MAC = setOf("00:00:00:00:00:00", "02:00:00:00:00:00", "ff:ff:ff:ff:ff:ff")
        val KEEP_IP = setOf("0.0.0.0", "255.255.255.255", "255.255.255.0")
    }
}