package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public record ParserPipeline6<Ctx, X, A, B, C, D, E, F, G>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, G>>>>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, G>>>>>>>  {
    public ParserPipeline6<Ctx, X, A, B, C, D, E, F, G> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline6<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline5<Ctx, X, B, C, D, E, F, G> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline5<>(Parser.keeper(value, parseA));
    }

    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, G>>>>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
