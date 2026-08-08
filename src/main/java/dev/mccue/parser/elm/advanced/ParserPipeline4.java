package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public /*value*/ record ParserPipeline4<Ctx, X, A, B, C, D, E>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, E>>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, E>>>>> {
    public ParserPipeline4<Ctx, X, A, B, C, D, E> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline4<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline3<Ctx, X, B, C, D, E> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline3<>(Parser.keeper(value, parseA));
    }


    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, E>>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
