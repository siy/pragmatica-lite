package com.github.pgasync.async;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompletableFuture<T> implements IntermediateFuture<T> {
    volatile Object result;       // Either the result or boxed AltResult
    volatile Completion stack;    // Top of Treiber stack of dependent actions

    final void internalComplete(Object r) { // CAS from null to r
        RESULT.compareAndSet(this, null, r);
    }

    /**
     * Returns true if successfully pushed c onto stack.
     */
    final boolean tryPushStack(Completion c) {
        Completion h = stack;
        NEXT.set(c, h);         // CAS piggyback
        return STACK.compareAndSet(this, h, c);
    }

    /**
     * Unconditionally pushes c onto stack, retrying if necessary.
     */
    final void pushStack(Completion c) {
        do {
        } while (!tryPushStack(c));
    }

    /* ------------- Encoding and decoding outcomes -------------- */

    static final class AltResult { // See above
        final Throwable ex;        // null only for NIL

        AltResult(Throwable x) {
            this.ex = x;
        }
    }

    /**
     * The encoding of the null value.
     */
    static final AltResult NIL = new AltResult(null);

    /**
     * Completes with the null value, unless already completed.
     */
    final void completeNull() {
        RESULT.compareAndSet(this, null, NIL);
    }

    /**
     * Returns the encoding of the given non-exceptional value.
     */
    final Object encodeValue(T t) {
        return (t == null) ? NIL : t;
    }

    /**
     * Completes with a non-exceptional result, unless already completed.
     */
    final void completeValue(T t) {
        RESULT.compareAndSet(this, null, (t == null) ? NIL : t);
    }

    /**
     * Returns the encoding of the given (non-null) exception as a wrapped CompletionException unless it is one already.
     */
    static AltResult encodeThrowable(Throwable x) {
        return new AltResult((x instanceof CompletionException) ? x :
                             new CompletionException(x));
    }

    /**
     * Completes with an exceptional result, unless already completed.
     */
    final void completeThrowable(Throwable x) {
        RESULT.compareAndSet(this, null, encodeThrowable(x));
    }

    /**
     * Returns the encoding of the given (non-null) exception as a wrapped CompletionException unless it is one already.  May return the given Object
     * r (which must have been the result of a source future) if it is equivalent, i.e. if this is a simple relay of an existing CompletionException.
     */
    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        } else if (r instanceof AltResult && x == ((AltResult) r).ex) {
            return r;
        }
        return new AltResult(x);
    }

    /**
     * Completes with the given (non-null) exceptional result as a wrapped CompletionException unless it is one already, unless already completed. May
     * complete with the given Object r (which must have been the result of a source future) if it is equivalent, i.e. if this is a simple propagation
     * of an existing CompletionException.
     */
    final void completeThrowable(Throwable x, Object r) {
        RESULT.compareAndSet(this, null, encodeThrowable(x, r));
    }

    /**
     * Returns the encoding of a copied outcome; if exceptional, rewraps as a CompletionException, else returns argument.
     */
    static Object encodeRelay(Object r) {
        Throwable x;
        if (r instanceof AltResult
            && (x = ((AltResult) r).ex) != null
            && !(x instanceof CompletionException)) {
            r = new AltResult(new CompletionException(x));
        }
        return r;
    }

    /**
     * Completes with r or a copy of r, unless already completed. If exceptional, r is first coerced to a CompletionException.
     */
    final void completeRelay(Object r) {
        RESULT.compareAndSet(this, null, encodeRelay(r));
    }

    /**
     * Decodes outcome to return result or throw unchecked exception.
     */
    private static Object reportJoin(Object r) {
        if (r instanceof AltResult) {
            Throwable x;
            if ((x = ((AltResult) r).ex) == null) {
                return null;
            }
            if (x instanceof CancellationException) {
                throw (CancellationException) x;
            }
            if (x instanceof CompletionException) {
                throw (CompletionException) x;
            }
            throw new CompletionException(x);
        }
        return r;
    }

    /* ------------- Async task preliminaries -------------- */

    /**
     * A marker interface identifying asynchronous tasks produced by {@code async} methods. This may be useful for monitoring, debugging, and tracking
     * asynchronous activities.
     *
     * @since 1.8
     */
    public interface AsynchronousCompletionTask {
    }

    private static final boolean USE_COMMON_POOL = (ForkJoinPool.getCommonPoolParallelism() > 1);

    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot support parallelism.
     */
    private static final Executor ASYNC_POOL = USE_COMMON_POOL ?
                                               ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();

    /**
     * Fallback if ForkJoinPool.commonPool() cannot support parallelism
     */
    private static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            Objects.requireNonNull(r);
            new Thread(r).start();
        }
    }


    // Modes for Completion.tryFire. Signedness matters.
    static final int SYNC = 0;
    static final int ASYNC = 1;
    static final int NESTED = -1;

    /* ------------- Base Completion classes and operations -------------- */

    abstract static class Completion extends ForkJoinTask<Void>
        implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;      // Treiber stack link

        /**
         * Performs completion action if triggered, returning a dependent that may need propagation, if one exists.
         *
         * @param mode SYNC, ASYNC, or NESTED
         */
        abstract CompletableFuture<?> tryFire(int mode);

        /**
         * Returns true if possibly still triggerable. Used by cleanStack.
         */
        abstract boolean isLive();

        public final void run() {
            tryFire(ASYNC);
        }

        public final boolean exec() {
            tryFire(ASYNC);
            return false;
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }
    }

    /**
     * Pops and tries to trigger all reachable dependents.  Call only when known to be done.
     */
    final void postComplete() {
        /*
         * On each step, variable f holds current dependents to pop
         * and run.  It is extended along only one path at a time,
         * pushing others to avoid unbounded recursion.
         */
        CompletableFuture<?> f = this;
        Completion h;
        while ((h = f.stack) != null ||
               (f != this && (h = (f = this).stack) != null)) {
            CompletableFuture<?> d;
            Completion t;
            if (STACK.compareAndSet(f, h, t = h.next)) {
                if (t != null) {
                    if (f != this) {
                        pushStack(h);
                        continue;
                    }
                    NEXT.compareAndSet(h, t, null); // try to detach
                }
                f = (d = h.tryFire(NESTED)) == null ? this : d;
            }
        }
    }

    /**
     * Traverses stack and unlinks one or more dead Completions, if found.
     */
    final void cleanStack() {
        Completion p = stack;
        // ensure head of stack live
        for (boolean unlinked = false; ; ) {
            if (p == null) {
                return;
            } else if (p.isLive()) {
                if (unlinked) {
                    return;
                } else {
                    break;
                }
            } else if (STACK.weakCompareAndSet(this, p, (p = p.next))) {
                unlinked = true;
            } else {
                p = stack;
            }
        }
        // try to unlink first non-live
        for (Completion q = p.next; q != null; ) {
            Completion s = q.next;
            if (q.isLive()) {
                p = q;
                q = s;
            } else if (NEXT.weakCompareAndSet(p, q, s)) {
                break;
            } else {
                q = p.next;
            }
        }
    }

    /* ------------- One-input Completions -------------- */

    /**
     * A Completion with a source, dependent, and executor.
     */
    abstract static class UniCompletion<T, V> extends Completion {
        CompletableFuture<V> dep;          // the dependent to complete
        CompletableFuture<T> src;          // source for action

        UniCompletion(CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.dep = dep;
            this.src = src;
        }

        /**
         * Returns true if action can be run. Call only when known to be triggerable. Uses FJ tag bit to ensure that only one thread claims ownership.
         * If async, starts as task -- a later call to tryFire will run action.
         */
        final boolean claim() {
            return compareAndSetForkJoinTaskTag((short) 0, (short) 1);
        }

        final boolean isLive() {
            return dep != null;
        }
    }

    /**
     * Pushes the given completion unless it completes while trying. Caller should first check that result is null.
     */
    final void unipush(Completion c) {
        if (c != null) {
            while (!tryPushStack(c)) {
                if (result != null) {
                    NEXT.set(c, null);
                    break;
                }
            }
            if (result != null) {
                c.tryFire(SYNC);
            }
        }
    }

    /**
     * Post-processing by dependent after successful UniCompletion tryFire. Tries to clean stack of source a, and then either runs postComplete or
     * returns this to caller, depending on mode.
     */
    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (a != null && a.stack != null) {
            Object r;
            if ((r = a.result) == null) {
                a.cleanStack();
            }
            if (mode >= 0 && (r != null || a.result != null)) {
                a.postComplete();
            }
        }
        if (result != null && stack != null) {
            if (mode < 0) {
                return this;
            } else {
                postComplete();
            }
        }
        return null;
    }

    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn;

        UniApply(CompletableFuture<V> dep,
                 CompletableFuture<T> src,
                 Function<? super T, ? extends V> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Object r;
            Throwable x;
            Function<? super T, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null) {
                return null;
            }
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim()) {
                        return null;
                    } else {
                        @SuppressWarnings("unchecked") T t = (T) r;
                        d.completeValue(f.apply(t));
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    private <V> CompletableFuture<V> uniApplyStage(Function<? super T, ? extends V> f) {
        Object r;

        if ((r = result) != null) {
            return uniApplyNow(r, f);
        }

        CompletableFuture<V> d = newIncompleteFuture();
        unipush(new UniApply<>(d, this, f));
        return d;
    }

    private <V> CompletableFuture<V> uniApplyNow(Object r, Function<? super T, ? extends V> f) {
        Throwable x;
        CompletableFuture<V> d = newIncompleteFuture();

        if (r instanceof AltResult) {
            if ((x = ((AltResult) r).ex) != null) {
                d.result = encodeThrowable(x, r);
                return d;
            }
            r = null;
        }

        try {
            @SuppressWarnings("unchecked") T t = (T) r;
            d.result = d.encodeValue(f.apply(t));
        } catch (Throwable ex) {
            d.result = encodeThrowable(ex);
        }

        return d;
    }

    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> fn;

        UniAccept(CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            Object r;
            Throwable x;
            Consumer<? super T> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null) {
                return null;
            }
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim()) {
                        return null;
                    } else {
                        @SuppressWarnings("unchecked") T t = (T) r;
                        f.accept(t);
                        d.completeNull();
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    private CompletableFuture<Void> uniAcceptStage(Consumer<? super T> f) {
        Object r;
        if ((r = result) != null) {
            return uniAcceptNow(r, f);
        }
        CompletableFuture<Void> d = newIncompleteFuture();
        unipush(new UniAccept<>(d, this, f));
        return d;
    }

    private CompletableFuture<Void> uniAcceptNow(Object r, Consumer<? super T> f) {
        Throwable x;
        CompletableFuture<Void> d = newIncompleteFuture();
        if (r instanceof AltResult) {
            if ((x = ((AltResult) r).ex) != null) {
                d.result = encodeThrowable(x, r);
                return d;
            }
            r = null;
        }
        try {
            @SuppressWarnings("unchecked") T t = (T) r;
            f.accept(t);
            d.result = NIL;
        } catch (Throwable ex) {
            d.result = encodeThrowable(ex);
        }
        return d;
    }

    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;

        UniWhenComplete(CompletableFuture<T> dep, CompletableFuture<T> src,
                        BiConsumer<? super T, ? super Throwable> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            Object r;
            BiConsumer<? super T, ? super Throwable> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniWhenComplete(r, f, mode > 0 ? null : this)) {
                return null;
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniWhenComplete(Object r,
                                  BiConsumer<? super T, ? super Throwable> f,
                                  UniWhenComplete<T> c) {
        T t;
        Throwable x = null;
        if (result == null) {
            try {
                if (c != null && !c.claim()) {
                    return false;
                }
                if (r instanceof AltResult) {
                    x = ((AltResult) r).ex;
                    t = null;
                } else {
                    @SuppressWarnings("unchecked") T tr = (T) r;
                    t = tr;
                }
                f.accept(t, x);
                if (x == null) {
                    internalComplete(r);
                    return true;
                }
            } catch (Throwable ex) {
                if (x == null) {
                    x = ex;
                } else if (x != ex) {
                    x.addSuppressed(ex);
                }
            }
            completeThrowable(x, r);
        }
        return true;
    }

    private CompletableFuture<T> uniWhenCompleteStage(BiConsumer<? super T, ? super Throwable> f) {
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;

        if ((r = result) == null) {
            unipush(new UniWhenComplete<>(d, this, f));
        } else {
            d.uniWhenComplete(r, f, null);
        }
        return d;
    }

    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> fn;

        UniHandle(CompletableFuture<V> dep, CompletableFuture<T> src,
                  BiFunction<? super T, Throwable, ? extends V> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Object r;
            BiFunction<? super T, Throwable, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniHandle(r, f, mode > 0 ? null : this)) {
                return null;
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniHandle(Object r,
                                BiFunction<? super S, Throwable, ? extends T> f,
                                UniHandle<S, T> c) {
        S s;
        Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim()) {
                    return false;
                }
                if (r instanceof AltResult) {
                    x = ((AltResult) r).ex;
                    s = null;
                } else {
                    x = null;
                    @SuppressWarnings("unchecked") S ss = (S) r;
                    s = ss;
                }
                completeValue(f.apply(s, x));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <V> CompletableFuture<V> uniHandleStage(BiFunction<? super T, Throwable, ? extends V> f) {
        CompletableFuture<V> d = newIncompleteFuture();
        Object r;
        if ((r = result) == null) {
            unipush(new UniHandle<>(d, this, f));
        } else {
            d.uniHandle(r, f, null);
        }
        return d;
    }

    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> fn;

        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src,
                         Function<? super Throwable, ? extends T> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            Object r;
            Function<? super Throwable, ? extends T> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniExceptionally(r, f, mode > 0 ? null : this)) {
                return null;
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniExceptionally(Object r,
                                   Function<? super Throwable, ? extends T> f,
                                   UniExceptionally<T> c) {
        Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim()) {
                    return false;
                }
                if (r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
                    completeValue(f.apply(x));
                } else {
                    internalComplete(r);
                }
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private CompletableFuture<T> uniExceptionallyStage(Function<Throwable, ? extends T> f) {
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;
        if ((r = result) == null) {
            unipush(new UniExceptionally<>(d, this, f));
        } else {
            d.uniExceptionally(r, f, null);
        }
        return d;
    }

    static final class UniRelay<U, T extends U> extends UniCompletion<T, U> {
        UniRelay(CompletableFuture<U> dep, CompletableFuture<T> src) {
            super(dep, src);
        }

        CompletableFuture<U> tryFire(int mode) {
            CompletableFuture<U> d;
            CompletableFuture<T> a;
            Object r;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null) {
                return null;
            }
            if (d.result == null) {
                d.completeRelay(r);
            }
            src = null;
            dep = null;
            return d.postFire(a, mode);
        }
    }

    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletableFuture<V>> fn;

        UniCompose(Executor executor, CompletableFuture<V> dep,
                   CompletableFuture<T> src,
                   Function<? super T, ? extends CompletableFuture<V>> fn) {
            super(dep, src);
            this.fn = fn;
        }

        CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Function<? super T, ? extends CompletableFuture<V>> f;
            Object r;
            Throwable x;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null) {
                return null;
            }
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim()) {
                        return null;
                    }
                    @SuppressWarnings("unchecked") T t = (T) r;
                    CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                    if ((r = g.result) != null) {
                        d.completeRelay(r);
                    } else {
                        g.unipush(new UniRelay<>(d, g));
                        if (d.result == null) {
                            return null;
                        }
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    private <V> CompletableFuture<V> uniComposeStage(Executor e, Function<? super T, ? extends CompletableFuture<V>> f) {
        if (f == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> d = newIncompleteFuture();
        Object r, s;
        Throwable x;
        if ((r = result) == null) {
            unipush(new UniCompose<>(e, d, this, f));
        } else {
            if (r instanceof AltResult) {
                if ((x = ((AltResult) r).ex) != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                r = null;
            }
            try {
                if (e != null) {
                    e.execute(new UniCompose<>(null, d, this, f));
                } else {
                    @SuppressWarnings("unchecked") T t = (T) r;
                    CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                    if ((s = g.result) != null) {
                        d.result = encodeRelay(s);
                    } else {
                        g.unipush(new UniRelay<>(d, g));
                    }
                }
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    /* ------------- Two-input Completions -------------- */

    /**
     * A Completion for an action with two sources
     */
    abstract static class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> snd; // second source for action

        BiCompletion(Executor executor, CompletableFuture<V> dep,
                     CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(dep, src);
            this.snd = snd;
        }
    }

    /**
     * A Completion delegating to a BiCompletion
     */
    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;

        CoCompletion(BiCompletion<?, ?, ?> base) {
            this.base = base;
        }

        CompletableFuture<?> tryFire(int mode) {
            BiCompletion<?, ?, ?> c;
            CompletableFuture<?> d;
            if ((c = base) == null || (d = c.tryFire(mode)) == null) {
                return null;
            }
            base = null; // detach
            return d;
        }

        boolean isLive() {
            BiCompletion<?, ?, ?> c;
            return (c = base) != null
                   && c.dep != null;
        }
    }

    /**
     * Pushes completion to this and b unless both done. Caller should first check that either result or b.result is null.
     */
    final void bipush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if (c != null) {
            while (result == null) {
                if (tryPushStack(c)) {
                    if (b.result == null) {
                        b.unipush(new CoCompletion(c));
                    } else if (result != null) {
                        c.tryFire(SYNC);
                    }
                    return;
                }
            }
            b.unipush(c);
        }
    }

    /**
     * Post-processing after successful BiCompletion tryFire.
     */
    final CompletableFuture<T> postFire(CompletableFuture<?> a,
                                        CompletableFuture<?> b, int mode) {
        if (b != null && b.stack != null) { // clean second source
            Object r;
            if ((r = b.result) == null) {
                b.cleanStack();
            }
            if (mode >= 0 && (r != null || b.result != null)) {
                b.postComplete();
            }
        }
        return postFire(a, mode);
    }

    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> { // for And
        BiRelay(CompletableFuture<Void> dep,
                CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s, z;
            Throwable x;
            if ((a = src) == null || (r = a.result) == null
                || (b = snd) == null || (s = b.result) == null
                || (d = dep) == null) {
                return null;
            }
            if (d.result == null) {
                if ((r instanceof AltResult
                     && (x = ((AltResult) (z = r)).ex) != null) ||
                    (s instanceof AltResult
                     && (x = ((AltResult) (z = s)).ex) != null)) {
                    d.completeThrowable(x, z);
                } else {
                    d.completeNull();
                }
            }
            src = null;
            snd = null;
            dep = null;
            return d.postFire(a, b, mode);
        }
    }

    /**
     * Recursively constructs a tree of completions.
     */
    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs,
                                           int lo, int hi) {
        var d = new CompletableFuture<Void>();
        if (lo > hi) {  // empty
            d.result = NIL;
        } else {
            CompletableFuture<?> a, b;
            Object r, s, z;
            Throwable x;
            int mid = (lo + hi) >>> 1;
            if ((a = (lo == mid ? cfs[lo] :
                      andTree(cfs, lo, mid))) == null ||
                (b = (lo == hi ? a : (hi == mid + 1) ? cfs[hi] :
                                     andTree(cfs, mid + 1, hi))) == null) {
                throw new NullPointerException();
            }
            if ((r = a.result) == null || (s = b.result) == null) {
                a.bipush(b, new BiRelay<>(d, a, b));
            } else if ((r instanceof AltResult
                        && (x = ((AltResult) (z = r)).ex)
                           != null) ||
                       (s instanceof AltResult
                        && (x = ((AltResult) (z = s)).ex)
                           != null)) {
                d.result = encodeThrowable(x, z);
            } else {
                d.result = NIL;
            }
        }
        return d;
    }

    /* ------------- Signallers -------------- */

    /**
     * Completion for recording and releasing a waiting thread.  This class implements ManagedBlocker to avoid starvation when blocking actions pile
     * up in ForkJoinPools.
     */
    static final class Signaller extends Completion
        implements ForkJoinPool.ManagedBlocker {
        long nanos;                    // remaining wait time if timed
        final long deadline;           // non-zero if timed
        boolean interrupted;
        volatile Thread thread;

        Signaller(long nanos, long deadline) {
            this.thread = Thread.currentThread();
            this.nanos = nanos;
            this.deadline = deadline;
        }

        CompletableFuture<?> tryFire(int ignore) {
            Thread w; // no need to atomically claim
            if ((w = thread) != null) {
                thread = null;
                LockSupport.unpark(w);
            }
            return null;
        }

        public boolean isReleasable() {
            if (Thread.interrupted()) {
                interrupted = true;
            }
            return ((deadline != 0L
                     && (nanos <= 0L || (nanos = deadline - System.nanoTime()) <= 0L)) || thread == null);
        }

        public boolean block() {
            while (!isReleasable()) {
                if (deadline == 0L) {
                    LockSupport.park(this);
                } else {
                    LockSupport.parkNanos(this, nanos);
                }
            }
            return true;
        }

        boolean isLive() {
            return thread != null;
        }
    }

    private Object waitingGet() {
        Signaller q = null;
        boolean queued = false;
        Object r;
        while ((r = result) == null) {
            if (q == null) {
                q = new Signaller(0L, 0L);
            } else if (!queued) {
                queued = tryPushStack(q);
            } else {
                try {
                    ForkJoinPool.managedBlock(q);
                } catch (InterruptedException ie) { // currently cannot happen
                    q.interrupted = true;
                }
            }
        }
        if (q != null) {
            q.thread = null;
            if (q.interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        postComplete();
        return r;
    }

    /* ------------- public methods -------------- */

    /**
     * Creates a new incomplete CompletableFuture.
     */
    public CompletableFuture() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public T join() {
        Object r;
        if ((r = result) == null) {
            r = waitingGet();
        }
        return (T) reportJoin(r);
    }

    @Override
    public CompletableFuture<T> complete(T value) {
        completeValue(value);
        postComplete();
        return this;
    }

    @Override
    public void completeExceptionally(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }

        internalComplete(new AltResult(ex));
        postComplete();
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }

        return uniApplyStage(fn);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }

        return uniAcceptStage(action);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends IntermediateFuture<U>> fn) {
        return uniComposeStage(null, (Function<? super T, ? extends CompletableFuture<U>>) fn);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return uniWhenCompleteStage(action);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }

        return uniHandleStage(fn);
    }

    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return uniExceptionallyStage(fn);
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<>();
    }

    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }

    private static final VarHandle RESULT;
    private static final VarHandle STACK;
    private static final VarHandle NEXT;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT = l.findVarHandle(CompletableFuture.class, "result", Object.class);
            STACK = l.findVarHandle(CompletableFuture.class, "stack", Completion.class);
            NEXT = l.findVarHandle(Completion.class, "next", Completion.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.org/browse/JDK-8074773
        @SuppressWarnings("unused")
        Class<?> ensureLoaded = LockSupport.class;
    }
}
