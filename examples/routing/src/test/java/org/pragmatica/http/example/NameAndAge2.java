package org.pragmatica.http.example;

public record NameAndAge2(String name, int age) {
    public static NameAndAgeBuilder builder() {
        return name -> age -> new NameAndAge2(name, age);
    }

    public interface NameAndAgeBuilder {
        Stage1 name(String name);

        interface Stage1 {
            NameAndAge2 age(int age);
        }
    }
}
