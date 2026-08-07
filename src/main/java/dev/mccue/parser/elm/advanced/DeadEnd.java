package dev.mccue.parser.elm.advanced;

import io.vavr.collection.Seq;

public record DeadEnd<Context, Problem>(
        int row,
        int col,
        Problem problem,
        Seq<Located<Context>> contextStack
) {
}
