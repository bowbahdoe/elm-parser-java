package dev.mccue.parser.elm.advanced;

import io.vavr.collection.Seq;

public sealed interface Bag<Context, Problem> {
    /*value*/ record Empty<Context, Problem>() implements Bag<Context, Problem> {}

    /*value*/ record AddRight<Context, Problem>(
            Bag<Context, Problem> bag,
            DeadEnd<Context, Problem> deadEnd
    ) implements Bag<Context, Problem> {
    }

    /*value*/ record Append<Context, Problem>(
            Bag<Context, Problem> a,
            Bag<Context, Problem> b
    ) implements Bag<Context, Problem> {
    }

    static <Context, Problem> Bag<Context, Problem> fromState(State<Context> state, Problem problem) {
        return new AddRight<>(
                new Empty<>(),
                new DeadEnd<>(state.row(), state.col(), problem, state.context())
        );
    }

    static <Context, Problem> Bag<Context, Problem> fromInfo(
            int row,
            int col,
            Problem problem,
            Seq<Located<Context>> context
    ) {
        return new AddRight<>(new Empty<>(), new DeadEnd<>(row, col, problem, context));
    }

    default Seq<DeadEnd<Context, Problem>> toList(Seq<DeadEnd<Context, Problem>> list) {
        return switch (this) {
            case Empty() -> list;
            case AddRight(var bag1, var x) -> bag1.toList(list.prepend(x));
            case Append(var bag1, var bag2) -> bag1.toList(bag2.toList(list));
        };
    }

}
