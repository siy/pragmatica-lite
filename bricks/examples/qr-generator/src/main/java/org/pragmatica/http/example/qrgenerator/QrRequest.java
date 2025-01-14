package org.pragmatica.http.example.qrgenerator;

import org.pragmatica.annotation.Template;

@Template
public record QrRequest(String urlToEmbed) {}
