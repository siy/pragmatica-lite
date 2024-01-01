package org.pragmatica.http.example;

public interface NameAndAgeBuilder2 {
    static NameAndAgeBuilder2 builder() {
        return name -> age -> new NameAndAge(name, age);
    }

    Stage1 name(String name);

    interface Stage1 {
        NameAndAge age(int age);
    }
}
