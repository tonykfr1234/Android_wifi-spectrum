package com.wifispectrum.analyzer.core

import java.io.InputStream
import java.util.zip.GZIPInputStream

/** IEEE MA-L registry lookup built from assets/oui.tsv.gz (see scripts/build_oui.py). */
class VendorDb(private val byOui: Map<Int, String>) {

    val size: Int get() = byOui.size

    fun lookup(mac: String): String? {
        if (Mac.isLocallyAdministered(mac)) return null
        val key = Mac.ouiKey(mac) ?: return null
        return byOui[key]
    }

    companion object {
        val EMPTY = VendorDb(emptyMap())

        fun parseGzip(input: InputStream): VendorDb = GZIPInputStream(input).bufferedReader().use { r ->
            val map = HashMap<Int, String>(48_000)
            r.forEachLine { line ->
                val tab = line.indexOf('\t')
                if (tab == 6) {
                    val key = line.substring(0, 6).toIntOrNull(16)
                    if (key != null) map[key] = line.substring(7)
                }
            }
            VendorDb(map)
        }
    }
}
