package com.darki.dex.host.capture

import android.media.MediaCodecInfo
import android.media.MediaCodecList

object MediaCodecListHelper {
    fun findEncoder(mime: String): MediaCodecInfo? =
        MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .firstOrNull { info ->
                info.isEncoder &&
                    info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                    info.getCapabilitiesForType(mime).colorFormats.any { format ->
                        format == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    }
            }
}
