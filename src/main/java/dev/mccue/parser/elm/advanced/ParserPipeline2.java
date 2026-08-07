package dev.mccue.parser.elm.advanced;

import io.vavr.Function1;

public record ParserPipeline2<Ctx, X, A, B, C>(
        Parser<Ctx, X, Function1<A, Function1<B, C>>> value
) implements Parser<Ctx, X, Function1<A, Function1<B, C>>> {
    public ParserPipeline2<Ctx, X, A, B, C> __(
            Parser<Ctx, X, ?> ignoreParser
    ) {
        return new ParserPipeline2<>(Parser.ignorer(value, ignoreParser));
    }

    public ParserPipeline1<Ctx, X, B, C> _$(
            Parser<Ctx, X, A> parseA
    ) {
        return new ParserPipeline1<>(Parser.keeper(value, parseA));
    }

    @Override
    public PStep<Ctx, X, Function1<A, Function1<B, C>>> apply(State<Ctx> ctxState) {
        return value.apply(ctxState);
    }
}
