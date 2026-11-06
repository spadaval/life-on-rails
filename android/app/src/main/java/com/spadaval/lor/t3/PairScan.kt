package com.spadaval.lor.t3

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Scanning T3's pairing QR code with Play services' code scanner (no camera permission), for the
 * agent page's pairing card and the T3 debug screen.
 */
object PairScan {
    /** Opens the scanner; [found] gets a pairing link that names its own server, [failed] why there is none. */
    fun scan(context: Context, found: (link: String, target: PairTarget) -> Unit, failed: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val link = barcode.rawValue.orEmpty()
                // A scanned link names its own server, so no typed address overrides it.
                runCatching { Pairing.parse("", link) }.fold(
                    { found(link, it) },
                    { failed("That QR code isn't a T3 pairing link.") },
                )
            }
            .addOnFailureListener { failed("Can't scan: ${it.message}") }
    }
}
