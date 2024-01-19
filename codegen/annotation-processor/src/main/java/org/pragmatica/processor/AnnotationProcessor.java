package org.pragmatica.processor;

import com.google.auto.service.AutoService;
import org.pragmatica.annotation.Template;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

@SupportedAnnotationTypes("org.pragmatica.annotation.*")
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(NOTE, "AnnotationProcessor initialized");
        System.out.println("AnnotationProcessor.init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            System.out.println("AnnotationProcessor.process");
            processRecords(roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(ERROR, "Exception: occurred %s".formatted(e));
        }
        return true;    // claim annotations, do not let them be processed further
    }

    private void processRecords(RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(Template.class);
        var types = ElementFilter.typesIn(annotated);

        for (var type : types) {
            processingEnv.getMessager().printMessage(NOTE, "Found annotated type: %s".formatted(type));

            for (var component : type.getRecordComponents()) {
                System.out.printf("Component: %s, type: %s%n", component, component.asType());

                var typeContext = TypeContext.from(component.asType());

                System.out.println(typeContext);
            }
        }
    }

    private record TypeContext(String type, List<String> includes) {
        public static TypeContext from(TypeMirror type) {
            var typeString = type.toString();

            if (type.getKind() != TypeKind.DECLARED) {
                return new TypeContext(typeString, List.of());
            }

            //TODO: use recursive parsing
            var classList = Stream.of(typeString.replace("<", ",")
                                                .split(","))
                                  .map(t -> t.replace(">", ""))
                                  .toList();

            var typeText = classList.getFirst().substring(classList.getFirst().lastIndexOf(".") + 1);
            var includes = classList.stream()
                                    .map(TypeContext::asImport)
                                    .toList();

            return new TypeContext(typeText, includes);
        }

        private static String asImport(String t) {
            var prefix = t.substring(0, t.lastIndexOf("."));
            var lastElement = prefix.substring(prefix.lastIndexOf(".") + 1);

            return Character.isUpperCase(lastElement.charAt(0)) ? prefix + ".*" : t;
        }

        private static String simpleName(String source, List<String> collectedImports) {
            var split = source.split("<", 2);

            split[0] = split[0].substring(split[0].lastIndexOf(".") + 1);

            if (split.length == 1) {
                return split[0].trim();
            }

            var type = split[0];
            collectedImports.add(asImport(type));

            var rest = simpleName(split[1], collectedImports);

            if (rest.isEmpty()) {
                return type;
            }
            //TODO: finish this
            return null;
        }
    }
}