package dev.mccue.parser.elm.advanced;

public sealed interface PStep<Context, Problem, Value> {
    /*value*/ record Good<Context, Problem, Value>(
            boolean b,
            Value value,
            State<Context> state
    ) implements PStep<Context, Problem, Value> {}

    /*value*/ record Bad<Context, Problem, Value>(
            boolean b,
            Bag<Context, Problem> bag
    ) implements PStep<Context, Problem, Value> {}

    // Good Bool value (State context)
    //  | Bad Bool (Bag context problem)
}
