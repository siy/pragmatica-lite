package org.pragmatica.example;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RecordBuilder.Template(options = @RecordBuilder.Options(
addClassRetainedGenerated = false,
addFunctionalMethodsToWith = true,
addStaticBuilder = false,
enableWither = false
))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface MyCoRecordBuilder {
}