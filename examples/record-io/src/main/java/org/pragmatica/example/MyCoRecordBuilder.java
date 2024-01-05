package org.pragmatica.example;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RecordBuilder.Template(options = @RecordBuilder.Options(
addClassRetainedGenerated = false,
addStaticBuilder = false,
addFunctionalMethodsToWith = true,
enableWither = false,
builderMode = RecordBuilder.BuilderMode.STAGED,
stagedBuilderMethodName = "builder"
))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface MyCoRecordBuilder {
}