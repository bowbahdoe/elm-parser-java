package dev.mccue.parser.elm.advanced;

public sealed interface PStep<Context, Problem, Value> {
    record Good<Context, Problem, Value>(
            boolean b,
            Value value,
            State<Context> state
    ) implements PStep<Context, Problem, Value> {}

    record Bad<Context, Problem, Value>(
            boolean b,
            Bag<Context, Problem> bag
    ) implements PStep<Context, Problem, Value> {}

    // Good Bool value (State context)
    //  | Bad Bool (Bag context problem)
}
