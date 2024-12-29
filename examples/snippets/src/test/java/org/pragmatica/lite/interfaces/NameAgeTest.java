package org.pragmatica.lite.interfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

import org.junit.jupiter.api.Test;

class NameAgeTest {

    @Test
    void fullNameAgeAndMiddleName() {
        var nameAge = NameAge.builder()
                             .firstName("John")
                             .lastName("Doe")
                             .middleName(some("Smith"))
                             .age(some(30));
        assertEquals("John", nameAge.firstName());
        assertEquals("Doe", nameAge.lastName());
        assertEquals(some("Smith"), nameAge.middleName());
        assertEquals(some(30), nameAge.age());
    }

    @Test
    void withoutMiddleNameAndAge() {
        var nameAge = NameAge.builder()
                             .firstName("John")
                             .lastName("Doe")
                             .withoutMiddleNameAndAge();

        assertEquals("John", nameAge.firstName());
        assertEquals("Doe", nameAge.lastName());
        assertEquals(none(), nameAge.middleName());
        assertEquals(none(), nameAge.age());
    }

    @Test
    void withUnknownAge() {
        var nameAge = NameAge.builder()
                             .firstName("John")
                             .lastName("Doe")
                             .middleName(some("Smith"))
                             .withUnknownAge();
        assertEquals("John", nameAge.firstName());
        assertEquals("Doe", nameAge.lastName());
        assertEquals(some("Smith"), nameAge.middleName());
        assertEquals(none(), nameAge.age());
    }

    @Test
    void withoutMiddleName() {
        var nameAge = NameAge.builder()
                             .firstName("John")
                             .lastName("Doe")
                             .withNoMiddleName()
                             .age(some(42));

        assertEquals("John", nameAge.firstName());
        assertEquals("Doe", nameAge.lastName());
        assertEquals(none(), nameAge.middleName());
        assertEquals(some(42), nameAge.age());
    }
}