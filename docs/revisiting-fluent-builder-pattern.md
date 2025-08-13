### **Revisiting Fluent Builder Pattern**

Some time ago, I wrote a couple of articles ([here](https://medium.com/codex/unleashing-power-of-java-interfaces-21a21989777b) and [here](https://medium.com/@sergiy-yevtushenko/simple-implementation-of-fluent-builder-safe-alternative-to-traditional-builder-41a46e6de45b)) which mentioned a convenient and concise approach to writing Fluent (also called “Staged”) Builder in Java. But there was a question which always popped in comments&nbsp;—&nbsp;how to write Fluent Builder when some fields could be set to default. This article is the detailed answer to this question.

Let’s start from the beginning: what is the Fluent Builder pattern? It’s the specific variant of the Builder pattern, which “guides” the developer through the process of building an object, enabling setting of one field at a time. This approach has several benefits:

* No way to omit setting some field, code does not compile.
* When a new field is added to the base object, the compiler enforces updating all places where instances are built.

In other words, Fluent Builder is a useful tool for writing reliable and maintainable code. Nevertheless, just like with any pattern, there are cases when this pattern should not be used.

#### **From Regular Builder to Fluent Builder**

Review of possible use cases shows that there is a whole range of combinations of optional and mandatory fields:

* All fields optional
* Most fields optional, few fields mandatory
* Most fields mandatory, few fields optional
* All fields mandatory

Regular Builder is the best choice for the first case&nbsp;—&nbsp;all fields optional.

For the second case&nbsp;—&nbsp;where only a few fields mandatory, the best choice is the Regular Builder with `build()` method accepting mandatory parameters. Adding a new optional field in this case is safe, while adding a new mandatory field affects the signature of the `build()` method and enforces updating all places where it’s called. The only inconvenience of this pattern is the fact that mandatory parameters passed without names.

For the last case, when all fields mandatory, Fluent Builder obviously is the best choice.

Now it’s time to take a look at the case when few fields are optional. Regular builder is inconvenient here because the lack of naming of mandatory fields passed to build() method defeats the purpose of the Builder pattern. If naming is not an issue and in the majority of cases objects are built with optional fields set to defaults, static factory method(s) might be more convenient and will result is less boilerplate. In other cases, Fluent Builder with some modifications might be more convenient. Let’s take a closer look.

Let’s start from the Fluent Builder for case when all fields mandatory:
```java
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
   }

   public interface NameAgeBuilderStage4 {  
       NameAge age(Option<Integer> age);  
   }  
}
```
Note that all optional fields are shifted to the end of the build chain. This does not affect field order in the built object, only the build chain needs to be rearranged.

Now, let’s add a method which will set optional fields to defaults:
```java
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

       default NameAgeBuilderStage4 withNoMiddleName() {return middleName(Option.none());}

       default NameAge withoutMiddleNameAndAge() {return middleName(Option.none()).withUnknownAge();}  
   }

   public interface NameAgeBuilderStage4 {  
       NameAge age(Option<Integer> age);

       default NameAge withUnknownAge() {return age(Option.none());}  
   }  
}
```
Now we can build the object like this:
```java
var nameAge1 = NameAge.builder()  
                     .firstName("John")  
                     .lastName("Doe")  
                     .withoutMiddleNameAndAge();

var nameAge2 = NameAge.builder()  
                     .firstName("John")  
                     .lastName("Doe")  
                     .middleName(some("Smith"))  
                     .withUnknownAge();

var nameAge3 = NameAge.builder()  
                     .firstName("John")  
                     .lastName("Doe")  
                     .withNoMiddleName()  
                     .age(some(42));
```
#### **Conclusion**

The described approach provides a convenient solution for the case when some fields in the built object are optional. While it is not necessarily results in the most concise code, this approach enables preserving of the important context and makes code much better suited for reading and maintenance.
