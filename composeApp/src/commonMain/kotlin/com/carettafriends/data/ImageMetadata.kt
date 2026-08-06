package com.carettafriends.data

/**
 * Strip the metadata out of a JPEG before it leaves the phone.
 *
 * A nest photo carries EXIF from the camera — GPS to six decimals, the timestamp, the device. The
 * app already has all of that as *nest data*: the coordinate is a field on the record, where it is
 * covered by the same access rules as the rest of the nest. Keeping a second copy inside the image
 * is how a photo forwarded, exported or cached somewhere quietly becomes a map to a protected
 * nesting beach. So the shared copy carries pixels only; the phone's own file keeps its EXIF.
 *
 * Removes APP1 (EXIF and XMP), APP13 (IPTC/Photoshop) and comment segments, and leaves APP0 (JFIF)
 * and APP2 (ICC colour profile) alone so the image still renders correctly. Anything that isn't a
 * JPEG is returned untouched — better an unstripped upload than a corrupted one.
 */
fun stripImageMetadata(bytes: ByteArray): ByteArray {
    if (bytes.size < 4) return bytes
    // SOI
    if (bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return bytes

    val out = ArrayList<Byte>(bytes.size)
    out.add(bytes[0])
    out.add(bytes[1])

    var i = 2
    while (i + 3 < bytes.size) {
        if (bytes[i] != 0xFF.toByte()) return bytes          // not a marker where one must be
        val marker = bytes[i + 1].toInt() and 0xFF
        // Start of scan: everything after this is compressed image data — copy it verbatim.
        if (marker == 0xDA) {
            for (j in i until bytes.size) out.add(bytes[j])
            return out.toByteArray()
        }
        val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
        if (length < 2 || i + 2 + length > bytes.size) return bytes   // malformed; don't touch it
        val drop = marker == 0xE1 || marker == 0xED || marker == 0xFE // EXIF/XMP · IPTC · comment
        if (!drop) {
            for (j in i until i + 2 + length) out.add(bytes[j])
        }
        i += 2 + length
    }
    return out.toByteArray()
}
