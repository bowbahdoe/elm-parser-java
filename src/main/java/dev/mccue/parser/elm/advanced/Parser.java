package dev.mccue.parser.elm.advanced;

import io.vavr.*;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import java.util.function.Predicate;

// type Parser context problem value =
//  Parser (State context -> PStep context problem value)
public interface Parser<Context, Problem, Value> extends Function1<
        State<Context>,
        PStep<Context, Problem, Value>
        > {
    @SuppressWarnings("unchecked")
    static <C, X, A> Parser<C, X, A> narrowValue(Parser<C, X, ? extends A> parser) {
        return (Parser<C, X, A>) parser;
    }

    static <C, X, A> Either<Seq<DeadEnd<C, X>>, A> run(Parser<C, X, A> parse, String src) {
        return switch (parse.apply(new State<>(
                src,
                0,
                1,
                List.empty(),
                1,
                1
        ))) {
            case PStep.Good(_, var value, _) ->
                Either.right(value);
            case PStep.Bad(_, var bag) ->
                Either.left(bag.toList(List.empty()));
        };
    }

    static <Context, Problem, Value> Parser<Context, Problem, Value> succeed(
            @SuppressWarnings("unused") Witness<Context, Problem> witness,
            Value value
    ) {
        return s -> new PStep.Good<>(false, value, s);
    }

    static <Context, Problem, Value> Parser<Context, Problem, Value> succeed(
            Value value
    ) {
        return succeed(null, value);
    }

    static <Context, Problem, Value> Parser<Context, Problem, Value> problem(
            @SuppressWarnings("unused") Witness<Context, Problem> witness,
            Problem problem
    ) {
        return s -> new PStep.Bad<>(false, Bag.fromState(s, problem));
    }

    static <Context, Problem, Value> Parser<Context, Problem, Value> problem(
            Problem problem
    ) {
        return problem(null, problem);
    }

    static <Context, Problem, A, B> Parser<Context, Problem, B> map(
            @SuppressWarnings("unused") Witness<Context, Problem> witness,
            Function1<A, B> func,
            Parser<Context, Problem, A> parser
    ) {
        return s0 -> switch (parser.apply(s0)) {
            case PStep.Good(var p, var a, var s1) ->
                new PStep.Good<>(p, func.apply(a), s1);
            case PStep.Bad(var p, var x) ->
                    new PStep.Bad<>(p, x);
        };
    }

    static <Context, Problem, A, B> Parser<Context, Problem, B> map(
            Function1<A, B> func,
            Parser<Context, Problem, A> parser
    ) {
        return map(null, func, parser);
    }


    static <Context, Problem, A, B, Value> Parser<Context, Problem, Value> map2(
            @SuppressWarnings("unused") Witness<Context, Problem> witness,
            Function2<A, B, Value> func,
            Parser<Context, Problem, A> parserA,
            Parser<Context, Problem, B> parserB
    ) {
        return s0 -> switch (parserA.apply(s0)) {
            case PStep.Good(var p1, var a, var s1) ->
                    switch (parserB.apply(s1)) {
                        case PStep.Bad(var p2, var x) ->
                            new PStep.Bad<>(p1 || p2, x);
                        case PStep.Good(var p2, var b, var s2) ->
                            new PStep.Good<>(p1 || p2, func.apply(a, b), s2);
                    };
            case PStep.Bad(var p, var x) ->
                    new PStep.Bad<>(p, x);
        };
    }

    static <Context, Problem, A, B, Value> Parser<Context, Problem, Value> map2(
            Function2<A, B, Value> func,
            Parser<Context, Problem, A> parserA,
            Parser<Context, Problem, B> parserB
    ) {
        return map2(null, func, parserA, parserB);
    }

    static <C, X, A, B> Parser<C, X, B> keeper(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Parser<C, X, Function1<A, B>> parseFunc,
            Parser<C, X, A> parseArg
    ) {
        return map2(Function1::apply, parseFunc, parseArg);
    }

    static <C, X, A, B> Parser<C, X, B> keeper(
            Parser<C, X, Function1<A, B>> parseFunc,
            Parser<C, X, A> parseArg
    ) {
        return keeper(null, parseFunc, parseArg);
    }

    static <C, X, Keep> Parser<C, X, Keep> ignorer(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Parser<C, X, Keep> keepParser,
            Parser<C, X, ?> ignoreParser
    ) {
        return map2((a, _) -> a, keepParser, ignoreParser);
    }

    static <C, X, Keep> Parser<C, X, Keep> ignorer(
            Parser<C, X, Keep> keepParser,
            Parser<C, X, ?> ignoreParser
    ) {
        return ignorer(null, keepParser, ignoreParser);
    }


    static <C, X, A, B> Parser<C, X, B> andThen(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Function1<A, Parser<C, X, B>> callback,
            Parser<C, X, A> parseA
    ) {
        return s0 -> switch (parseA.apply(s0)) {
            case PStep.Bad(var p, var x) ->
                new PStep.Bad<>(p, x);
            case PStep.Good(var p1, var a, var s1) -> {
                var parseB = callback.apply(a);
                yield switch (parseB.apply(s1)) {
                    case PStep.Bad(var p2, var x) ->
                        new PStep.Bad<>(p1 || p2, x);
                    case PStep.Good(var p2, var b, var s2) ->
                        new PStep.Good<>(p1 || p2, b, s2);
                };
            }
        };
    }

    static <C, X, A, B> Parser<C, X, B> andThen(
            Function1<A, Parser<C, X, B>> callback,
            Parser<C, X, A> parseA
    ) {
        return andThen(null, callback, parseA);
    }

    static <C, X, A> Parser<C, X, A> lazy(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Function0<Parser<C, X, A>> thunk
    ) {
        return s -> {
            var parse = thunk.apply();
            return parse.apply(s);
        };
    }

    static <C, X, A> Parser<C, X, A> lazy(
            Function0<Parser<C, X, A>> thunk
    ) {
        return lazy(null, thunk);
    }

    static <C, X, A> Parser<C, X, A> oneOf(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Seq<Parser<C, X, A>> parsers
    ) {
        return s -> oneOfHelp(s, new Bag.Empty<>(), parsers);
    }

    static <C, X, A> Parser<C, X, A> oneOf(
            Seq<Parser<C, X, A>> parsers
    ) {
        return oneOf(null, parsers);
    }

    private static <C, X, A> PStep<C, X, A> oneOfHelp(
            State<C> s0,
            Bag<C, X> bag,
            Seq<Parser<C, X, A>> parsers
    ) {
        while (true) {
            if (parsers.isEmpty()) {
                return new PStep.Bad<>(false, bag);
            }
            else {
                var parse = parsers.head();
                var remainingParsers = parsers.tail();
                switch (parse.apply(s0)) {
                    case PStep.Good<C, X, A> step -> {
                            return step;
                    }
                    case PStep.Bad<C, X, A>(var p, var x) -> {
                        if (p) {
                            return new PStep.Bad<>(p, x);
                        }
                        else {
                            bag = new Bag.Append<>(bag, x);
                            parsers = remainingParsers;
                        }
                    }
                };
            }
        }

    }

    static <C, X, A, State> Parser<C, X, A> loop(
            @SuppressWarnings("unused") Witness<C, X> witness,
            State state,
            Function1<State, Parser<C, X, Step<State, A>>> callback
    ) {
        return s -> loopHelp(false, state, callback, s);
    }

    static <C, X, A, State> Parser<C, X, A> loop(
            State state,
            Function1<State, Parser<C, X, Step<State, A>>> callback
    ) {
        return loop(null, state, callback);
    }

    private static <C, X, A, State_> PStep<C, X, A> loopHelp(
            boolean p,
            State_ state,
            Function1<State_, Parser<C, X, Step<State_, A>>> callback,
            State<C> s0
    ) {
        while (true) {
            var parse = callback.apply(state);
            switch (parse.apply(s0)) {
                case PStep.Good(var p1, var step, var s1) -> {
                    switch (step) {
                        case Step.Loop(var newState) -> {
                            p = p || p1;
                            state = newState;
                            s0 = s1;
                        }
                        case Step.Done(var result) -> {
                            return new PStep.Good<>(p || p1, result, s1);
                        }
                    }
                }
                case PStep.Bad(var p1, var x) -> {
                    return new PStep.Bad<>(p || p1, x);
                }
            }
        }
    }

    static <C, X, A> Parser<C, X, A> backtrackable(Parser<C, X, A> parse) {
        return s0 -> switch (parse.apply(s0)) {
            case PStep.Bad(_, var x) ->
                new PStep.Bad<>(false, x);
            case PStep.Good(_, var a, var s1) ->
                new PStep.Good<>(false, a, s1);
        };
    }

    static <C, X, A> Parser<C, X, A> commit(A a) {
        return s -> new PStep.Good<>(true, a, s);
    }

    static <C, X> Parser<C, X, Void> symbol(Token<X> token) {
        return token(token);
    }

    static <C, X> Parser<C, X, Void> symbol(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Token<X> token
    ) {
        return token(token);
    }

    static <C, X> Parser<C, X, Void> symbol(String value, X problem) {
        return token(value, problem);
    }

    static <C, X> Parser<C, X, Void> symbol(
            @SuppressWarnings("unused") Witness<C, X> witness,
            String value,
            X problem
    ) {
        return token(witness, value, problem);
    }

    static <C, X> Parser<C, X, Void> token(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Token<X> token
    ) {
        var str = token.value();
        var expecting = token.problem();

        var progress = !str.isEmpty();
        return s -> {
            var isSubStringResult = isSubString(str, s.offset(), s.row(), s.col(), s.src());
            var newOffset = isSubStringResult._1;
            var newRow = isSubStringResult._2;
            var newCol = isSubStringResult._3;
            if (newOffset == -1) {
                return new PStep.Bad<>(false, Bag.fromState(s, expecting));
            }
            else {
                return new PStep.Good<>(progress, null, new State<>(
                        s.src(),
                        newOffset,
                        s.indent(),
                        s.context(),
                        newRow,
                        newCol
                ));
            }
        };
    }

    static <C, X> Parser<C, X, Void> token(
            Token<X> token
    ) {
        return token((Witness<C, X>) null, token);
    }

    static <C, X> Parser<C, X, Void> token(
            String value,
            X problem
    ) {
        return token((Witness<C, X>) null, new Token<>(value, problem));
    }

    static <C, X> Parser<C, X, Void> token(
            Witness<C, X> witness,
            String value,
            X problem
    ) {
        return token(witness, new Token<>(value, problem));
    }

    static <C, X> Parser<C, X, Void> chompUntil(
            Token<X> token
    ) {
        var str = token.value();
        var expecting = token.problem();

        return s -> {
            var subStringResult = findSubString(str, s.offset(), s.row(), s.col(), s.src());
            var newOffset = subStringResult._1;
            var newRow = subStringResult._2;
            var newCol = subStringResult._3;

            if (newOffset == -1) {
                return new PStep.Bad<>(false, Bag.fromInfo(newRow, newCol, expecting, s.context()));
            }
            else {
                return new PStep.Good<>(s.offset() < newOffset, null, new State<>(
                        s.src(),
                        newOffset,
                        s.indent(),
                        s.context(),
                        newRow,
                        newCol
                ));
            }
        };
    }

    static <C, X> Parser<C, X, Void> chompUntilEndOr(String str) {
        return s -> {
            var subStringResult = findSubString(str, s.offset(), s.row(), s.col(), s.src());
            var newOffset = subStringResult._1;
            var newRow = subStringResult._2;
            var newCol = subStringResult._3;

            var adjustedOffset = newOffset < 0 ? s.src().length() : newOffset;

            return new PStep.Good<>(s.offset() < adjustedOffset, null, new State<>(
                    s.src(),
                    newOffset,
                    s.indent(),
                    s.context(),
                    newRow,
                    newCol
            ));
        };
    }

    static <C, X> Parser<C, X, String> variable(
            Witness<C, X> w,
            Predicate<Integer> start,
            Predicate<Integer> inner,
            Set<String> reserved,
            X expecting
    ) {
        return s -> {
            var firstOffset = isSubChar(start, s.offset(), s.src());

            if (firstOffset == -1) {
                return new PStep.Bad<>(false, Bag.fromState(s, expecting));
            }
            else {
                var s1 = firstOffset == -2
                        ? varHelp(inner, s.offset() + 1, s.row() + 1, 1, s.src(), s.indent(), s.context())
                        : varHelp(inner, firstOffset, s.row(), s.col() + 1, s.src(), s.indent(), s.context());

                var name = s.src().substring(s.offset(), s1.offset());

                if (reserved.contains(name)) {
                    return new PStep.Bad<>(false, Bag.fromState(s, expecting));
                }
                else {
                    return new PStep.Good<>(true, name, s1);
                }
            }
        };
    }

    static <C, X> Parser<C, X, String> variable(
            Predicate<Integer> start,
            Predicate<Integer> inner,
            Set<String> reserved,
            X expecting
    ) {
        return variable(null, start, inner, reserved, expecting);
    }

    static <C> State<C> varHelp(
            Predicate<Integer> isGood,
            int offset,
            int row,
            int col,
            String src,
            int indent,
            Seq<Located<C>> context
    ) {
        while (true) {
            var newOffset = isSubChar(isGood, offset, src);
            if (newOffset == -1) {
                return new State<>(
                        src,
                        offset,
                        indent,
                        context,
                        row,
                        col
                );
            }
            else if (newOffset == -2) {
                offset++;
                row++;
                col=1;
            }
            else {
                offset = newOffset;
                col++;
            }
        }
    }

    static <C, X, A> Parser<C, X, Vector<A>> sequence(
            Token<X> start,
            Token<X> separator,
            Token<X> end,
            Parser<C, X, ?> spaces,
            Parser<C, X, A> item,
            Trailing trailing
    ) {
        return sequence(null, start, separator, end, spaces, item, trailing);
    }

    static <C, X, A> Parser<C, X, Vector<A>> sequence(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Token<X> start,
            Token<X> separator,
            Token<X> end,
            Parser<C, X, ?> spaces,
            Parser<C, X, A> item,
            Trailing trailing
    ) {
        return skip(token(start), skip(spaces, sequenceEnd(token(end), spaces, item, token(separator), trailing)));
    }

    static <C, X, A> Parser<C, X, Vector<A>> sequence(
            Parser<C, X, ?> start,
            Parser<C, X, ?> separator,
            Parser<C, X, ?> end,
            Parser<C, X, ?> spaces,
            Parser<C, X, A> item,
            Trailing trailing
    ) {
        return sequence(null, start, separator, end, spaces, item, trailing);
    }

    static <C, X, A> Parser<C, X, Vector<A>> sequence(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Parser<C, X, ?> start,
            Parser<C, X, ?> separator,
            Parser<C, X, ?> end,
            Parser<C, X, ?> spaces,
            Parser<C, X, A> item,
            Trailing trailing
    ) {
        return skip(start, skip(spaces, sequenceEnd(end, spaces, item, separator, trailing)));
    }



    private static <C, X, A> Parser<C, X, Vector<A>> sequenceEnd(
            Parser<C, X, ?> ender,
            Parser<C, X, ?> ws,
            Parser<C, X, A> parseItem,
            Parser<C, X, ?> sep,
            Trailing trailing
    ) {
        Function1<A, Parser<C, X, Vector<A>>> chompRest = item -> switch (trailing) {
            case FORBIDDEN ->
                    loop(
                            Vector.of(item),
                            items -> sequenceEndForbidden(
                                    ender, ws, parseItem, sep, items
                            )
                    );
            case OPTIONAL ->
                    loop(
                            Vector.of(item),
                            items -> sequenceEndOptional(
                                    ender, ws, parseItem, sep, items
                            )
                    );
            case MANDATORY -> ignorer(
                    skip(ws, skip(sep, skip(ws, loop(
                            Vector.of(item),
                            items -> sequenceEndMandatory(ws, parseItem, sep, items))
                    ))),
                    ender
            );
        };

        return oneOf(Vector.of(
                andThen(chompRest, parseItem),
                map(_ -> Vector.empty(), ender)
        ));
    }

    private static <C, X, A> Parser<C, X, Step<Vector<A>, Vector<A>>> sequenceEndForbidden(
            Parser<C, X, ?> ender,
            Parser<C, X, ?> ws,
            Parser<C, X, A> parseItem,
            Parser<C, X, ?> sep,
            Vector<A> items
    ) {
        return skip(ws, oneOf(Vector.of(
                skip(sep, (skip(ws, map(item -> Step.loop(items.append(item)), parseItem)))),
                map(_ -> Step.done(items), ender)
        )));
    }

    private static <C, X, A> Parser<C, X, Step<Vector<A>, Vector<A>>> sequenceEndOptional(
            Parser<C, X, ?> ender,
            Parser<C, X, ?> ws,
            Parser<C, X, A> parseItem,
            Parser<C, X, ?> sep,
            Vector<A> items
    ) {
        var parseEnd = map(_ -> Step.<Vector<A>, Vector<A>>done(items), ender);

        return skip(ws, oneOf(Vector.of(
                skip(sep, skip(ws, oneOf(Vector.of(
                        map(item -> Step.loop(items.append(item)), parseItem),
                        parseEnd
                )))),
                parseEnd
        )));
    }

    static <C, X, A> Parser<C, X, Step<Vector<A>, Vector<A>>> sequenceEndMandatory(
            Parser<C, X, ?> ws,
            Parser<C, X, A> parseItem,
            Parser<C, X, ?> sep,
            Vector<A> items
    ) {
        return oneOf(Vector.of(
                map(item -> Step.loop(items.append(item)),
                        ignorer(parseItem, ignorer(ws, ignorer(sep, ws)))),
                map(_ -> Step.done(items), succeed(null))
        ));
    }

    static <C, X, A> Parser<C, X, A> skip(
            Parser<C, X, ?> iParser,
            Parser<C, X, A> kParser
    ) {
        return map2((_, b) -> b, iParser, kParser);
    }



    static <C, X> Parser<C, X, Void> keyword(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Token<X> token
    ) {
        var kwd = token.value();
        var expecting = token.problem();

        var progress = !kwd.isEmpty();
        return s -> {
            var isSubStringResult = isSubString(kwd, s.offset(), s.row(), s.col(), s.src());
            var newOffset = isSubStringResult._1;
            var newRow = isSubStringResult._2;
            var newCol = isSubStringResult._3;

            if (newOffset == -1 || 0 < isSubChar(c -> Character.isLetterOrDigit(c) || c == '_', newOffset, s.src())) {
                return new PStep.Bad<>(false, Bag.fromState(s, expecting));
            }
            else {
                return new PStep.Good<>(
                        progress,
                        null,
                        new State<>(
                                s.src(),
                                newOffset,
                                s.indent(),
                                s.context(),
                                newRow,
                                newCol
                        )
                );
            }
        };
    }
    static <C, X> Parser<C, X, Void> keyword(Token<X> token) {
        return keyword((Witness<C, X>) null, token);
    }

    static <C, X> Parser<C, X, Void> end(
            @SuppressWarnings("unused") Witness<C, X> witness,
            X x
    ) {
        return s -> {
            if (s.src().length() == s.offset()) {
                return new PStep.Good<>(false, null, s);
            }
            else {
                return new PStep.Bad<>(false, Bag.fromState(s, x));
            }
        };
    }
    static <C, X> Parser<C, X, Void> end(X x) {
        return end(null, x);
    }

    static <C, X> Parser<C, X, String> getChompedString(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Parser<C, X, ?> parser
    ) {
        return mapChompedString((a, _) -> a, parser);
    }

    static <C, X> Parser<C, X, String> getChompedString(Parser<C, X, ?> parser) {
        return getChompedString(null, parser);
    }


    static <C, X, A, B> Parser<C, X, B> mapChompedString(
            Function2<String, A, B> func,
            Parser<C, X, A> parse
    ) {
        return s0 -> switch (parse.apply(s0)) {
            case PStep.Bad(var p, var x) ->
                new PStep.Bad<>(p, x);
            case PStep.Good(var p, var a, var s1) ->
                new PStep.Good<>(p, func.apply(s0.src().substring(s0.offset(), s1.offset()), a), s1);
        };
    }

    static <C, X> Parser<C, X, Void> chompIf(Predicate<Integer> isGood, X expecting) {
        return s -> {
            var newOffset = isSubChar(isGood, s.offset(), s.src());
            if (newOffset == -1) { // not found
                return new PStep.Bad<>(false, Bag.fromState(s, expecting));
            }
            else if (newOffset == -2) { // newline
                return new PStep.Good<>(
                        true,
                        null,
                        new State<>(
                                s.src(),
                                s.offset() + 1,
                                s.indent(),
                                s.context(),
                                s.row() + 1,
                                1
                        )
                );
            }
            else {
                return new PStep.Good<>(
                        true,
                        null,
                        new State<>(
                                s.src(),
                                newOffset,
                                s.indent(),
                                s.context(),
                                s.row(),
                                s.col() + 1
                        )
                );
            }
        };
    }

    static <C, X> Parser<C, X, Void> chompWhile(
            @SuppressWarnings("unused") Witness<C, X> witness,
            Predicate<Integer> isGood
    ) {
        return s -> chompWhileHelp(isGood, s.offset(), s.row(), s.col(), s);
    }

    static <C, X> Parser<C, X, Void> chompWhile(Predicate<Integer> isGood) {
        return chompWhile(null, isGood);
    }

    static <C, X> PStep<C, X, Void> chompWhileHelp(
            Predicate<Integer> isGood,
            int offset,
            int row,
            int col,
            State<C> s0
    ) {
        while (true) {
            var newOffset = isSubChar(isGood, offset, s0.src());
            if (newOffset == -1) { // no match
                return new PStep.Good<>(
                        s0.offset() < offset,
                        null,
                        new State<>(
                                s0.src(),
                                offset,
                                s0.indent(),
                                s0.context(),
                                row,
                                col
                        )
                );
            } else if (newOffset == -2) { // matched a newline
                offset++;
                row++;
                col = 1;
            } else { // normal match
                offset = newOffset;
                col++;
            }
        }

    }

    static <C, X, A> Parser<C, X, A> inContext(C context, Parser<C, X, A> parse) {
        return s0 -> switch (parse.apply(
                changeContext(
                        s0.context()
                                .prepend(new Located<>(s0.row(), s0.col(), context)),
                        s0
                )
        )) {
            case PStep.Good(var p, var a, var s1) ->
                    new PStep.Good<>(p, a, changeContext(s0.context(), s1));
            case PStep.Bad<C, X, A> step ->
                    step;
        };
    }

    private static <C> State<C> changeContext(Seq<Located<C>> newContext, State<C> s) {
        return new State<>(
                s.src(),
                s.offset(),
                s.indent(),
                newContext,
                s.row(),
                s.col()
        );
    }

    static <C, X> Parser<C, X, Void> spaces(@SuppressWarnings("unused") Witness<C, X> w) {
        return chompWhile(c -> c == ' ' || c == '\n' || c == '\r');
    }
    static <C, X> Parser<C, X, Void> spaces() {
        return spaces(null);
    }

    static <C, X> Parser<C, X, Integer> getIndent() {
        return s -> new PStep.Good<>(false, s.indent(), s);
    }

    static <C, X, A> Parser<C, X, A> withIndent(
            int newIndent,
            Parser<C, X, A> parse
    ) {
        return s0 -> switch (parse.apply(changeIndent(newIndent, s0))) {
            case PStep.Good(var p, var a, var s1) ->
                new PStep.Good<>(p, a, changeIndent(s0.indent(), s1));
            case PStep.Bad(var p, var x) ->
                new PStep.Bad<>(p, x);
        };
    }

    private static <C> State<C> changeIndent(int newIndent, State<C> s) {
        return new State<>(
                s.src(),
                s.offset(),
                newIndent,
                s.context(),
                s.row(),
                s.col()
        );
    }

    static <C, X> Parser<C, X, Tuple2<Integer, Integer>> getPosition() {
        return s -> new PStep.Good<>(false, Tuple.of(s.row(), s.col()), s);
    }

    static <C, X> Parser<C, X, Integer> getRow() {
        return s -> new PStep.Good<>(false, s.row(), s);
    }

    static <C, X> Parser<C, X, Integer> getCol() {
        return s -> new PStep.Good<>(false, s.col(), s);
    }

    static <C, X> Parser<C, X, Integer> getOffset() {
        return s -> new PStep.Good<>(false, s.offset(), s);
    }

    static <C, X> Parser<C, X, String> getSource() {
        return s -> new PStep.Good<>(false, s.src(), s);
    }

    /// When making a fast parser, you want to avoid allocation as much as
    /// possible. That means you never want to mess with the source string, only
    /// keep track of an offset into that string.
    ///
    /// You use `isSubString` like this:
    ///
    ///     isSubString("let", offset, row, col, "let x = 4 in x")
    ///         --==> ( newOffset, newRow, newCol )
    ///
    /// You are looking for `"let"` at a given `offset`. On failure, the
    /// `newOffset` is `-1`. On success, the `newOffset` is the new offset. With
    /// our `"let"` example, it would be `offset + 3`.
    ///
    /// You also provide the current `row` and `col` which do not align with
    /// `offset` in a clean way. For example, when you see a `\n` you are at
    /// `row = row + 1` and `col = 1`. Furthermore, some UTF16 characters are
    /// two words wide, so even if there are no newlines, `offset` and `col`
    /// may not be equal.
    private static Tuple3<Integer, Integer, Integer> isSubString(
            String smallString,
            int offset,
            int row,
            int col,
            String bigString
    ) {
        var smallLength = smallString.length();
        var isGood = offset + smallLength <= bigString.length();
        for (var i = 0; isGood && i < smallLength; )
        {
            var code = bigString.charAt(offset);
            isGood = smallString.charAt(i++) == bigString.charAt(offset++);
            if (code == '\n') {
                row++;
                col = 1;
            }
            else {
                col++;
                if ((code & 0xF800) == 0xD800) {
                    isGood = isGood && smallString.charAt(i++) == bigString.charAt(offset++);
                }
            }
        }

        return Tuple.of(isGood ? offset : -1, row, col);
    }

    private static int chompBase10(int offset, String string) {
        for (; offset < string.length(); offset++)
        {
            var code = string.charAt(offset);
            if (code < 0x30 || 0x39 < code)
            {
                return offset;
            }
        }
        return offset;
    }

    private static Tuple2<Integer, Integer> consumeBase(int base, int offset, String string) {
        int total = 0;
        for (; offset < string.length(); offset++)
        {
            var digit = string.charAt(offset) - 0x30;
            if (digit < 0 || base <= digit) break;
            total = base * total + digit;
        }
        return Tuple.of(offset, total);
    }

    private static Tuple2<Integer, Integer> consumeBase16(int offset, String string) {
        int total = 0;
        for (; offset < string.length(); offset++)
        {
            var code = string.charAt(offset);
            if (0x30 <= code && code <= 0x39)
            {
                total = 16 * total + code - 0x30;
            }
            else if (0x41 <= code && code <= 0x46)
            {
                total = 16 * total + code - 55;
            }
            else if (0x61 <= code && code <= 0x66)
            {
                total = 16 * total + code - 87;
            }
            else
            {
                break;
            }
        }
        return Tuple.of(offset, total);
    }

    private static <C> State<C> bumpOffset(int newOffset, State<C> s) {
        return new State<>(
                s.src(),
                newOffset,
                s.indent(),
                s.context(),
                s.row(),
                s.col() + (newOffset - s.offset())
        );
    }

    static <C, X> Parser<C, X, Integer> int_(
            X expecting,
            X invalid
    ) {
        return number(
                Either.right(Function1.identity()),
                Either.left(invalid),
                Either.left(invalid),
                Either.left(invalid),
                Either.left(invalid),
                invalid,
                expecting
        );
    }

    static <C, X> Parser<C, X, Double> float_(
            X expecting,
            X invalid
    ) {
        return number(
                Either.right(Double::valueOf),
                Either.left(invalid),
                Either.left(invalid),
                Either.left(invalid),
                Either.right(Function1.identity()),
                invalid,
                expecting
        );
    }

    static <C, X, A> Parser<C, X, A> number(
            Either<X, Function1<Integer, A>> int_,
            Either<X, Function1<Integer, A>> hex,
            Either<X, Function1<Integer, A>> octal,
            Either<X, Function1<Integer, A>> binary,
            Either<X, Function1<Double, A>> float_,
            X invalid,
            X expecting
    ) {
        return s -> {
            if (isAsciiCode(0x30 /* 0 */, s.offset(), s.src())) {
                var zeroOffset = s.offset() + 1;
                var baseOffset = zeroOffset + 1;
                if (isAsciiCode(0x78 /* x */, zeroOffset, s.src())) {
                    return finalizeInt(invalid, hex, baseOffset, consumeBase16(baseOffset, s.src()), s);
                }
                else if (isAsciiCode(0x6F /* o */, zeroOffset, s.src())) {
                    return finalizeInt(invalid, octal, baseOffset, consumeBase(8, baseOffset, s.src()), s);
                }
                else if (isAsciiCode(0x62 /* b */, zeroOffset, s.src())) {
                    return finalizeInt(invalid, binary, baseOffset, consumeBase(2, baseOffset, s.src()), s);
                }
                else {
                    return finalizeFloat(invalid, expecting, int_, float_, Tuple.of(zeroOffset, 0), s);
                }
            } else {
                return finalizeFloat(invalid, expecting, int_, float_, consumeBase(10, s.offset(), s.src()), s);
            }
        };
    }

    private static <C, X, A> PStep<C, X, A> finalizeInt(
            X invalid,
            Either<X, Function1<Integer, A>> handler,
            int startOffset,
            Tuple2<Integer, Integer> intPair,
            State<C> s
    ) {
        var endOffset = intPair._1;
        var n = intPair._2;
        if (handler.isLeft()) {
            return new PStep.Bad<>(true, Bag.fromState(s, handler.getLeft()));
        }
        else {
            var toValue = handler.get();
            if (startOffset == endOffset) {
                return new PStep.Bad<>(
                        s.offset() < startOffset,
                        Bag.fromState(s, invalid)
                );
            }
            else {
                return new PStep.Good<>(
                        true,
                        toValue.apply(n),
                        bumpOffset(endOffset, s)
                );
            }
        }
    }

    private static <C, X, A> PStep<C, X, A> finalizeFloat(
            X invalid,
            X expecting,
            Either<X, Function1<Integer, A>> intSettings,
            Either<X, Function1<Double, A>> floatSettings,
            Tuple2<Integer, Integer> intPair,
            State<C> s
    ) {
        var intOffset = intPair._1;
        var floatOffset = consumeDotAndExp(intOffset, s.src());
        if (floatOffset < 0) {
            return new PStep.Bad<>(true,
                    Bag.fromInfo(
                            s.row(),
                            (s.col() - (floatOffset + s.offset())),
                            invalid,
                            s.context()
                    )
            );
        }
        else if (s.offset() == floatOffset) {
            return new PStep.Bad<>(
                    false,
                    Bag.fromState(s, expecting)
            );
        }
        else if (intOffset == floatOffset) {
            return finalizeInt(invalid, intSettings, s.offset(), intPair, s);
        }
        else {
            if (floatSettings.isLeft()) {
                return new PStep.Bad<>(true, Bag.fromState(s, invalid));
            }
            else {
                var toValue = floatSettings.get();
                try {
                    var n = Double.parseDouble(s.src().substring(s.offset(), floatOffset));
                    return new PStep.Good<>(true, toValue.apply(n), bumpOffset(floatOffset, s));
                } catch (NumberFormatException _) {
                    return new PStep.Bad<>(true, Bag.fromState(s, invalid));
                }
            }
        }
    }

    // On a failure, returns negative index of problem.
    private static int consumeDotAndExp(int offset, String src) {
        if (isAsciiCode('.', offset, src)) {
            return consumeExp(chompBase10(offset + 1, src), src);
        }
        else {
            return consumeExp(offset, src);
        }
    }

    // On a failure, returns negative index of problem.
    private static int consumeExp(int offset, String src) {
        if (isAsciiCode(0x65 /* e */, offset, src) || isAsciiCode(0x45 /* E */, offset, src)) {
            var eOffset = offset + 1;
            var expOffset = (
                    isAsciiCode(0x2B /* + */, eOffset, src)
                            || isAsciiCode(0x2D /* - */, eOffset, src)
            )
                    ? eOffset + 1
                    : eOffset;
            var newOffset = chompBase10(expOffset, src);
            if (expOffset == newOffset) {
                return -newOffset;
            }
            else {
                return newOffset;
            }
        }
        else {
            return offset;
        }
    }
    static <C, X> Parser<C, X, Void> lineComment(Token<X> start) {
        return ignorer(token(start), chompUntilEndOr("\n"));
    }

    static <C, X> Parser<C, X, Void> multiComment(Token<X> open, Token<X> close, Nestability nestability) {
        return switch (nestability) {
            case NOT_NESTABLE -> ignorer(token(open), chompUntil(close));
            case NESTABLE -> nestableComment(open, close);
        };
    }



    private static <C, X> Parser<C, X, Void> nestableComment(Token<X> open, Token<X> close) {
        var oStr = open.value();
        var oX = open.problem();

        var cStr = close.value();
        var cX = close.problem();

        if (oStr.isEmpty()) {
            return problem(oX);
        }
        else {
            var openChar = oStr.charAt(0);
            if (cStr.isEmpty()) {
                return problem(cX);
            }
            else {
                var closeChar = cStr.charAt(0);
                Predicate<Integer> isNotRelevant = char_ ->
                        char_ != openChar && char_ != closeChar;
                Parser<C, X, Void> chompOpen = token(open);

                return Parser.ignorer(chompOpen, nestableHelp(isNotRelevant, chompOpen, token(close), cX, 1));
            }
        }
    }

    private static <C, X> Parser<C, X, Void> nestableHelp(
            Predicate<Integer> isNotRelevant,
            Parser<C, X, ?> open,
            Parser<C, X, ?> close,
            X expectingClose,
            int nestLevel
    ) {
        return skip(chompWhile(isNotRelevant), oneOf(Vector.of(
            nestLevel == 1
                    ? map(_ -> null, close)
                    : andThen(_ -> nestableHelp(isNotRelevant, open, close, expectingClose, nestLevel - 1), close),
                andThen(_ -> nestableHelp(isNotRelevant, open, close, expectingClose, nestLevel + 1), open),
                andThen(_ -> nestableHelp(isNotRelevant, open, close, expectingClose, nestLevel), chompIf(_ -> true, expectingClose)
        ))));
    }
    private static Tuple3<Integer, Integer, Integer> findSubString(
            String smallString,
            int offset,
            int row,
            int col,
            String bigString
    ) {
        var newOffset = bigString.indexOf(smallString, offset);
        var target = newOffset < 0 ? bigString.length() : newOffset + smallString.length();

        while (offset < target)
        {
            var code = bigString.charAt(offset++);
            if (code == 0x000A) {
                col = 1;
                row++;
            }
            else {
                col++;
                if ((code & 0xF800) == 0xD800) {
                    offset++;
                }
            }
        }

        return Tuple.of(newOffset, row, col);
    }


    private static boolean isAsciiCode(int code, int offset, String string) {
        // From what I can tell, sometimes float_() will look ahead beyond the length
        // of the string. This was probably taking advantage of the fact that you would
        // get NaN going out of bounds in JS.
        return offset < string.length() && string.charAt(offset) == code;
    }

    private static int isSubChar(Predicate<Integer> predicate, int offset, String string) {
        if (string.length() <= offset) {
            return -1;
        }
        else {
            if ((string.charAt(offset) & 0xF800) == 0xD800) {
                if (predicate.test(Character.toCodePoint(string.charAt(offset), string.charAt(offset + 1)))) {
                    return offset + 2;
                }
                else {
                    return -1;
                }
            } else {
                if (predicate.test((int) string.charAt(offset))) {
                    if (string.charAt(offset) == '\n') {
                        return -2;
                    }
                    else {
                        return offset + 1;
                    }
                }
                else {
                    return -1;
                }
            }
        }
    }

    static <C, X, A> ParserPipeline0<C, X, A> of0(A a) {
        return new ParserPipeline0<>(Parser.succeed(a));
    }

    static <C, X, A, B> ParserPipeline1<C, X, A, B> of1(Function1<A, B> f) {
        return new ParserPipeline1<>(Parser.succeed(f));
    }

    static <Ctx, X, A, B, C> ParserPipeline2<Ctx, X, A, B, C> of2(Function2<A, B, C> f) {
        return new ParserPipeline2<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D> ParserPipeline3<Ctx, X, A, B, C, D> of3(Function3<A, B, C, D> f) {
        return new ParserPipeline3<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E> ParserPipeline4<Ctx, X, A, B, C, D, E> of4(Function4<A, B, C, D, E> f) {
        return new ParserPipeline4<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F> ParserPipeline5<Ctx, X, A, B, C, D, E, F> of5(Function5<A, B, C, D, E, F> f) {
        return new ParserPipeline5<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G> ParserPipeline6<Ctx, X, A, B, C, D, E, F, G> of6(Function6<A, B, C, D, E, F, G> f) {
        return new ParserPipeline6<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G, H> ParserPipeline7<Ctx, X, A, B, C, D, E, F, G, H> of7(Function7<A, B, C, D, E, F, G, H> f) {
        return new ParserPipeline7<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G, H, I> ParserPipeline8<Ctx, X, A, B, C, D, E, F, G, H, I> of8(Function8<A, B, C, D, E, F, G, H, I> f) {
        return new ParserPipeline8<>(Parser.succeed(f.curried()));
    }

    // witnessed

    static <C, X, A, B> ParserPipeline0<C, X, A> of0(Witness<C, X> w, A a) {
        return new ParserPipeline0<>(Parser.succeed(a));
    }

    static <C, X, A, B> ParserPipeline1<C, X, A, B> of1(Witness<C, X> w, Function1<A, B> f) {
        return new ParserPipeline1<>(Parser.succeed(f));
    }

    static <Ctx, X, A, B, C> ParserPipeline2<Ctx, X, A, B, C> of2(Witness<Ctx, X> w, Function2<A, B, C> f) {
        return new ParserPipeline2<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D> ParserPipeline3<Ctx, X, A, B, C, D> of3(Witness<Ctx, X> w, Function3<A, B, C, D> f) {
        return new ParserPipeline3<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E> ParserPipeline4<Ctx, X, A, B, C, D, E> of4(Witness<Ctx, X> w, Function4<A, B, C, D, E> f) {
        return new ParserPipeline4<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F> ParserPipeline5<Ctx, X, A, B, C, D, E, F> of5(Witness<Ctx, X> w, Function5<A, B, C, D, E, F> f) {
        return new ParserPipeline5<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G> ParserPipeline6<Ctx, X, A, B, C, D, E, F, G> of6(Witness<Ctx, X> w, Function6<A, B, C, D, E, F, G> f) {
        return new ParserPipeline6<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G, H> ParserPipeline7<Ctx, X, A, B, C, D, E, F, G, H> of7(Witness<Ctx, X> w, Function7<A, B, C, D, E, F, G, H> f) {
        return new ParserPipeline7<>(Parser.succeed(f.curried()));
    }

    static <Ctx, X, A, B, C, D, E, F, G, H, I> ParserPipeline8<Ctx, X, A, B, C, D, E, F, G, H, I> of8(Witness<Ctx, X> w, Function8<A, B, C, D, E, F, G, H, I> f) {
        return new ParserPipeline8<>(Parser.succeed(f.curried()));
    }

    static <C, X, A> ParserPipeline0<C, X, A> of0(Parser<C, X, A> parser) {
        return new ParserPipeline0<>(parser);
    }

    static <C, X, A> ParserPipeline0<C, X, A> of0(Witness<C,X> w, Parser<C, X, A> parser) {
        return new ParserPipeline0<>(parser);
    }

    static <C, X, A, B> ParserPipeline1<C, X, A, B> of1(Parser<C, X, Function1<A, B>> parser) {
        return new ParserPipeline1<>(parser);
    }

    static <C, X, A, B> ParserPipeline1<C, X, A, B> of1(Witness<C,X> w, Parser<C, X, Function1<A, B>> parser) {
        return new ParserPipeline1<>(parser);
    }

    static <Ctx, X, A, B, C> ParserPipeline2<Ctx, X, A, B, C> of2(Parser<Ctx, X, Function1<A, Function1<B, C>>> parser) {
        return new ParserPipeline2<>(parser);
    }

    static <Ctx, X, A, B, C> ParserPipeline2<Ctx, X, A, B, C> of2(Witness<Ctx,X> w, Parser<Ctx, X, Function1<A, Function1<B, C>>> parser) {
        return new ParserPipeline2<>(parser);
    }

}