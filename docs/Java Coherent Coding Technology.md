# Java Coherent Coding Technology 

This article is a description of a something, which, I believe, will make software development less art, more engineering.

So, why "technology"? Answer is simple: technology in other areas of engineering describe a predictable process, which follows clear rules and requirements, and produces product of predictable properties and quality.
Below described one of the possible ways to produce typical Java backend code of predictable properties.

## Part I. Internal code structure.
It is well known fact, that piece and harmony among team members regarding some topic can be achieved by setting standard.
That's exactly what is proposed below - set of rules, just by following which you'll make you code as unified as possible.
Keep in mind, it was not designed to be pretty, but be predictable, consistent and easy to follow.
Some feelings might be hurt, but each rule has clear purpose and justification.

### Fixed Set Of Code Structure Patterns
After writing and reading tons and tons of code in different languages and, especially, after 
adding some functional programming sauces to my everyday coding, I started noticing similar 
patterns again and again. They are of somewhat lower level than, for example, typical 
software design patterns like Strategy or Builder. These patterns are responsible for the code structure, kind of 
building blocks for implementation. All these patterns are well known, I've just gathered them together.   

Full list of patterns:

- Sequencer (Pipeline) - sequence of processing steps
- Fan-out-fan-in (Fork-Join) - gathering independent information from different sources followed by processing all obtained parts together.
- Aspect - doing something before and after the call. Aspect has same input and output type and just returns input value without touching it.
- Condition - Choosing between more than one processing paths.
- Iteration - Repeating the same operation (possibly with different arguments)
- Leaf - The actual low level logic, which lives entirely at single level of abstraction. It either, generates output from input parameters and performs only one logical step of operation; or just an adaptor from external API to internal or vice-versa.

By combining these six patterns it is possible to build backend business logic of arbitrary complexity.
With this in mind, following rule sounds obvious:

> ### Rule **#1:** (Single Level Of Abstraction)
> **All code uses only patterns shown above and each function/method implements exactly one pattern.**

This rule is a practical application of Single Abstraction Level principle for functions/methods.
It has several practical implications, I'll explore them later.

> **Note**
> 
> Most patterns don't do any actual business logic. Instead they orchestrate, route execution and collect data. Actual processing or bridging into external APIs happens in **Leafs**.

This results in clear layering 

> ### Rule **#2:** (Magic number "5")
> No more than 5 parts of same kind at once. 

I.e. no more than 5 sub-steps, no more than 5 parameters, no more than 5 levels of hierarchy of step breakdown, etc. etc.

The sole purpose of this rule is complexity management. By limiting number of things we have to deal at once we're 
limiting cognitive load and simplify reasoning about what code does. 

Of course, limit is not absolute. Sometimes it's not up to us to decide about number of things we have to deal with. For example, domain entity may have more than 5 states and we
will be ought to, for example, use switch with more than 5 branches. In all other cases the rule should be strictly followed.

> The choice of the number 5 is not random. This is a lower bound of well know "7 plus or minus 2" rule for the size of the short-term memory.
> I believe this limit is also applicable to the number of things we can conveniently deal at once.

 
-------------------

At this stage I don't know if that is enough in all situations. My considerations:
- At first level we have 5 sub-steps
- At second level each sub-step will be divided into 5 sub-steps.
- And so forth and so on until we reach level 5.

-------------------

This rule has numerous consequences (just ones I was able to identify:

- "Parse, don't validate" is an natural way to design code at lower level.
- Single level of abstraction is a natural way to write the code.
- Code gets clear hierarchical structure. The further you from input, the more and more primitive each part.
- Although personal taste may affect some aspects, but even then logical code structure remains clear. It does not matter anymore who wrote that code.
- After initial "breakdown" of processing steps one can quite precise estimate how long it will take. Surprises still possible, but extent and impact will be limited: "breakdown" enables quick identification of the possible issues. The more layers of intermediate steps you implement, the more precise your estimate is.
- It is possible to adjust functionality of the each step just by shifting sub-steps between two sequential steps.
- Once Leafs identified, they are simple enough to implement regardless from the seniority.
- Onboarding to the new project means getting familiar with the business, not with code. If there are several projects in one organization and you know the business good enough,you don't care how many projects you're actually working with.

Suggested approach for structuring business logic is following:
- At the topmost level the entire business logic is structured as set of functions, where each function implements single use case.
- Business logic is naturally structured as "functional services", i.e. objects which have one method. Method may fail or not. This depends on the implemented business logic.
- Business logic objects are created via factory method, by providing all necessary dependencies.
- Elements of domain are created via factory method or using *Fluent Builder* (depending on complexity). If domain element requires validation, factory method or Builder may return failure. This way business logic never deals with invalid data and type system ensures this.
