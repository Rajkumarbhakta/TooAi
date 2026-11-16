package com.rkbapps.tooai.ui.screens.barcode

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.google.mlkit.vision.barcode.common.Barcode
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.utils.copyText

// The kinds of scan the history can be filtered by. Anything else only shows under All.
enum class QrCategory(val label: String, val valueTypes: Set<Int>) {
    WIFI("Wi-Fi", setOf(Barcode.TYPE_WIFI)),
    LINKS("Links", setOf(Barcode.TYPE_URL)),
    CONTACTS(
        "Contacts",
        setOf(Barcode.TYPE_CONTACT_INFO, Barcode.TYPE_PHONE, Barcode.TYPE_EMAIL, Barcode.TYPE_SMS)
    ),
    TEXT("Text", setOf(Barcode.TYPE_TEXT));

    fun matches(scan: QrScan) = scan.valueType in valueTypes
}

data class QrType(val label: String, val title: String, @param:DrawableRes val icon: Int)

fun qrType(valueType: Int?): QrType = when (valueType) {
    Barcode.TYPE_WIFI -> QrType("Wi-Fi", "Wi-Fi network", R.drawable.wifi)
    Barcode.TYPE_URL -> QrType("Link", "Link", R.drawable.link)
    Barcode.TYPE_PHONE -> QrType("Phone", "Phone number", R.drawable.smartphone)
    Barcode.TYPE_EMAIL -> QrType("Email", "Email", R.drawable.mail)
    Barcode.TYPE_SMS -> QrType("SMS", "Text message", R.drawable.sms)
    Barcode.TYPE_CONTACT_INFO -> QrType("Contact", "Contact", R.drawable.contact)
    Barcode.TYPE_GEO -> QrType("Location", "Location", R.drawable.location)
    Barcode.TYPE_TEXT -> QrType("Text", "Text", R.drawable.notes)
    Barcode.TYPE_PRODUCT -> QrType("Product", "Product code", R.drawable.qr_code)
    Barcode.TYPE_ISBN -> QrType("ISBN", "Book (ISBN)", R.drawable.qr_code)
    Barcode.TYPE_CALENDAR_EVENT -> QrType("Event", "Calendar event", R.drawable.qr_code)
    Barcode.TYPE_DRIVER_LICENSE -> QrType("License", "Driver license", R.drawable.qr_code)
    else -> QrType("Code", "Scanned code", R.drawable.qr_code)
}

fun formatName(format: Int?): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR code"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    Barcode.FORMAT_AZTEC -> "Aztec"
    Barcode.FORMAT_PDF417 -> "PDF417"
    else -> "Barcode"
}

fun getFormatLabel(format: Int): String {
    return when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }
}

fun getValueTypeLabel(type: Int): String {
    return when (type) {
        Barcode.TYPE_CONTACT_INFO -> "CONTACT_INFO"
        Barcode.TYPE_EMAIL -> "EMAIL"
        Barcode.TYPE_ISBN -> "ISBN"
        Barcode.TYPE_PHONE -> "PHONE"
        Barcode.TYPE_PRODUCT -> "PRODUCT"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_TEXT -> "TEXT"
        Barcode.TYPE_URL -> "URL"
        Barcode.TYPE_WIFI -> "WIFI"
        Barcode.TYPE_GEO -> "GEO"
        Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR_EVENT"
        Barcode.TYPE_DRIVER_LICENSE -> "DRIVER_LICENSE"
        else -> "UNKNOWN"
    }
}

enum class WifiSecurity(val label: String) { OPEN("Open"), WEP("WEP"), WPA("WPA/WPA2"), WPA3("WPA3") }

data class WifiDetails(
    val ssid: String,
    val password: String?,
    val security: WifiSecurity,
    val isHidden: Boolean
)

// WIFI:T:WPA;S:name;P:secret;H:false;;  Backslash escapes ; , : and \ inside values.
fun parseWifi(raw: String?): WifiDetails? {
    if (raw == null || !raw.startsWith("WIFI:", ignoreCase = true)) return null
    val fields = parseFields(raw.substring(5))
    val ssid = fields["S"] ?: return null
    val security = when (fields["T"]?.uppercase()) {
        "WEP" -> WifiSecurity.WEP
        "WPA", "WPA2" -> WifiSecurity.WPA
        "SAE", "WPA3" -> WifiSecurity.WPA3
        null, "", "NOPASS" -> WifiSecurity.OPEN
        // Not a network. Android Studio's wireless debugging pairing code uses T:ADB, for one.
        else -> return null
    }
    return WifiDetails(
        ssid = ssid,
        password = fields["P"]?.takeIf { it.isNotEmpty() && security != WifiSecurity.OPEN },
        security = security,
        isHidden = fields["H"].equals("true", ignoreCase = true)
    )
}

private val wifiPassword = Regex("""(P:)((?:\\.|[^;])*)""", RegexOption.IGNORE_CASE)

fun maskWifiPassword(raw: String): String = raw.replace(wifiPassword) { "${it.groupValues[1]}••••••••" }

// Splits KEY:value;KEY:value; on unescaped semicolons. The first value wins for repeated keys.
private fun parseFields(body: String): Map<String, String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < body.length) {
        val c = body[i]
        when {
            c == '\\' && i + 1 < body.length -> {
                current.append(body[i + 1])
                i++
            }
            c == ';' -> {
                parts += current.toString()
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    if (current.isNotEmpty()) parts += current.toString()

    val fields = mutableMapOf<String, String>()
    parts.forEach { part ->
        val separator = part.indexOf(':')
        if (separator > 0) fields.putIfAbsent(part.substring(0, separator).uppercase(), part.substring(separator + 1))
    }
    return fields
}

data class ContactDetails(val name: String?, val phone: String?, val email: String?, val company: String?)

private val vCardLine = Regex("""^(FN|N|TEL|EMAIL|ORG)(?:;[^:]*)?:(.*)$""", RegexOption.IGNORE_CASE)

// Handles MECARD and vCard, the two contact formats QR generators use.
fun parseContact(raw: String?): ContactDetails? {
    if (raw == null) return null
    if (raw.startsWith("MECARD:", ignoreCase = true)) {
        val fields = parseFields(raw.substring(7))
        return ContactDetails(
            name = fields["N"]?.split(',')?.reversed()?.joinToString(" ")?.trim(),
            phone = fields["TEL"],
            email = fields["EMAIL"],
            company = fields["ORG"]
        )
    }
    if (raw.contains("BEGIN:VCARD", ignoreCase = true)) {
        val fields = mutableMapOf<String, String>()
        raw.lines().forEach { line ->
            vCardLine.find(line.trim())?.let { match ->
                fields.putIfAbsent(match.groupValues[1].uppercase(), match.groupValues[2].trim())
            }
        }
        val structuredName = fields["N"]?.split(';')?.let { parts ->
            listOfNotNull(parts.getOrNull(1), parts.getOrNull(0)).filter { it.isNotBlank() }.joinToString(" ")
        }
        return ContactDetails(
            name = fields["FN"] ?: structuredName,
            phone = fields["TEL"],
            email = fields["EMAIL"],
            company = fields["ORG"]?.replace(';', ' ')?.trim()
        )
    }
    return null
}

// The value a scan is about: the network name rather than the WIFI: payload, for example.
fun QrScan.title(): String {
    parseWifi(rawVale)?.let { return it.ssid }
    return displayValue?.takeIf { it.isNotBlank() } ?: rawVale.orEmpty()
}

private val urlPrefix = Regex("^(https?://)?(www\\.)?", RegexOption.IGNORE_CASE)

// For headings: links without their scheme, like the address bar shows them.
fun QrScan.shortTitle(): String =
    if (valueType == Barcode.TYPE_URL) title().replace(urlPrefix, "").ifEmpty { title() } else title()

data class QrAction(val label: String, @param:DrawableRes val icon: Int, val run: (Context) -> Unit)

// The one thing each kind of scan is for. Null means copying the value is the main action.
fun QrScan.primaryAction(): QrAction? {
    val raw = rawVale.orEmpty()
    val display = displayValue.orEmpty()
    return when (valueType) {
        Barcode.TYPE_WIFI -> parseWifi(raw)?.let { wifi ->
            QrAction("Connect", R.drawable.wifi) { connectToWifi(it, wifi) }
        }
        Barcode.TYPE_URL -> QrAction("Open", R.drawable.open_in_new) {
            val url = raw.ifBlank { display }
            val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(url)) url else "https://$url"
            it.launch(Intent(Intent.ACTION_VIEW, Uri.parse(withScheme)))
        }
        Barcode.TYPE_PHONE -> QrAction("Call", R.drawable.call) {
            val number = raw.removePrefix("tel:").removePrefix("TEL:").ifBlank { display }
            it.launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
        }
        Barcode.TYPE_EMAIL -> QrAction("Email", R.drawable.mail) { it.launch(emailIntent(raw, display)) }
        Barcode.TYPE_SMS -> QrAction("Message", R.drawable.sms) { it.launch(smsIntent(raw, display)) }
        Barcode.TYPE_GEO -> QrAction("Open map", R.drawable.location) {
            it.launch(Intent(Intent.ACTION_VIEW, Uri.parse(raw)))
        }
        Barcode.TYPE_CONTACT_INFO -> parseContact(raw)?.let { contact ->
            QrAction("Add contact", R.drawable.contact) { it.launch(addContactIntent(contact)) }
        }
        else -> null
    }
}

private fun emailIntent(raw: String, display: String): Intent {
    if (raw.startsWith("mailto:", ignoreCase = true)) return Intent(Intent.ACTION_SENDTO, Uri.parse(raw))
    val intent = Intent(Intent.ACTION_SENDTO)
    if (raw.startsWith("MATMSG:", ignoreCase = true)) {
        val fields = parseFields(raw.substring(7))
        intent.data = Uri.fromParts("mailto", fields["TO"].orEmpty(), null)
        fields["SUB"]?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
        fields["BODY"]?.let { intent.putExtra(Intent.EXTRA_TEXT, it) }
    } else {
        intent.data = Uri.fromParts("mailto", display, null)
    }
    return intent
}

// SMSTO:number:message, or an sms: URI.
private fun smsIntent(raw: String, display: String): Intent {
    if (raw.startsWith("SMSTO:", ignoreCase = true)) {
        val parts = raw.substring(6).split(':', limit = 2)
        return Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", parts[0], null)).apply {
            parts.getOrNull(1)?.let { putExtra("sms_body", it) }
        }
    }
    if (raw.startsWith("sms:", ignoreCase = true)) return Intent(Intent.ACTION_SENDTO, Uri.parse(raw))
    return Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", display, null))
}

private fun addContactIntent(contact: ContactDetails) =
    Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        contact.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        contact.phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
        contact.email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
        contact.company?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
    }

// Android 11+ adds the network through a system confirmation sheet, with no permission needed.
// Older versions, and WEP which suggestions don't support, copy the password and open Wi-Fi settings.
private fun connectToWifi(context: Context, wifi: WifiDetails) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifi.security != WifiSecurity.WEP) {
        try {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(wifi.ssid)
                .setIsHiddenSsid(wifi.isHidden)
                .apply {
                    when (wifi.security) {
                        WifiSecurity.WPA -> wifi.password?.let { setWpa2Passphrase(it) }
                        WifiSecurity.WPA3 -> wifi.password?.let { setWpa3Passphrase(it) }
                        else -> Unit
                    }
                }
                .build()
            val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
                .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // An invalid passphrase length throws here. Fall through to the settings page.
            Log.e("QrScan", "Wi-Fi suggestion failed", e)
        }
    }
    wifi.password?.let { context.copyText(it) }
    context.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
}

private fun Context.launch(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app can open this", Toast.LENGTH_SHORT).show()
    }
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.launch(Intent.createChooser(intent, "Share scan"))
}
