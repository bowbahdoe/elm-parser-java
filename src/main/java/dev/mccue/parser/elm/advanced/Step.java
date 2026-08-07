package dev.mccue.parser.elm.advanced;

public sealed interface Step<State, A> {
    record Loop<State, A>(State state) implements Step<State, A> {}

    static <State, A> Step<State, A> loop(State state) {
        return new Loop<>(state);
    }

    record Done<State, A>(A value) implements Step<State, A> {}

    static <State, A> Step<State, A> done(A value) {
        return new Done<>(value);
    }
}
