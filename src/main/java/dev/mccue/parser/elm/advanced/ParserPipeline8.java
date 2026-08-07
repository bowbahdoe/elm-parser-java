package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public record ParserPipeline8<Ctx, X, A, B, C, D, E, F, G, H, I>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, Function1<H, I>>>>>>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, Function1<H, I>>>>>>>>>  {
    public ParserPipeline8<Ctx, X, A, B, C, D, E, F, G, H, I> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline8<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline7<Ctx, X, B, C, D, E, F, G, H, I> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline7<>(Parser.keeper(value, parseA));
    }

    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, Function1<H, I>>>>>>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
