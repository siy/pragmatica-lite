package org.pragmatica.message;

import java.lang.annotation.*;

/// This annotation marks methods which handler messages from [MessageRouter].
/// At present this annotation is used only for documentation purposes.
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Inherited
public @interface MessageReceiver {
}
