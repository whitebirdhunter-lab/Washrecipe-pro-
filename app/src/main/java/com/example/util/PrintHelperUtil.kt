package com.example.util

import android.content.Context
import androidx.print.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrintHelperUtil {

    /**
     * Opens Android's system print dialog using PrintHelper.
     * Allows selecting real physical printers, network printers, or "Save as PDF",
     * with paper size (e.g. A4, Letter, 4x6 photo paper) and orientation options.
     */
    suspend fun printPhoto(
        context: Context,
        filePath: String,
        photoName: String,
        scaleFit: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!PrintHelper.systemSupportsPrint()) {
                return@withContext Result.failure(Exception("Printing is not supported on this device."))
            }

            val bitmap = PhotoFileUtils.decodeBitmapForPrinting(filePath)
                ?: return@withContext Result.failure(Exception("Could not prepare image data for printing."))

            withContext(Dispatchers.Main) {
                val printHelper = PrintHelper(context).apply {
                    scaleMode = if (scaleFit) PrintHelper.SCALE_MODE_FIT else PrintHelper.SCALE_MODE_FILL
                    colorMode = PrintHelper.COLOR_MODE_COLOR
                }
                val jobName = "PhotoArchive_${photoName.replace(" ", "_")}"
                printHelper.printBitmap(jobName, bitmap)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
