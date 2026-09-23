package com.pavitraristaa.media.mapper;

import com.pavitraristaa.media.dto.MediaFileResponse;
import com.pavitraristaa.media.entity.MediaFile;
import com.pavitraristaa.media.service.MediaUrlResolver;
import org.springframework.stereotype.Component;

@Component
public class MediaFileMapper {

    private final MediaUrlResolver mediaUrlResolver;

    public MediaFileMapper(MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public MediaFileResponse toResponse(MediaFile media) {
        return new MediaFileResponse(
                media.getUuid(),
                media.getOriginalFilename(),
                media.getMimeType(),
                media.getFileSizeBytes(),
                mediaUrlResolver.urlFor(media),
                media.getStatus().name()
        );
    }
}
