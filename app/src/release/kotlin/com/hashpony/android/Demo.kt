package com.hashpony.android

import android.content.Context
import android.content.Intent
import com.hashpony.android.ui.files.FilesViewModel
import com.hashpony.android.ui.text.TextViewModel
import com.hashpony.android.ui.verify.VerifyViewModel

/** Release builds have no screenshot demo; see src/debug for the real one. */
object Demo {
    @Suppress("UNUSED_PARAMETER")
    fun handle(
        context: Context,
        intent: Intent?,
        files: FilesViewModel,
        text: TextViewModel,
        verify: VerifyViewModel,
    ): String? = null
}
