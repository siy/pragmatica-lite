package org.pragmatica.processor;

import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.util.List;

//@Disabled
public class ProcTest {
    @Test
    public void runProcessor() {
        var args = List.of(
            "--enable-preview",
            "--release",
            "21",
            "-processor",
            AnnotationProcessor.class.getName(),
            "/home/siy/IdeaProjects/PFJ/pragmatica-lite/codegen/annotations/src/main/java/org/pragmatica/annotation/Template.java",
            "/home/siy/IdeaProjects/PFJ/pragmatica-lite/codegen/annotation-processor-tests/src/test/java/org/pragmatica/processor/record/NameAge.java"
        );

        ToolProvider.getSystemJavaCompiler()
                    .run(System.in, System.out, System.err,  args.toArray(new String[0]));
    }
}