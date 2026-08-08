package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public /*value*/ record ParserPipeline1<C, X, A, B>(
        Parser<C, X, Function1<A, B>> value
) implements Parser<C, X, Function1<A, B>> {
    public ParserPipeline1<C, X, A, B> __(
            Parser<C, X, ?> ignoreParser
    ) {
        return new ParserPipeline1<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline0<C, X, B> _$(
            Parser<C, X, A> parseA
    ) {
        return new ParserPipeline0<>(Parser.keeper(value, parseA));
    }


    @Override
    public PStep<C, X, Function1<A, B>> apply(State<C> cState) {
        return value.apply(cState);
    }
}
