package com.carettafriends.data

/** Open the OS share sheet with [text] (motivational "I'm a volunteer" card). Platform-specific. */
expect fun platformShare(text: String)

/** Watermark the photo at [srcPath] with [caption] (nest code · date · brand) and share the image. */
expect fun platformShareImage(srcPath: String, caption: String)
