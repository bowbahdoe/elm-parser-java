package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public record ParserPipeline7<Ctx, X, A, B, C, D, E, F, G, H>(
        Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, H>>>>>>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, H>>>>>>>>  {
    public ParserPipeline7<Ctx, X, A, B, C, D, E, F, G, H> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline7<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline6<Ctx, X, B, C, D, E, F, G, H> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline6<>(Parser.keeper(value, parseA));
    }

    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, Function1<F, Function1<G, H>>>>>>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
