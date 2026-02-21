package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.utils.firstChildElement
import com.machiav3lli.backup.utils.namedBoolean
import com.machiav3lli.backup.utils.namedByteArray
import com.machiav3lli.backup.utils.namedInt
import com.machiav3lli.backup.utils.namedString
import com.machiav3lli.backup.utils.toElementList
import org.w3c.dom.Element
import timber.log.Timber
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

data class WifiNetwork(
    val ssid: String,
    val preSharedKey: String?,
    val isHidden: Boolean,
    val security: WifiSecurityType,
    val autoJoin: Boolean,
    val isMacRandomized: Boolean,
) {
    /**
     * Command structure:
     *   cmd wifi add-network <ssid-hex> <security> [<passphrase>] [flags]
     * Flags used:
     *   -x   treat <ssid> as hex bytes (UTF-8) – avoids shell quoting issues
     *   -h   hidden SSID
     *   -d   disable auto-join
     */
    private fun buildCommand(): String {
        val ssidHex = ssid.toByteArray(Charsets.UTF_8)
            .joinToString("") { "%02x".format(it) }

        return buildString {
            append("cmd wifi add-network $ssidHex ${security.toCmdArg()}")
            if (security.requiresPassphrase) {
                val psk = preSharedKey
                    ?: error("Security type $security requires a PSK but none is present for '$ssid'")
                append(" '${psk.replace("'", "'\\''")}'")
            }
            append(" -x")
            if (isHidden) append(" -h")
            if (!autoJoin) append(" -d")
        }
    }

    companion object {
        fun buildRestoreCommands(networks: List<WifiNetwork>): List<String> =
            networks.mapNotNull { network ->
                if (!network.security.isRestoreSupported) {
                    Timber.w(
                        "WifiNetworkParser: skipping '${network.ssid}' " +
                                "(${network.security} – enterprise networks require manual setup)"
                    )
                    return@mapNotNull null
                }
                network.buildCommand()
            }
    }
}

/**
 * Security types understood by `cmd wifi add-network / connect-network`.
 * The mapping is based on Android's WifiConfiguration.KeyMgmt bitmask:
 *   Bit 0 (0x01) – NONE  (open or WEP when WEP keys are present)
 *   Bit 1 (0x02) – WPA_PSK
 *   Bit 2 (0x04) – WPA_EAP
 *   Bit 3 (0x08) – IEEE8021X
 *   Bit 7 (0x80) – SAE  (WPA3-Personal)
 *   Bit 8 (0x01 in second byte) – OWE  (Enhanced Open)
 * See frameworks/base/wifi/java/android/net/wifi/WifiConfiguration.java under AOSP.
 */
enum class WifiSecurityType {
    OPEN,
    OWE,

    // Legacy, effectively unsupported by `cmd wifi add-network` on API 29+.
    WEP,
    WPA_PSK,
    WPA3_SAE,
    WPA2_WPA3,

    // WPA-Enterprise (EAP/IEEE 802.1X). `cmd wifi add-network` cannot configure EAP parameters
    // (certificates, identity, phase-2, etc.), so these networks are **skipped** during restore
    WPA_EAP;

    val requiresPassphrase: Boolean
        get() = this in setOf(WEP, WPA_PSK, WPA3_SAE, WPA2_WPA3)

    /** Whether restore via `cmd wifi add-network` is supported for this type. */
    val isRestoreSupported: Boolean
        get() = this != WPA_EAP

    // Returns the security keyword expected by `cmd wifi add-network <ssid> <keyword>`.
    fun toCmdArg(): String = when (this) {
        OPEN      -> "open"
        OWE       -> "owe"
        WEP       -> "wpa2"
        WPA_PSK   -> "wpa2"
        WPA3_SAE  -> "wpa3"
        WPA2_WPA3 -> "wpa3"
        WPA_EAP   -> error("WPA-EAP networks cannot be restored via cmd wifi add-network")
    }

    companion object {
        // Derives the security type from the raw bytes of the `AllowedKeyMgmt` byte-array field
        // keyMgmtBytes:  Raw bytes decoded from the hex string in the XML, or empty.
        fun fromKeyMgmtBytes(keyMgmtBytes: ByteArray, hasWepKey: Boolean): WifiSecurityType {
            // Java BitSet is stored little-endian; first byte covers bits 0–7, second byte 8–15.
            val byte0 = keyMgmtBytes.getOrElse(0) { 0 }.toInt() and 0xFF
            val byte1 = keyMgmtBytes.getOrElse(1) { 0 }.toInt() and 0xFF

            val isNone = byte0 and 0x01 != 0   // bit 0
            val isPsk = byte0 and 0x02 != 0    // bit 1
            val isEap = byte0 and 0x04 != 0    // bit 2
            val is8021x = byte0 and 0x08 != 0  // bit 3
            val isSae = byte0 and 0x80 != 0    // bit 7
            val isOwe = byte1 and 0x01 != 0    // bit 8

            return when {
                isEap || is8021x    -> WPA_EAP
                isPsk && isSae      -> WPA2_WPA3
                isSae               -> WPA3_SAE
                isPsk               -> WPA_PSK
                isOwe               -> OWE
                isNone && hasWepKey -> WEP
                else                -> OPEN
            }
        }
    }
}

object WifiNetworkParser {
    fun parse(stream: InputStream): List<WifiNetwork> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(stream)

        doc.documentElement.normalize()

        return doc.getElementsByTagName("Network")
            .toElementList()
            .mapNotNull { networkNode ->
                runCatching { parseNetwork(networkNode) }
                    .onFailure { e ->
                        Timber.w("WifiNetworkParser: skipping malformed <Network>: $e")
                    }
                    .getOrNull()
            }
    }

    private fun parseNetwork(networkElement: Element): WifiNetwork {
        val wifiConfig = networkElement.firstChildElement("WifiConfiguration")
            ?: error("Missing <WifiConfiguration>")
        val rawSsid = wifiConfig.namedString("SSID")
            ?: error("Missing SSID")
        val ssid = rawSsid.unquoteAndroidString()
        val rawPsk = wifiConfig.namedString("PreSharedKey")
        val preSharedKey = rawPsk?.unquoteAndroidString()
        val isHidden = wifiConfig.namedBoolean("HiddenSSID") ?: false
        val macRandom = (wifiConfig.namedInt("MacRandomizationSetting") ?: 0) != 0

        val hasWepKey = networkElement
            .getElementsByTagName("WEPKeys")
            .toElementList()
            .any { it.textContent.isNotBlank() && it.textContent != "null" }

        val keyMgmtHex = wifiConfig.namedByteArray("AllowedKeyMgmt") ?: ""
        val keyMgmtBytes = keyMgmtHex.decodeHex()
        val security = WifiSecurityType.fromKeyMgmtBytes(keyMgmtBytes, hasWepKey)

        // a boolean field on A10+; fall-back: NetworkStatus/SelectionStatus
        val autoJoin = wifiConfig.namedBoolean("AutoJoinEnabled")
            ?: parseAutoJoinFromNetworkStatus(networkElement)

        return WifiNetwork(
            ssid = ssid,
            preSharedKey = preSharedKey,
            isHidden = isHidden,
            security = security,
            autoJoin = autoJoin,
            isMacRandomized = macRandom,
        )
    }

    private fun parseAutoJoinFromNetworkStatus(networkElement: Element): Boolean {
        val status = networkElement
            .firstChildElement("NetworkStatus")
            ?.namedString("SelectionStatus")
            ?: return true
        return status == "NETWORK_SELECTION_ENABLED"
    }

    private fun String.unquoteAndroidString(): String =
        if (startsWith('"') && endsWith('"') && length >= 2) substring(1, length - 1) else this

    // decodes a compact hex string (e.g. `"0a2f"`) to a ByteArray
    private fun String.decodeHex(): ByteArray {
        if (isBlank()) return ByteArray(0)
        require(length % 2 == 0) { "Hex string has odd length: '$this'" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
