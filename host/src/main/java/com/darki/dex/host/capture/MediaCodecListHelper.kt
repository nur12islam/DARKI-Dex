package com.darki.dex.host.capture

import android.media.MediaCodecInfo
import android.media.MediaCodecList

object MediaCodecListHelper {
    fun findEncoders(mime: String, width: Int, height: Int, frameRate: Int): List<MediaCodecInfo> =
        MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .filter { info ->
                info.isEncoder &&
                    info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                    runCatching {
                        val capabilities = info.getCapabilitiesForType(mime)
                        capabilities.colorFormats.any { format ->
                            format == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                        } && capabilities.videoCapabilities.areSizeAndRateSupported(width, height, frameRate.toDouble())
                    }.getOrDefault(false)
            }
            .sortedWith(
                compareByDescending<MediaCodecInfo> { it.isHardwareAccelerated }
                    .thenByDescending { it.isVendor }
                    .thenBy { it.name }
            )

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
