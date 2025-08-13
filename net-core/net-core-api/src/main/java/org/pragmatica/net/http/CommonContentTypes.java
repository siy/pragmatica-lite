/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.net.http;

/// Common content types for HTTP requests and responses
public enum CommonContentTypes implements ContentType {
    // Text content types - decode as string
    TEXT_PLAIN("text/plain; charset=UTF-8", ContentCategory.PLAIN_TEXT),
    TEXT_CSS("text/css; charset=UTF-8", ContentCategory.PLAIN_TEXT),
    TEXT_JAVASCRIPT("text/javascript; charset=UTF-8", ContentCategory.PLAIN_TEXT),
    TEXT_CSV("text/csv; charset=UTF-8", ContentCategory.PLAIN_TEXT),

    // HTML content types - decode as string (can be parsed if needed)  
    TEXT_HTML("text/html; charset=UTF-8", ContentCategory.HTML),

    // JSON content types - deserialize using JSON parser
    APPLICATION_JSON("application/json; charset=UTF-8", ContentCategory.JSON),
    APPLICATION_JSON_UTF8("application/json; charset=UTF-8", ContentCategory.JSON),
    APPLICATION_LD_JSON("application/ld+json; charset=UTF-8", ContentCategory.JSON),

    // XML content types - deserialize using XML parser
    APPLICATION_XML("application/xml; charset=UTF-8", ContentCategory.XML),
    TEXT_XML("text/xml; charset=UTF-8", ContentCategory.XML),
    APPLICATION_SOAP_XML("application/soap+xml; charset=UTF-8", ContentCategory.XML),
    IMAGE_SVG("image/svg+xml", ContentCategory.XML), // SVG is XML-based

    // Form data content types - deserialize using form parser
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded", ContentCategory.FORM_DATA),
    MULTIPART_FORM_DATA("multipart/form-data", ContentCategory.FORM_DATA),
    MULTIPART_MIXED("multipart/mixed", ContentCategory.FORM_DATA),

    // Binary content types - handle as byte array
    APPLICATION_OCTET_STREAM("application/octet-stream", ContentCategory.BINARY),
    APPLICATION_PDF("application/pdf", ContentCategory.BINARY),
    APPLICATION_ZIP("application/zip", ContentCategory.BINARY),
    APPLICATION_GZIP("application/gzip", ContentCategory.BINARY),
    APPLICATION_JAVASCRIPT("application/javascript", ContentCategory.BINARY), // Binary JS files
    APPLICATION_WASM("application/wasm", ContentCategory.BINARY),
    APPLICATION_PROTOBUF("application/x-protobuf", ContentCategory.BINARY),

    // Image content types - binary data
    IMAGE_JPEG("image/jpeg", ContentCategory.BINARY),
    IMAGE_PNG("image/png", ContentCategory.BINARY),
    IMAGE_GIF("image/gif", ContentCategory.BINARY),
    IMAGE_WEBP("image/webp", ContentCategory.BINARY),

    // Audio content types - binary data
    AUDIO_MPEG("audio/mpeg", ContentCategory.BINARY),
    AUDIO_WAV("audio/wav", ContentCategory.BINARY),
    AUDIO_OGG("audio/ogg", ContentCategory.BINARY),

    // Video content types - binary data
    VIDEO_MP4("video/mp4", ContentCategory.BINARY),
    VIDEO_WEBM("video/webm", ContentCategory.BINARY),
    VIDEO_OGG("video/ogg", ContentCategory.BINARY),

    ;

    private final String headerText;
    private final ContentCategory category;

    CommonContentTypes(String headerText, ContentCategory category) {
        this.headerText = headerText;
        this.category = category;
    }

    @Override
    public String headerText() {
        return headerText;
    }

    @Override
    public ContentCategory category() {
        return category;
    }
}