package org.pragmatica.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HierarchyScannerTest {

    // Test hierarchy for sealed interface scanning - records nested inside their interfaces
    sealed interface Message permits Request, Response {}

    sealed interface Request extends Message {
        record Get(String path) implements Request {}
        record Post(String path, String body) implements Request {}
    }

    sealed interface Response extends Message {
        record Success(int code, String body) implements Response {}
        record Error(int code, String message) implements Response {}
    }

    @Test
    void concreteSubtypes_finds_all_implementations_of_sealed_interface() {
        var subtypes = HierarchyScanner.concreteSubtypes(Message.class);

        assertThat(subtypes).hasSize(4);
        assertThat(subtypes).contains(
            Request.Get.class,
            Request.Post.class,
            Response.Success.class,
            Response.Error.class
        );
    }

    @Test
    void concreteSubtypes_finds_implementations_of_nested_sealed_interface() {
        var subtypes = HierarchyScanner.concreteSubtypes(Request.class);

        assertThat(subtypes).hasSize(2);
        assertThat(subtypes).contains(Request.Get.class, Request.Post.class);
    }

    @Test
    void concreteSubtypes_returns_class_itself_for_non_interface() {
        var subtypes = HierarchyScanner.concreteSubtypes(Request.Get.class);

        assertThat(subtypes).hasSize(1);
        assertThat(subtypes).contains(Request.Get.class);
    }

    @Test
    void walkUpTheTree_collects_hierarchy_from_concrete_classes() {
        var classes = HierarchyScanner.walkUpTheTree(List.of(Request.Get.class, Response.Success.class));

        // Should include the classes themselves plus all related types from their hierarchies
        assertThat(classes).contains(Request.Get.class, Response.Success.class);
        assertThat(classes.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void walkUpTheTree_handles_empty_input() {
        var classes = HierarchyScanner.walkUpTheTree(List.of());

        assertThat(classes).isEmpty();
    }

    @Test
    void walkUpTheTree_deduplicates_when_classes_share_hierarchy() {
        var classes = HierarchyScanner.walkUpTheTree(List.of(Request.Get.class, Request.Post.class));

        // Both share Request interface, should not have duplicates
        long uniqueCount = classes.stream().distinct().count();
        assertThat(classes).hasSize((int) uniqueCount);
    }
}
