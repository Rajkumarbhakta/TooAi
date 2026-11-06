package com.rkbapps.tooai.ui.screens.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrScanContentTest {

    @Test
    fun `parseWifi reads name, password, security and hidden flag`() {
        val wifi = parseWifi("WIFI:T:WPA;S:HomeNet_5G;P:secret123;H:true;;")!!
        assertEquals("HomeNet_5G", wifi.ssid)
        assertEquals("secret123", wifi.password)
        assertEquals(WifiSecurity.WPA, wifi.security)
        assertTrue(wifi.isHidden)
    }

    @Test
    fun `parseWifi unescapes semicolons, colons and backslashes`() {
        val wifi = parseWifi("""WIFI:S:Cafe\;Net\:2;T:WPA;P:pa\\ss\;word;;""")!!
        assertEquals("Cafe;Net:2", wifi.ssid)
        assertEquals("""pa\ss;word""", wifi.password)
    }

    @Test
    fun `parseWifi treats a missing or nopass type as open and drops the password`() {
        val open = parseWifi("WIFI:S:Guest;T:nopass;P:ignored;;")!!
        assertEquals(WifiSecurity.OPEN, open.security)
        assertNull(open.password)
        assertFalse(open.isHidden)
        assertEquals(WifiSecurity.OPEN, parseWifi("WIFI:S:Guest;;")!!.security)
    }

    @Test
    fun `parseWifi maps SAE to WPA3 and rejects other payloads`() {
        assertEquals(WifiSecurity.WPA3, parseWifi("WIFI:T:SAE;S:Mesh;P:longpassword;;")!!.security)
        assertNull(parseWifi("https://example.com"))
        assertNull(parseWifi("WIFI:T:WPA;P:nossid;;"))
        assertNull(parseWifi("WIFI:T:ADB;S:studio-abc;P:pairing;;"))
        assertNull(parseWifi(null))
    }

    @Test
    fun `maskWifiPassword hides only the password, escapes included`() {
        assertEquals(
            "WIFI:T:WPA;S:Home;P:••••••••;;",
            maskWifiPassword("""WIFI:T:WPA;S:Home;P:se\;cret;;""")
        )
    }

    @Test
    fun `parseContact reads MECARD with surname first`() {
        val contact = parseContact("MECARD:N:Doe,John;TEL:+919000012345;EMAIL:john@example.com;;")!!
        assertEquals("John Doe", contact.name)
        assertEquals("+919000012345", contact.phone)
        assertEquals("john@example.com", contact.email)
    }

    @Test
    fun `parseContact reads vCard, preferring FN and the first number`() {
        val vCard = """
            BEGIN:VCARD
            VERSION:3.0
            N:Doe;Jane;;;
            FN:Jane Doe
            ORG:Example Inc.
            TEL;TYPE=CELL:+1 555 0100
            TEL;TYPE=WORK:+1 555 0199
            EMAIL;TYPE=INTERNET:jane@example.com
            END:VCARD
        """.trimIndent()
        val contact = parseContact(vCard)!!
        assertEquals("Jane Doe", contact.name)
        assertEquals("+1 555 0100", contact.phone)
        assertEquals("jane@example.com", contact.email)
        assertEquals("Example Inc.", contact.company)
    }

    @Test
    fun `parseContact falls back to the structured vCard name`() {
        val contact = parseContact("BEGIN:VCARD\r\nN:Doe;Jane;;;\r\nEND:VCARD")!!
        assertEquals("Jane Doe", contact.name)
        assertNull(parseContact("just some text"))
    }
}
