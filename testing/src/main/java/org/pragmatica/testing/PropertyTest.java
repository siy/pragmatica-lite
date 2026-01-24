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

package org.pragmatica.testing;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Functions.Fn3;
import org.pragmatica.lang.Functions.Fn4;
import org.pragmatica.lang.Functions.Fn5;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.Tuple.Tuple4;
import org.pragmatica.lang.Tuple.Tuple5;

/// Property-based test runner with shrinking support.
public sealed interface PropertyTest {
    /// Create a property test builder for a single arbitrary.
    static <T> PropertyTestBuilder<T> forAll(Arbitrary<T> arbitrary) {
        return new PropertyTestBuilderImpl<>(arbitrary);
    }

    /// Create a property test builder for two arbitraries.
    static <T1, T2> PropertyTestBuilder2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2) {
        return new PropertyTestBuilder2Impl<>(a1, a2);
    }

    /// Create a property test builder for three arbitraries.
    static <T1, T2, T3> PropertyTestBuilder3<T1, T2, T3> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3) {
        return new PropertyTestBuilder3Impl<>(a1, a2, a3);
    }

    /// Create a property test builder for four arbitraries.
    static <T1, T2, T3, T4> PropertyTestBuilder4<T1, T2, T3, T4> forAll(Arbitrary<T1> a1,
                                                                        Arbitrary<T2> a2,
                                                                        Arbitrary<T3> a3,
                                                                        Arbitrary<T4> a4) {
        return new PropertyTestBuilder4Impl<>(a1, a2, a3, a4);
    }

    /// Create a property test builder for five arbitraries.
    static <T1, T2, T3, T4, T5> PropertyTestBuilder5<T1, T2, T3, T4, T5> forAll(Arbitrary<T1> a1,
                                                                                Arbitrary<T2> a2,
                                                                                Arbitrary<T3> a3,
                                                                                Arbitrary<T4> a4,
                                                                                Arbitrary<T5> a5) {
        return new PropertyTestBuilder5Impl<>(a1, a2, a3, a4, a5);
    }

    interface PropertyTestBuilder<T> {
        PropertyTestBuilder<T> tries(int count);
        PropertyTestBuilder<T> seed(long seed);
        PropertyTestBuilder<T> shrinkingDepth(int depth);

        Result<PropertyResult> check(Fn1<Boolean, T> property);
        Result<PropertyResult> checkResult(Fn1<Result<?>, T> property);
    }

    interface PropertyTestBuilder2<T1, T2> {
        PropertyTestBuilder2<T1, T2> tries(int count);
        PropertyTestBuilder2<T1, T2> seed(long seed);
        PropertyTestBuilder2<T1, T2> shrinkingDepth(int depth);

        Result<PropertyResult> check(Fn2<Boolean, T1, T2> property);
        Result<PropertyResult> checkResult(Fn2<Result<?>, T1, T2> property);
    }

    interface PropertyTestBuilder3<T1, T2, T3> {
        PropertyTestBuilder3<T1, T2, T3> tries(int count);
        PropertyTestBuilder3<T1, T2, T3> seed(long seed);
        PropertyTestBuilder3<T1, T2, T3> shrinkingDepth(int depth);

        Result<PropertyResult> check(Fn3<Boolean, T1, T2, T3> property);
        Result<PropertyResult> checkResult(Fn3<Result<?>, T1, T2, T3> property);
    }

    interface PropertyTestBuilder4<T1, T2, T3, T4> {
        PropertyTestBuilder4<T1, T2, T3, T4> tries(int count);
        PropertyTestBuilder4<T1, T2, T3, T4> seed(long seed);
        PropertyTestBuilder4<T1, T2, T3, T4> shrinkingDepth(int depth);

        Result<PropertyResult> check(Fn4<Boolean, T1, T2, T3, T4> property);
        Result<PropertyResult> checkResult(Fn4<Result<?>, T1, T2, T3, T4> property);
    }

    interface PropertyTestBuilder5<T1, T2, T3, T4, T5> {
        PropertyTestBuilder5<T1, T2, T3, T4, T5> tries(int count);
        PropertyTestBuilder5<T1, T2, T3, T4, T5> seed(long seed);
        PropertyTestBuilder5<T1, T2, T3, T4, T5> shrinkingDepth(int depth);

        Result<PropertyResult> check(Fn5<Boolean, T1, T2, T3, T4, T5> property);
        Result<PropertyResult> checkResult(Fn5<Result<?>, T1, T2, T3, T4, T5> property);
    }

    record unused() implements PropertyTest {}
}

final class PropertyTestBuilderImpl<T> implements PropertyTest.PropertyTestBuilder<T> {
    private final Arbitrary<T> arbitrary;
    private int tries = 100;
    private long seed = System.nanoTime();
    private int shrinkingDepth = 100;

    PropertyTestBuilderImpl(Arbitrary<T> arbitrary) {
        this.arbitrary = arbitrary;
    }

    @Override
    public PropertyTest.PropertyTestBuilder<T> tries(int count) {
        this.tries = count;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder<T> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder<T> shrinkingDepth(int depth) {
        this.shrinkingDepth = depth;
        return this;
    }

    @Override
    public Result<PropertyResult> check(Fn1<Boolean, T> property) {
        return checkResult(value -> property.apply(value)
                                    ? Result.unitResult()
                                    : PropertyTestError.PROPERTY_FAILED.result());
    }

    @Override
    public Result<PropertyResult> checkResult(Fn1<Result<?>, T> property) {
        RandomSource random = RandomSource.seeded(seed);
        for (int i = 0; i < tries; i++) {
            Shrinkable<T> shrinkable = arbitrary.generate(random);
            T originalValue = shrinkable.value();
            PropertyCheckResult checkResult = checkProperty(property, originalValue);
            if (!checkResult.passed()) {
                ShrinkResult<T> shrinkResult = shrink(property, shrinkable);
                return Result.success(new PropertyResult.Failed(i + 1,
                                                                originalValue,
                                                                shrinkResult.value(),
                                                                shrinkResult.steps(),
                                                                checkResult.error()));
            }
        }
        return Result.success(new PropertyResult.Passed(tries));
    }

    private PropertyCheckResult checkProperty(Fn1<Result<?>, T> property, T value) {
        try{
            Result<?> result = property.apply(value);
            return result.fold(_ -> new PropertyCheckResult(false, Option.none()),
                               _ -> new PropertyCheckResult(true, Option.none()));
        } catch (Throwable t) {
            return new PropertyCheckResult(false, Option.option(t));
        }
    }

    private ShrinkResult<T> shrink(Fn1<Result<?>, T> property, Shrinkable<T> shrinkable) {
        T smallest = shrinkable.value();
        int steps = 0;
        Shrinkable<T> current = shrinkable;
        for (int depth = 0; depth < shrinkingDepth; depth++) {
            var shrinks = current.shrink()
                                 .iterator();
            boolean foundSmaller = false;
            while (shrinks.hasNext()) {
                Shrinkable<T> candidate = shrinks.next();
                PropertyCheckResult checkResult = checkProperty(property, candidate.value());
                if (!checkResult.passed()) {
                    smallest = candidate.value();
                    current = candidate;
                    steps++;
                    foundSmaller = true;
                    break;
                }
            }
            if (!foundSmaller) {
                break;
            }
        }
        return new ShrinkResult<>(smallest, steps);
    }
}

record PropertyCheckResult(boolean passed, Option<Throwable> error) {}

record ShrinkResult<T>(T value, int steps) {}

enum PropertyTestError implements org.pragmatica.lang.Cause {
    PROPERTY_FAILED;
    @Override
    public String message() {
        return "Property check failed";
    }
}

final class PropertyTestBuilder2Impl<T1, T2> implements PropertyTest.PropertyTestBuilder2<T1, T2> {
    private final Arbitrary<Tuple2<T1, T2>> tupleArbitrary;
    private int tries = 100;
    private long seed = System.nanoTime();
    private int shrinkingDepth = 100;

    PropertyTestBuilder2Impl(Arbitrary<T1> a1, Arbitrary<T2> a2) {
        this.tupleArbitrary = random -> {
            Shrinkable<T1> s1 = a1.generate(random);
            Shrinkable<T2> s2 = a2.generate(random);
            return Shrinkable.unshrinkable(Tuple.tuple(s1.value(), s2.value()));
        };
    }

    @Override
    public PropertyTest.PropertyTestBuilder2<T1, T2> tries(int count) {
        this.tries = count;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder2<T1, T2> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder2<T1, T2> shrinkingDepth(int depth) {
        this.shrinkingDepth = depth;
        return this;
    }

    @Override
    public Result<PropertyResult> check(Fn2<Boolean, T1, T2> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .check(tuple -> tuple.map(property));
    }

    @Override
    public Result<PropertyResult> checkResult(Fn2<Result<?>, T1, T2> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .checkResult(tuple -> tuple.map(property));
    }
}

final class PropertyTestBuilder3Impl<T1, T2, T3> implements PropertyTest.PropertyTestBuilder3<T1, T2, T3> {
    private final Arbitrary<Tuple3<T1, T2, T3>> tupleArbitrary;
    private int tries = 100;
    private long seed = System.nanoTime();
    private int shrinkingDepth = 100;

    PropertyTestBuilder3Impl(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3) {
        this.tupleArbitrary = random -> {
            Shrinkable<T1> s1 = a1.generate(random);
            Shrinkable<T2> s2 = a2.generate(random);
            Shrinkable<T3> s3 = a3.generate(random);
            return Shrinkable.unshrinkable(Tuple.tuple(s1.value(), s2.value(), s3.value()));
        };
    }

    @Override
    public PropertyTest.PropertyTestBuilder3<T1, T2, T3> tries(int count) {
        this.tries = count;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder3<T1, T2, T3> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder3<T1, T2, T3> shrinkingDepth(int depth) {
        this.shrinkingDepth = depth;
        return this;
    }

    @Override
    public Result<PropertyResult> check(Fn3<Boolean, T1, T2, T3> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .check(tuple -> tuple.map(property));
    }

    @Override
    public Result<PropertyResult> checkResult(Fn3<Result<?>, T1, T2, T3> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .checkResult(tuple -> tuple.map(property));
    }
}

final class PropertyTestBuilder4Impl<T1, T2, T3, T4> implements PropertyTest.PropertyTestBuilder4<T1, T2, T3, T4> {
    private final Arbitrary<Tuple4<T1, T2, T3, T4>> tupleArbitrary;
    private int tries = 100;
    private long seed = System.nanoTime();
    private int shrinkingDepth = 100;

    PropertyTestBuilder4Impl(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4) {
        this.tupleArbitrary = random -> {
            Shrinkable<T1> s1 = a1.generate(random);
            Shrinkable<T2> s2 = a2.generate(random);
            Shrinkable<T3> s3 = a3.generate(random);
            Shrinkable<T4> s4 = a4.generate(random);
            return Shrinkable.unshrinkable(Tuple.tuple(s1.value(), s2.value(), s3.value(), s4.value()));
        };
    }

    @Override
    public PropertyTest.PropertyTestBuilder4<T1, T2, T3, T4> tries(int count) {
        this.tries = count;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder4<T1, T2, T3, T4> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder4<T1, T2, T3, T4> shrinkingDepth(int depth) {
        this.shrinkingDepth = depth;
        return this;
    }

    @Override
    public Result<PropertyResult> check(Fn4<Boolean, T1, T2, T3, T4> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .check(tuple -> tuple.map(property));
    }

    @Override
    public Result<PropertyResult> checkResult(Fn4<Result<?>, T1, T2, T3, T4> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .checkResult(tuple -> tuple.map(property));
    }
}

final class PropertyTestBuilder5Impl<T1, T2, T3, T4, T5> implements PropertyTest.PropertyTestBuilder5<T1, T2, T3, T4, T5> {
    private final Arbitrary<Tuple5<T1, T2, T3, T4, T5>> tupleArbitrary;
    private int tries = 100;
    private long seed = System.nanoTime();
    private int shrinkingDepth = 100;

    PropertyTestBuilder5Impl(Arbitrary<T1> a1, Arbitrary<T2> a2, Arbitrary<T3> a3, Arbitrary<T4> a4, Arbitrary<T5> a5) {
        this.tupleArbitrary = random -> {
            Shrinkable<T1> s1 = a1.generate(random);
            Shrinkable<T2> s2 = a2.generate(random);
            Shrinkable<T3> s3 = a3.generate(random);
            Shrinkable<T4> s4 = a4.generate(random);
            Shrinkable<T5> s5 = a5.generate(random);
            return Shrinkable.unshrinkable(Tuple.tuple(s1.value(), s2.value(), s3.value(), s4.value(), s5.value()));
        };
    }

    @Override
    public PropertyTest.PropertyTestBuilder5<T1, T2, T3, T4, T5> tries(int count) {
        this.tries = count;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder5<T1, T2, T3, T4, T5> seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public PropertyTest.PropertyTestBuilder5<T1, T2, T3, T4, T5> shrinkingDepth(int depth) {
        this.shrinkingDepth = depth;
        return this;
    }

    @Override
    public Result<PropertyResult> check(Fn5<Boolean, T1, T2, T3, T4, T5> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .check(tuple -> tuple.map(property));
    }

    @Override
    public Result<PropertyResult> checkResult(Fn5<Result<?>, T1, T2, T3, T4, T5> property) {
        return new PropertyTestBuilderImpl<>(tupleArbitrary).tries(tries)
                                                            .seed(seed)
                                                            .shrinkingDepth(shrinkingDepth)
                                                            .checkResult(tuple -> tuple.map(property));
    }
}
