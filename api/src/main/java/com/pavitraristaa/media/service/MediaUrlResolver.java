package com.pavitraristaa.media.service;

import com.pavitraristaa.config.PavitraProperties;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.entity.MediaStatus;
import com.pavitraristaa.media.storage.ObjectStorage;
import org.springframework.stereotype.Component;

/**
 * Turns stored media into short-lived read URLs. Objects stay private in storage.
 */
@Component
public class MediaUrlResolver {

    private final ObjectStorage objectStorage;
    private final PavitraProperties properties;

    public MediaUrlResolver(ObjectStorage objectStorage, PavitraProperties properties) {
        this.objectStorage = objectStorage;
        this.properties = properties;
    }

    /** Returns null when the media is not readable or storage is not configured. */
    public String urlFor(MediaFile media) {
        if (media == null || media.getStatus() != MediaStatus.ACTIVE || !objectStorage.isConfigured()) {
            return null;
        }
        return objectStorage.createDownloadUrl(
                media.getBucket(), media.getObjectKey(), properties.getMedia().getDownloadUrlTtl());
    }
}
