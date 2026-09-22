package com.pavitraristaa.media.service;

import com.pavitraristaa.media.entity.MediaFile;

/**
 * Lets other features declare that they still reference a media file, so media can refuse to delete it
 * without depending on those features.
 */
public interface MediaUsageChecker {

    boolean isInUse(MediaFile media);
}
