package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public /*value*/ record ParserPipeline3<Ctx, X, A, B, C, D>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, D>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, D>>>> {
    public ParserPipeline3<Ctx, X, A, B, C, D> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline3<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline2<Ctx, X, B, C, D> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline2<>(Parser.keeper(value, parseA));
    }


    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, D>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
