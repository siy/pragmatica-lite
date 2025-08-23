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

import java.nio.charset.StandardCharsets;

/// Common HTTP content types used in web applications and APIs.
/// Provides convenient constants for frequently used media types.
public final class CommonContentTypes {
    
    // === JSON Content Types ===
    
    /// Standard JSON content type
    public static final ContentType APPLICATION_JSON = ContentType.of("application", "json");
    
    /// JSON with UTF-8 charset
    public static final ContentType APPLICATION_JSON_UTF8 = APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);
    
    /// JSON-LD (Linked Data) content type
    public static final ContentType APPLICATION_LD_JSON = ContentType.of("application", "ld+json");
    
    // === XML Content Types ===
    
    /// Standard XML content type
    public static final ContentType APPLICATION_XML = ContentType.of("application", "xml");
    
    /// XML with UTF-8 charset
    public static final ContentType APPLICATION_XML_UTF8 = APPLICATION_XML.withCharset(StandardCharsets.UTF_8);
    
    /// Text XML content type
    public static final ContentType TEXT_XML = ContentType.of("text", "xml");
    
    /// Text XML with UTF-8 charset
    public static final ContentType TEXT_XML_UTF8 = TEXT_XML.withCharset(StandardCharsets.UTF_8);
    
    // === Text Content Types ===
    
    /// Plain text content type
    public static final ContentType TEXT_PLAIN = ContentType.of("text", "plain");
    
    /// Plain text with UTF-8 charset
    public static final ContentType TEXT_PLAIN_UTF8 = TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);
    
    /// HTML content type
    public static final ContentType TEXT_HTML = ContentType.of("text", "html");
    
    /// HTML with UTF-8 charset
    public static final ContentType TEXT_HTML_UTF8 = TEXT_HTML.withCharset(StandardCharsets.UTF_8);
    
    /// CSS content type
    public static final ContentType TEXT_CSS = ContentType.of("text", "css");
    
    /// JavaScript content type
    public static final ContentType TEXT_JAVASCRIPT = ContentType.of("text", "javascript");
    
    /// CSV content type
    public static final ContentType TEXT_CSV = ContentType.of("text", "csv");
    
    // === Form Content Types ===
    
    /// URL-encoded form data
    public static final ContentType APPLICATION_FORM_URLENCODED = ContentType.of("application", "x-www-form-urlencoded");
    
    /// Multipart form data
    public static final ContentType MULTIPART_FORM_DATA = ContentType.of("multipart", "form-data");
    
    // === Binary Content Types ===
    
    /// Generic binary content
    public static final ContentType APPLICATION_OCTET_STREAM = ContentType.of("application", "octet-stream");
    
    /// PDF content type
    public static final ContentType APPLICATION_PDF = ContentType.of("application", "pdf");
    
    /// ZIP archive content type
    public static final ContentType APPLICATION_ZIP = ContentType.of("application", "zip");
    
    /// GZIP compressed content type
    public static final ContentType APPLICATION_GZIP = ContentType.of("application", "gzip");
    
    // === Image Content Types ===
    
    /// JPEG image content type
    public static final ContentType IMAGE_JPEG = ContentType.of("image", "jpeg");
    
    /// PNG image content type
    public static final ContentType IMAGE_PNG = ContentType.of("image", "png");
    
    /// GIF image content type
    public static final ContentType IMAGE_GIF = ContentType.of("image", "gif");
    
    /// SVG image content type
    public static final ContentType IMAGE_SVG_XML = ContentType.of("image", "svg+xml");
    
    /// WebP image content type
    public static final ContentType IMAGE_WEBP = ContentType.of("image", "webp");
    
    // === Audio/Video Content Types ===
    
    /// MP3 audio content type
    public static final ContentType AUDIO_MPEG = ContentType.of("audio", "mpeg");
    
    /// MP4 video content type
    public static final ContentType VIDEO_MP4 = ContentType.of("video", "mp4");
    
    /// WebM video content type
    public static final ContentType VIDEO_WEBM = ContentType.of("video", "webm");
    
    // === Special Content Types ===
    
    /// Wildcard - matches any content type
    public static final ContentType ALL = ContentType.of("*", "*");
    
    // Utility class - prevent instantiation
    private CommonContentTypes() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}