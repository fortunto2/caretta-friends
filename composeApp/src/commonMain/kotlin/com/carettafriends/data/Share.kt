package com.carettafriends.data

/** Open the OS share sheet with [text] (motivational "I'm a volunteer" card). Platform-specific. */
expect fun platformShare(text: String)

/** Watermark the photo at [srcPath] with [caption] (nest code · date · brand) and share the image. */
expect fun platformShareImage(srcPath: String, caption: String)

/** Render a simple text report ([title] + [lines]) to a one-page PDF ([fileName].pdf) and open the OS
 *  share sheet. Used for the excavation record (tutanak) a volunteer hands to the coordinator. */
expect fun platformSharePdf(fileName: String, title: String, lines: List<String>)
