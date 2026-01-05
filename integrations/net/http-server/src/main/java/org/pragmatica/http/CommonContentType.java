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

package org.pragmatica.http;
/// Common HTTP content types.
public enum CommonContentType implements ContentType {
    TEXT_PLAIN("text/plain; charset=UTF-8", ContentCategory.TEXT),
    TEXT_HTML("text/html; charset=UTF-8", ContentCategory.TEXT),
    TEXT_CSS("text/css; charset=UTF-8", ContentCategory.TEXT),
    TEXT_JAVASCRIPT("text/javascript; charset=UTF-8", ContentCategory.TEXT),
    APPLICATION_JSON("application/json; charset=UTF-8", ContentCategory.JSON),
    APPLICATION_XML("application/xml; charset=UTF-8", ContentCategory.XML),
    APPLICATION_OCTET_STREAM("application/octet-stream", ContentCategory.BINARY),
    IMAGE_PNG("image/png", ContentCategory.BINARY),
    IMAGE_JPEG("image/jpeg", ContentCategory.BINARY),
    IMAGE_SVG("image/svg+xml", ContentCategory.XML);
    private final String headerText;
    private final ContentCategory category;
    CommonContentType(String headerText, ContentCategory category) {
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
