package org.pragmatica.config.format.conf;

import org.pragmatica.config.api.FormatReader;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;

import java.util.List;

/**
 * CONF config format reader. The CONG format can be considered a simple version of TOML or a variation of Windows INI.
 * <p>
 * The file consists of sections, each section starts with a section name in square brackets, followed by a list of key-value pairs. The key-value
 * pairs are separated by an equal sign, and the key is separated from the value by a equals sign. The key-value pairs are separated by a newline.
 * Logically the section name is a common prefix to all variables in the section. The line starting with a hash sign is a comment.
 */
public class ConfFormatReader implements FormatReader {
    @Override
    public Result<StringMap> read(String content) {
        return ConfParser.parse(content);
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("cfg", "ini", "conf");
    }
}
