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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Rudimentary annotation processor that generates RecordTemplate implementations for annotated records.
 */
//TODO: fix generation of load() for more than 9 parameters
//TODO: Generate With-er
//TODO: Generate CRUD Repo
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
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            processRecords(roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(ERROR, "Exception: occurred %s".formatted(e));
        }
        return true;    // claim annotations, do not let them be processed further
    }

    private void processRecords(RoundEnvironment roundEnv) throws IOException {
        var annotated = roundEnv.getElementsAnnotatedWith(Template.class);
        var types = ElementFilter.typesIn(annotated);

        for (var type : types) {
            processingEnv.getMessager().printMessage(NOTE, "Found annotated type: %s".formatted(type));

            var imports = new TreeSet<String>();
            var components = new ArrayList<RecordComponent>();
            var templateName = type.getQualifiedName().toString() + "Template";
            var shortName = type.getSimpleName().toString();

            for (var component : type.getRecordComponents()) {
                var typeContext = TypeContext.from(component.asType());

                imports.addAll(typeContext.imports());
                components.add(new RecordComponent(component.toString(), typeContext.type()));
            }

            // Add known imports
            imports.add("org.pragmatica.lang.Functions.Fn1");
            imports.add("org.pragmatica.lang.Result");
            imports.add("org.pragmatica.lang.Tuple.Tuple3");
            imports.add("org.pragmatica.lang.type.FieldNames");
            imports.add("org.pragmatica.lang.type.KeyToValue");
            imports.add("org.pragmatica.lang.type.RecordTemplate");
            imports.add("org.pragmatica.lang.type.TypeToken");
            imports.add("java.util.List");

            var recordType = new RecordType(type.toString(), components, imports, templateName, shortName);
            var code = recordType.generateCode();

            var builderFile = processingEnv.getFiler()
                                           .createSourceFile(templateName);

            try (var out = new PrintWriter(builderFile.openWriter())) {
                out.println(code);
            }
        }
    }

    private record TypeContext(String type, List<String> imports) {

        // Purely text-based conversion.
        // - extract prefixes from full type names
        // - generate list of imports
        // - remove prefixes from type names
        // Resulting type string contain only short type names
        public static TypeContext from(TypeMirror type) {
            var typeString = type.toString();

            if (type.getKind() != TypeKind.DECLARED) {
                return new TypeContext(typeString, List.of());
            }

            var classList = Stream.of(typeString.replace("<", ",")
                                                .split(","))
                                  .map(t -> t.replace(">", ""))
                                  .toList();

            var prefixes = classList.stream()
                                    .map(t -> t.substring(0, t.lastIndexOf(".") + 1))
                                    .toList();
            var imports = classList.stream()
                                   .map(TypeContext::asImport)
                                   .toList();
            var typeText = typeString;

            for (var prefix : prefixes) {
                typeText = typeText.replace(prefix, "");
            }

            return new TypeContext(typeText, imports);
        }

        private static String asImport(String t) {
            var prefix = t.substring(0, t.lastIndexOf("."));
            var lastElement = prefix.substring(prefix.lastIndexOf(".") + 1);

            return Character.isUpperCase(lastElement.charAt(0)) ? prefix + ".*" : t;
        }
    }

    record RecordComponent(String name, String type) {}

    record RecordType(String name, List<RecordComponent> components, Set<String> imports, String templateName, String shortName) {
        private String generateCode() {
            var connectedViaArrow = components().stream()
                                                .map(RecordComponent::name)
                                                .collect(Collectors.joining(" -> "));
            var connectedViaComma = components().stream()
                                                .map(RecordComponent::name)
                                                .collect(Collectors.joining(", "));


            return
                """
                package %s;
                                
                %s
                                
                %s
                                
                /**
                 * Generated by annotation processor
                 */
                public interface %sTemplate extends RecordTemplate<%s> {
                    %sTemplate INSTANCE = new %sTemplate() {};
                            
                    static %sBuilderStage1 builder() {
                        return %s -> new %s(%s);
                    }

                %s
                    @Override
                    default Result<%s> load(String prefix, KeyToValue mapping) {
                        return Result.all(
                %s)
                                     .map(%s::new);
                    }
                   
                    @Override
                    default FieldNames fieldNames() {
                        return () -> FORMATTED_NAMES;
                    }
                    
                    @Override
                    default List<Tuple3<String, TypeToken<?>, Fn1<?, %s>>> extractors() {
                        return VALUE_EXTRACTORS;
                    }
                    
                    List<Tuple3<String, TypeToken<?>, Fn1<?, %s>>> VALUE_EXTRACTORS = List.of(
                %s
                    );
                    
                    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
                }
                """.formatted(
                    templateName().substring(0, templateName().lastIndexOf(".")), // package
                    imports().stream().map("import %s;"::formatted).collect(Collectors.joining("\n")), // imports
                    "import static org.pragmatica.lang.Tuple.tuple;\n", // static imports
                    shortName(), shortName(), // template name, RecordTemplate type parameter
                    shortName(), shortName(), // instance type, instance type
                    shortName(), //builder name
                    connectedViaArrow, shortName(), connectedViaComma, // builder parameters, record type, constructor parameters
                    builderInterface(),
                    shortName(), //load type
                    buildGetters(), // load parameters
                    shortName(), // new instance type
                    shortName(), // new instance type
                    shortName(), // new instance type
                    buildTuples()
                );
        }

        private String builderInterface() {
            var stageFormat =
                """
                    interface %sBuilderStage%d {
                        %s
                    }
                    
                """;

            var cnt = 1;
            var componentIterator = components().iterator();

            var builderStages = new StringBuilder();

            while (componentIterator.hasNext()) {
                var component = componentIterator.next();
                var methodText = componentIterator.hasNext()
                                 ? "%sBuilderStage%d %s(%s %s);".formatted(shortName(), cnt + 1, component.name(), component.type(), component.name())
                                 : "%s %s(%s %s);".formatted(shortName(), component.name(), component.type(), component.name());
                var stage = stageFormat.formatted(shortName(), cnt, methodText);
                builderStages.append(stage);
                cnt++;
            }

            return builderStages.toString();
        }

        private String buildTuples() {
            var tuples = new StringBuilder();

            for (var component : components()) {
                var tuple = "        tuple(\"%s\", new TypeToken<%s>() {}, %s::%s),%n"
                    .formatted(component.name(), toObjectType(component.type()), shortName(), component.name());
                tuples.append(tuple);
            }
            tuples.setLength(tuples.length() - 2); // remove last comma

            return tuples.toString();
        }

        private String buildGetters() {
            var getters = new StringBuilder();

            for (var component : components()) {
                var getter = "                         mapping.get(prefix, \"%s\", new TypeToken<%s>() {}),%n"
                    .formatted(component.name(), toObjectType(component.type()));
                getters.append(getter);
            }

            getters.setLength(getters.length() - 2); // remove last comma

            return getters.toString();
        }
    }

    private static final Map<String, String> PRIMITIVE_TO_OBJECT = Map.of(
        "byte", "Byte",
        "char", "Character",
        "short", "Short",
        "int", "Integer",
        "long", "Long",
        "double", "Double",
        "float", "Float",
        "boolean", "Boolean"
    );

    private static String toObjectType(String type) {
        return PRIMITIVE_TO_OBJECT.getOrDefault(type, type);
    }
}
