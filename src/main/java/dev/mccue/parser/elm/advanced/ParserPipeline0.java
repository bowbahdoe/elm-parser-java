package dev.mccue.parser.elm.advanced;

public /*value*/ record ParserPipeline0<C, X, A>(Parser<C, X, A> value) implements Parser<C, X, A> {
    public ParserPipeline0<C, X, A> __(
            Parser<C, X, ?> ignoreParser
    ) {
        return new ParserPipeline0<>(Parser.ignorer(value, ignoreParser));
    }

    @Override
    public PStep<C, X, A> apply(State<C> cState) {
        return value.apply(cState);
    }
}
