/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.lang;

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Functions.ThrowingFn0;
import org.pragmatica.lang.io.CoreError;

/// From <a href="https://en.wikipedia.org/wiki/Unit_type">Unit type</a> :
/// <blockquote>
/// In the area of mathematical logic and computer science known as type theory, a unit type is a type that allows only one value (and thus can hold no
/// information). The carrier (underlying set) associated with a unit type can be any singleton set. There is an isomorphism between any two such sets,
/// so it is customary to talk about the unit type and ignore the details of its value. One may also regard the unit type as the type of 0-tuples, i.e.
/// the product of no types.
///
/// The unit type is the terminal object in the category of types and typed functions. It should not be confused with the zero or bottom type, which
/// allows no values and is the initial object in this category. Similarly, the Boolean is the type with two values.
///
/// The unit type is implemented in most functional programming languages. The void type that is used in some imperative programming languages serves
/// some of its functions, but because its carrier set is empty, it has some limitations.
/// </blockquote>
@SuppressWarnings("unused")
public final class Unit implements Tuple.Tuple0 {
    private Unit() {}

    private static final Unit UNIT = new Unit();

    public static Unit unit() {
        return UNIT;
    }

    public static <T> Unit toUnit(final T ignored) {
        return UNIT;
    }

    public static <T1, T2> Unit toUnit(final T1 ignored1, final T2 ignored2) {
        return UNIT;
    }

    public static <T1, T2, T3> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3) {
        return UNIT;
    }

    public static <T1, T2, T3, T4> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3, final T4 ignored4) {
        return UNIT;
    }

    public static <T1, T2, T3, T4, T5> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3,
                                                   final T4 ignored4, final T5 ignored5) {
        return UNIT;
    }

    public static <T1, T2, T3, T4, T5, T6> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3,
                                                       final T4 ignored4, final T5 ignored5, final T6 ignored6) {
        return UNIT;
    }

    public static <T1, T2, T3, T4, T5, T6, T7> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3, final T4 ignored4,
                                                           final T5 ignored5, final T6 ignored6, final T7 ignored7) {
        return UNIT;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3, final T4 ignored4,
                                                               final T5 ignored5, final T6 ignored6, final T7 ignored7, final T8 ignored8) {
        return UNIT;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Unit toUnit(final T1 ignored1, final T2 ignored2, final T3 ignored3,
                                                                   final T4 ignored4, final T5 ignored5, final T6 ignored6,
                                                                   final T7 ignored7, final T8 ignored8, final T9 ignored9) {
        return UNIT;
    }

    @Override
    public String toString() {
        return "()";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tuple0;
    }

    @Override
    public int hashCode() {
        return 0;   // There is only one instance of Unit
    }

    @Override
    public <T> T map(Fn0<T> mapper) {
        return mapper.apply();
    }

    @Override
    public <T> Result<T> lift(ThrowingFn0<T> mapper) {
        try {
            return Result.success(mapper.apply());
        } catch (Throwable throwable) {
            return new CoreError.Exception(throwable).result();
        }
    }
}
