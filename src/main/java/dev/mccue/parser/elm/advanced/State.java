package dev.mccue.parser.elm.advanced;

import io.vavr.collection.Seq;

public /*value*/ record State<Context>(
        String src,
        int offset,
        int indent,
        Seq<Located<Context>> context,
        int row,
        int col
){
    @SuppressWarnings("unchecked")
    static <C> State<C> narrow(State<? extends C> s) {
        return (State<C>) s;
    }
}
