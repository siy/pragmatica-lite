package org.pragmatica.http.example;

public interface NameAndAgeBuilder {
    static NameAndAgeBuilder builder() {
        return name -> age -> () -> new NameAndAge(name, age);
    }

    Stage1 name(String name);

    interface Stage1 {
        Stage2 age(int age);
    }

    interface Stage2 {
        NameAndAge build();
    }
}
