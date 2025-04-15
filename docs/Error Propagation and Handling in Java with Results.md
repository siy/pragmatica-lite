# Error Propagation and Handling in Java with Results

Error propagation and handling is one of the sources of heated debates.
Unfortunately, these debates mostly based on subjective opinions and lacking 
objective justification. Let's try to assess the whole topic objectively to 
understand strong and weak sides of all widely used approaches. Note that whole
discussion is focused on business errors, i.e. errors which have meaning in business
logic and are recoverable. Unrecoverable technical errors are not discussed below.

At first it is necessary to understand the error life cycle. Usually the place, 
where error is detected is not the place, where error could be dealt. That's because 
error handling requires wider context than is available at the point where error is detected.
Therefore each error has life cycle consisting of two distinct phases: propagation,
when error bubbles up and actual error handling. Quite often actual error handling 
does not happen at all inside the application and error is just propagated to the
caller. This fact emphasises importance of the convenient error propagation mechanism.

Another important consideration is the visibility of the possibility of error in the code. i.e. context preservation.
If possibility of the error is clearly visible in the code, this significantly improves
code maintainability and reduces mental overhead. 

With the above considerations in mind, let's review widely used approaches to error
propagation and handling:

1. **Return special value**. This approach has limited applicability since such a special
value not always exists and there is no way to propagate information about the nature of the error. The context is not preserved
and propagation must be performed manually.
Modification of this approach: return error code directly, return operation result using pointers.   
This allowed to pass limited information about the error, but propagation remains manual and possibility of error
relies on convention about the API design rather than explicit visibility.

2. **Return tuple containing error and operation result**. This approach enables propagation
of the information about the error, but propagation is still performed manually.

3. **Throw exception**. This approach provides automatic error propagation.  