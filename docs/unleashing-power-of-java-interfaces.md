### **Unleashing Power of Java Interfaces**

Java interfaces, for a very long time, were just that — interfaces, an anemic set of function prototypes. Even then, there were non-standard uses of interfaces (for example, marker interfaces), but that's it.

But since Java 8 there were substantial changes in the interfaces. Additions of default and static methods enabled many new possibilities. For example, enabled adding of new functionality to existing interfaces without breaking old code. Or hiding all implementations behind factory methods, enforcing “code against interface” policy. Addition of sealed interfaces enabled creation of true sum types and expressing in code design intents. Together, these changes made Java interfaces a powerful, concise and expressive tool. Let’s take a look at some non-traditional applications of Java interfaces

#### **Fluent Builder**

Fluent (or Staged), Builder, is a pattern used to assemble object instances. Unlike the traditional Builder pattern, it prevents creating of incomplete objects and enforces fixed order of field initialization. These properties make it the preferred choice for reliable and maintainable code.

The idea behind Fluent Builder is rather simple. Instead of returning the same Builder instance after setting a property, it returns a new type (class or interface), which has only one method, therefore guiding the developer through the process of instance initialization. Fluent builder may omit the *build()* method at the end, as instance assembling ends once the last field is set.

Unfortunately, the straightforward implementation of Fluent Builder is very verbose:
```java
public record NameAge(String firstName, String lastName, Option<String> middleName, int age) {  
   public static NameAgeBuilderStage1 builder() {  
       return new NameAgeBuilder();  
   }

   public static class NameAgeBuilder implements NameAgeBuilderStage1,  
                                                 NameAgeBuilderStage2,  
                                                 NameAgeBuilderStage3,  
                                                 NameAgeBuilderStage4 {  
       private String firstName;  
       private String lastName;  
       private Option<String> middleName;

       @Override  
       public NameAgeBuilderStage2 firstName(String firstName) {  
           this.firstName = firstName;  
           return this;  
       }

       @Override  
       public NameAgeBuilderStage3 lastName(String lastName) {  
           this.lastName = lastName;  
           return this;  
       }

       @Override  
       public NameAgeBuilderStage4 middleName(Option<String> middleName) {  
           this.middleName = middleName;  
           return this;  
       }

       @Override  
       public NameAge age(int age) {  
           return new NameAge(firstName, lastName, middleName, age);  
       }  
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
       NameAge age(int age);  
   }  
}
```
It is also not very safe, as it is still possible to cast the returned interface to *NameAgeBuilder* and call the *age()* method, getting an incomplete object.

We might notice that each interface is a typical functional interface, with only one method inside. With this in mind, we may rewrite the code above into the following:
```java
public record NameAge(String firstName, String lastName, Option<String> middleName, int age) {  
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
       NameAge age(int age);  
   }  
}
```
Besides being much more concise, this version is not susceptible to (even hacky) premature object creation.

#### **Reduction Of Implementation**

Although default methods were created to enable extension of existing interfaces without breaking the existing implementation, this is not the only use for them.

For a long time, if we did need multiple implementations of the same interface, where many implementations share some code, the only way to avoid code duplication was creation of an abstract class and inheriting those implementations from it. Although this avoided code duplication, this solution is relatively verbose and causes unnecessary coupling. The abstract class is a purely technical entity which has no corresponding part in the application domain.

With default methods, abstract classes are no longer necessary, common functionality can be written directly in the interface, reducing boilerplate, eliminating coupling and improving maintainability.

But what if we go further? Sometimes it is possible to express all necessary functionality using only very few implementation-specific methods. Ideally — just one. This makes implementation classes very compact, easy to reason about and maintain. Let’s for example, implement *Maybe\<T\>* monad (yet another name for *Optional\<T\>*/*Option\<T\>*). No matter how rich and diverse API we’re planning to implement, it still could be expressed as a call to a single method, let’s call it *fold():*
```java
<R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper)
```
This method accepts two functions, one is called when value is present and another, when value is missing. The result of application is just returned as the result of the implemented method. With this method, we can implement *map()* and *flatMap()* as:
```java
   default <U> Maybe<U> map(Function<? super T, U> mapper) {  
       return fold(Maybe::nothing, t -> just(mapper.apply(t)));  
   }

   default <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> mapper) {  
       return fold(Maybe::nothing, mapper);  
   }
```
These implementations are universal and applicable to both variants. Note that since we have exactly two implementations, it makes perfect sense to make the interface sealed. And to even further reduce the amount of boilerplate — use records:
```java
public sealed interface Maybe<T> {  
   default <U> Maybe<U> map(Function<? super T, U> mapper) {  
       return fold(Maybe::nothing, t -> just(mapper.apply(t)));  
   }

   default <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> mapper) {  
       return fold(Maybe::nothing, mapper);  
   }

   <R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper);

   static <T> Just<T> just(T value) {  
       return new Just<>(value);  
   }

   @SuppressWarnings("unchecked")  
   static <T> Nothing<T> nothing() {  
       return (Nothing<T>) Nothing.INSTANCE;  
   }

   static <T> Maybe<T> maybe(T value) {  
       return value == null ? nothing() : just(value);  
   }

   record Just<T>(T value) implements Maybe<T> {  
       public  <R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper) {  
           return justMapper.apply(value);  
       }  
   }

   record Nothing<T>() implements Maybe<T> {  
       static final Nothing<?> INSTANCE = new Nothing<>();

       @Override  
       public <R> R fold(Supplier<? extends R\> nothingMapper, Function<? super T, ? extends R\> justMapper) {  
           return nothingMapper.get();  
       }  
   }  
}
```
Although this is not strictly necessary for demonstration, this implementation uses shared constant for the implementation of *Nothing\<T\>*, reducing allocation. Another interesting property of this implementation — it uses no if statement (nor ternary operator) for the logic. This improves performance and enables better optimization by the Java compiler.

Another useful property of this implementation — it is convenient for the pattern matching (unlike Java *Optional\<T\>*, for example):
```java
var result = switch (maybe) {  
   case Just\<String\>(var value) -> value;  
   case Nothing\<String\> nothing -> "Nothing";  
};
```
But sometimes, even implementation classes are not necessary. The example below shows how the entire implementation fits into the interface (full code can be found [here](https://github.com/siy/pragmatica-lite/blob/f572684d6eb8fbad8e45a13837ac983518ed62d2/examples/url-shortener/src/main/java/org/pragmatica/http/example/urlshortener/persistence/ShortenedUrlRepository.java)):
```java
public interface ShortenedUrlRepository {  
   default Promise\<ShortenedUrl\> create(ShortenedUrl shortenedUrl) {  
       return QRY."INSERT INTO shortenedurl (\\{template().fieldNames()}) VALUES (\\{template().fieldValues(shortenedUrl)}) RETURNING \*"  
           .in(db())  
           .asSingle(template());  
   }

   default Promise\<ShortenedUrl\> read(String id) {  
       return QRY."SELECT \* FROM shortenedurl WHERE id = \\{id}"  
           .in(db())  
           .asSingle(template());  
   }

   default Promise\<Unit\> delete(String id) {  
       return QRY."DELETE FROM shortenedurl WHERE id = \\{id}"  
           .in(db())  
           .asUnit();  
   }

   DbEnv db();  
}
```
To turn this interface into a working instance, all we need is to provide an instance of environment. For example, like [this](https://github.com/siy/pragmatica-lite/blob/1054f83eccf004a7f04b9f72eb4d0f82283b86cb/examples/url-shortener/src/main/java/org/pragmatica/http/example/urlshortener/UrlShortener.java#L54):
```java
var dbEnv = DbEnv.with(dbEnvConfig);

ShortenedUrlRepository repository = () -> dbEnv;
```
This approach sometimes results in the code which is too concise and sometimes requires writing a more verbose version to preserve context. I’d say that this is quite an unusual property for Java code, which is often blamed for verbosity.

#### **Utility … Interfaces?**

Well, utility (as well as constant) interfaces were not feasible for a long time. Perhaps the main reason is that such interfaces could be implemented and constants as well as utility functions would be (unnecessary) part of the implementation.

But with sealed interfaces, this issue can be solved in a way, similar to how instantiation of utility classes is prevented:
```java
public sealed interface Utility {  
   ...

   record unused() implements Utility {}  
}
```
At first look, it makes no big sense to use this approach. But use of interface eliminates the need for visibility modifiers for each method and/or constant. This, in turn, reduces the amount of syntactic noise, which is mandatory for classes, but redundant for interfaces, as they have all their members public.

#### **Interfaces And Private Records**

The combination of these two constructs enables convenient writing code in “OO without classes” style, enforcing “code against interface” while reducing boilerplate at the same time. For example:
```java
public interface ContentType {  
   String headerText();  
   ContentCategory category();

   static ContentType custom(String headerText, ContentCategory category) {  
       record contentType(String headerText, ContentCategory category) implements ContentType {}

       return new contentType(headerText, category);  
   }  
}
```
The private record serves two purposes:

* It keeps use of implementation under complete control. No direct instantiations are possible, only via static factory method.
* Keeps implementation close to the interface, simplifying support, extension and maintenance.

Note that the interface is not sealed, so one can do, for example, the following:
```java
public enum CommonContentTypes implements ContentType {  
   TEXT\_PLAIN("text/plain; charset=UTF-8", ContentCategory.PLAIN\_TEXT),  
   APPLICATION\_JSON("application/json; charset=UTF-8", ContentCategory.JSON),

   ;  
   private final String headerText;  
   private final ContentCategory category;

   CommonContentTypes(String headerText, ContentCategory category) {  
       this.headerText = headerText;  
       this.category = category;  
   }

   @Override  
   public String headerText() {  
       return headerText;  
   }

   @Override  
   public ContentCategory category() {  
       return category;  
   }  
}
```
#### **Conclusion**

Interfaces are a powerful Java feature, often underestimated and underutilized. This article is an attempt to shed the light on the possible ways to utilize their power and get clean, expressive, concise yet readable code.
