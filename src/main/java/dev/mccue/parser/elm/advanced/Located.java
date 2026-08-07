package dev.mccue.parser.elm.advanced;

public record Located<Context>(
        int row,
        int col,
        Context context
) {
}
