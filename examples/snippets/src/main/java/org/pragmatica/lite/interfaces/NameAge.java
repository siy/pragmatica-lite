package org.pragmatica.lite.interfaces;

import org.pragmatica.lang.Option;

public record NameAge(String firstName, String lastName, Option<String> middleName, Option<Integer> age) {
    static NameAgeBuilderStage1 builder() {
        return firstName -> lastName -> middleName -> age -> new NameAge(firstName, lastName, middleName, age);
    }

    public interface NameAgeBuilderStage1 {
        NameAgeBuilderStage2 firstName(String firstName);
    }

    public interface NameAgeBuilderStage2 {
        NameAgeBuilderStage3 lastName(String lastName);
    }

    public interface NameAgeBuilderStage3 {
        NameAgeBuilderStage4 middleName(Option<String> middleName);

        default NameAgeBuilderStage4 withNoMiddleName() {
            return middleName(Option.none());
        }

        default NameAge withoutMiddleNameAndAge() {
            return middleName(Option.none()).withUnknownAge();
        }
    }

    public interface NameAgeBuilderStage4 {
        NameAge age(Option<Integer> age);

        default NameAge withUnknownAge() {
            return age(Option.none());
        }
    }
}
