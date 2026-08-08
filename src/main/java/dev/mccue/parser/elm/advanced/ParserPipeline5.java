package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public /*value*/ record ParserPipeline5<Ctx, X, A, B, C, D, E, F>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, F>>>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, F>>>>>> {
    public ParserPipeline5<Ctx, X, A, B, C, D, E, F> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline5<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline4<Ctx, X, B, C, D, E, F> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline4<>(Parser.keeper(value, parseA));
    }

    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, F>>>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
