/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.http.model;

/// Common content types (MIME types) as defined in various RFCs.
public enum CommonContentType implements ContentType {
    // Text types (RFC 2046, RFC 6657)
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    TEXT_CSS("text/css"),
    TEXT_JAVASCRIPT("text/javascript"),
    TEXT_CSV("text/csv"),
    TEXT_XML("text/xml"),

    // Application types (RFC 4627, RFC 7159, RFC 8259)
    APPLICATION_JSON("application/json"),
    APPLICATION_XML("application/xml"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    APPLICATION_PDF("application/pdf"),
    APPLICATION_ZIP("application/zip"),
    APPLICATION_GZIP("application/gzip"),
    APPLICATION_OCTET_STREAM("application/octet-stream"),
    APPLICATION_JAVASCRIPT("application/javascript"),
    APPLICATION_LD_JSON("application/ld+json"),
    APPLICATION_MSGPACK("application/msgpack"),
    APPLICATION_PROTOBUF("application/protobuf"),
    APPLICATION_GRAPHQL("application/graphql"),

    // Form data types (RFC 2388, RFC 1867)
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART_FORM_DATA("multipart/form-data"),

    // Image types (RFC 2046)
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_GIF("image/gif"),
    IMAGE_SVG_XML("image/svg+xml"),
    IMAGE_WEBP("image/webp"),
    IMAGE_BMP("image/bmp"),
    IMAGE_TIFF("image/tiff"),
    IMAGE_ICO("image/x-icon"),

    // Audio types (RFC 2046)
    AUDIO_MPEG("audio/mpeg"),
    AUDIO_OGG("audio/ogg"),
    AUDIO_WAV("audio/wav"),
    AUDIO_WEBM("audio/webm"),

    // Video types (RFC 2046)
    VIDEO_MP4("video/mp4"),
    VIDEO_MPEG("video/mpeg"),
    VIDEO_OGG("video/ogg"),
    VIDEO_WEBM("video/webm"),

    // Font types
    FONT_WOFF("font/woff"),
    FONT_WOFF2("font/woff2"),
    FONT_TTF("font/ttf"),
    FONT_OTF("font/otf"),

    // RFC 7807 - Problem Details
    APPLICATION_PROBLEM_JSON("application/problem+json"),
    APPLICATION_PROBLEM_XML("application/problem+xml"),

    // Streaming
    TEXT_EVENT_STREAM("text/event-stream"),
    APPLICATION_STREAM("application/stream+json");

    private final String mimeType;

    CommonContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String mimeType() {
        return mimeType;
    }

    /// Find a common content type by MIME type string.
    ///
    /// @param mimeType MIME type to look up
    /// @return matching enum constant, or null if not found
    public static CommonContentType fromString(String mimeType) {
        for (CommonContentType type : values()) {
            if (type.mimeType.equals(mimeType)) {
                return type;
            }
        }
        return null;
    }
}
