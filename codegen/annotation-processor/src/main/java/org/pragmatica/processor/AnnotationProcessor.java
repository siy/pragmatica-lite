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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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

    private void processRecords(RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(Template.class);
        var types = ElementFilter.typesIn(annotated);

        for (var type : types) {
            processingEnv.getMessager().printMessage(NOTE, "Found annotated type: %s".formatted(type));
//            System.out.printf("Type: %s, %s%n", type, type.getQualifiedName());
//            type.getQualifiedName();

            var imports = new TreeSet<String>();
            var components = new ArrayList<RecordComponent>();
            var templateName = type.getQualifiedName().toString() + "Template";
            var shortName = type.getSimpleName().toString();

            for (var component : type.getRecordComponents()) {
//                System.out.printf("Component: %s, type: %s%n", component, component.asType());

                var typeContext = TypeContext.from(component.asType());

                imports.addAll(typeContext.includes());
                components.add(new RecordComponent(component.toString(), typeContext.type()));

//                System.out.println(typeContext);
            }
//            System.out.println("Imports: " + imports);
//            System.out.println("Components: " + components);
            var recordType = new RecordType(type.toString(), components, imports, templateName, shortName);
//            System.out.println("Record type: " + recordType);

            var code = recordType.generateCode();
            System.out.println("Generated code: " + code);
        }
    }

    private record TypeContext(String type, List<String> includes) {
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
            var includes = classList.stream()
                                    .map(TypeContext::asImport)
                                    .toList();
            var typeText = typeString;

            for (var prefix : prefixes) {
                typeText = typeText.replace(prefix, "");
            }

            return new TypeContext(typeText, includes);
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
                
                /**
                 * Generated by annotation processor
                 */                
                public interface %sTemplate extends RecordTemplate<%s> {
                    %sTemplate INSTANCE = new %sTemplate() {};
                            
                    static %sBuilder builder() {
                        return %s -> new %s(%s);
                    }
                    
                    interface %sBuilder {
                        SrcUrl id(String id);
                    
                        interface SrcUrl {
                            Created srcUrl(String srcUrl);
                        }
                    
                        interface Created {
                            LastAccessed created(LocalDateTime created);
                        }
                    
                        interface LastAccessed {
                            ShortenedUrl lastAccessed(LocalDateTime lastAccessed);
                        }
                    }
                    
                    @Override
                    default Result<ShortenedUrl> load(String prefix, KeyToValue mapping) {
                        return Result.all(mapping.get(prefix, "id", new TypeToken<String>() {}),
                                          mapping.get(prefix, "srcUrl", new TypeToken<String>() {}),
                                          mapping.get(prefix, "created", new TypeToken<LocalDateTime>() {}),
                                          mapping.get(prefix, "lastAccessed", new TypeToken<LocalDateTime>() {}))
                                     .map(ShortenedUrl::new);
                    }
                    
                    
                    @Override
                    default FieldNames fieldNames() {
                        return () -> FORMATTED_NAMES;
                    }
                    
                    @Override
                    default List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, ShortenedUrl>>> extractors() {
                        return VALUE_EXTRACTORS;
                    }
                    
                    List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, ShortenedUrl>>> VALUE_EXTRACTORS = List.of(
                        tuple("id", new TypeToken<String>() {}, ShortenedUrl::id),
                        tuple("srcUrl", new TypeToken<String>() {}, ShortenedUrl::srcUrl),
                        tuple("created", new TypeToken<LocalDateTime>() {}, ShortenedUrl::created),
                        tuple("lastAccessed", new TypeToken<LocalDateTime>() {}, ShortenedUrl::lastAccessed)
                    );
                    
                    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
                }
                """.formatted(
                    templateName().substring(0, templateName().lastIndexOf(".")), // package
                    imports().stream().map("import %s;"::formatted).collect(Collectors.joining("\n")), // imports
                    shortName(), shortName(), // template name, RecordTemplate type parameter
                    shortName(), shortName(), // instance type, instance type
                    shortName(), //builder name
                    connectedViaArrow, shortName(), connectedViaComma // builder parameters, record type, constructor parameters
                );

        }
    }
    /*

     */
}