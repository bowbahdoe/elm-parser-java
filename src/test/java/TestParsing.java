import dev.mccue.parser.elm.advanced.Parser;
import dev.mccue.parser.elm.advanced.Token;
import dev.mccue.parser.elm.advanced.Trailing;
import dev.mccue.parser.elm.advanced.Witness;
import io.vavr.Function1;
import io.vavr.Function2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;


import static dev.mccue.parser.elm.advanced.Parser.*;

enum Prob {
    EXPECTED_WHITESPACE,
    EXPECTED_MODULE_NAME,
    EXPECTED_SEMICOLON,
    EXPECTED_COMMA,
    EXPECTED_MODULE,
    EXPECTED_REQUIRES,
    EXPECTED_EOF,
    EXPECTED_OPENING_BRACE,
    EXPECTED_CLOSING_BRACE,
    EXPECTED_STATIC,
    EXPECTED_TRANSITIVE,
    DUPLICATE_STATIC,
    DUPLICATE_TRANSITIVE,
    EXPECTED_TO,
    EXPECTED_PACKAGE_NAME,
    EXPECTED_CLASS_NAME
}

enum Ctx {
    PARSING_MODULE_INFO,
    PARSING_REQUIRES,
    PARSING_DECLARATION,
    PARSING_USES,
    PARSING_EXPORTS,
    PARSING_PROVIDES
}

record ModuleDeclaration(String name, Vector<Declaration> declarations) {
}

record StaticAndTransitive(boolean static_, boolean transitive) {
}

sealed interface Declaration {
    record Exports(String package_, Vector<String> to_) implements Declaration {
    }

    record Requires(
            StaticAndTransitive staticAndTransitive,
            String module
    ) implements Declaration {
    }

    record Uses(String service) implements Declaration {
    }

    record Provides(String service, Vector<String> with_) {}
}

public class TestParsing {
    <C, X> Parser<C, X, String> moduleName(Witness<C, X> w, X problem) {
        return variable(
                w,
                Character::isAlphabetic,
                c -> Character.isLetterOrDigit(c) || c == '.',
                HashSet.empty(),
                problem
        );
    }

    <C, X> Parser<C, X, String> packageName(Witness<C, X> w, X problem) {
        return variable(
                w,
                Character::isAlphabetic,
                c -> Character.isLetterOrDigit(c) || c == '.',
                HashSet.empty(),
                problem
        );
    }

    <C, X> Parser<C, X, String> className(Witness<C, X> w, X problem) {
        return variable(
                w,
                Character::isAlphabetic,
                c -> Character.isLetterOrDigit(c) || c == '.',
                HashSet.<String>empty(),
                problem
        );
    }

    <C, X> Parser<C, X, ?> zeroOrMoreWhitespace(Witness<C, X> w) {
        return chompWhile(w, Character::isWhitespace);
    }

    <C, X> Parser<C, X, ?> oneOrMoreWhitespace(Witness<C, X> w, X prob) {
        return ignorer(
                token(w, " ", prob),
                chompWhile(w, Character::isWhitespace)
        );
    }

    <C> Parser<C, Prob, ?> oneOrMoreWhitespace(Witness<C, Prob> w) {
        return oneOrMoreWhitespace(w, Prob.EXPECTED_WHITESPACE);
    }

    Parser<Ctx, Prob, Declaration.Requires> requires(Witness<Ctx, Prob> w) {
        return inContext(Ctx.PARSING_REQUIRES, Parser.of2(w, Declaration.Requires::new)
                .__(token("requires", Prob.EXPECTED_REQUIRES))
                .__(oneOrMoreWhitespace(w))
                ._$(staticAndTransitiveParser())
                ._$(moduleName(w, Prob.EXPECTED_REQUIRES))
                .__(zeroOrMoreWhitespace(w))
                .__(token(";", Prob.EXPECTED_SEMICOLON)));
    }

    Parser<Ctx, Prob, Declaration.Uses> uses(Witness<Ctx, Prob> w) {
        return inContext(Ctx.PARSING_USES, Parser.of1(w, Declaration.Uses::new)
                .__(token("uses", Prob.EXPECTED_REQUIRES))
                .__(oneOrMoreWhitespace(w))
                ._$(className(w, Prob.EXPECTED_PACKAGE_NAME))
                .__(zeroOrMoreWhitespace(w))
                .__(token(";", Prob.EXPECTED_SEMICOLON)));
    }

    Parser<Ctx, Prob, Declaration.Provides> provides(Witness<Ctx, Prob> w) {
        return inContext(Ctx.PARSING_MODULE_INFO, Parser.of2(w, Declaration.Provides::new)
                .__(token("provides", Prob.EXPECTED_REQUIRES))
                .__(oneOrMoreWhitespace(w))
                ._$(className(w, Prob.EXPECTED_PACKAGE_NAME))
                .__(oneOrMoreWhitespace(w))
                ._$(oneOf(Vector.of(
                        Parser.of0(w, Vector.<String>empty())
                                .__(token(";", Prob.EXPECTED_SEMICOLON)),
                        Parser.of1(w, Function1.<Vector<String>>identity())
                                ._$(sequence(
                                        new Token<>("with ", Prob.EXPECTED_TO),
                                        new Token<>(",", Prob.EXPECTED_COMMA),
                                        new Token<>(";", Prob.EXPECTED_SEMICOLON),
                                        spaces(),
                                        className(w, Prob.EXPECTED_CLASS_NAME),
                                        Trailing.FORBIDDEN
                                ))
                                .value()
                ))));
    }

    Parser<Ctx, Prob, Declaration.Exports> exports(Witness<Ctx, Prob> w) {
        return inContext(Ctx.PARSING_EXPORTS, Parser.of2(w, Declaration.Exports::new)
                .__(token("exports", Prob.EXPECTED_REQUIRES))
                .__(oneOrMoreWhitespace(w))
                ._$(packageName(w, Prob.EXPECTED_PACKAGE_NAME))
                .__(zeroOrMoreWhitespace(w))
                ._$(oneOf(Vector.of(
                        Parser.of0(w, Vector.<String>empty())
                                .__(token(";", Prob.EXPECTED_SEMICOLON)),
                        Parser.of1(w, Function1.<Vector<String>>identity())
                                ._$(sequence(
                                        new Token<>("to", Prob.EXPECTED_TO),
                                        new Token<>(",", Prob.EXPECTED_COMMA),
                                        new Token<>(";", Prob.EXPECTED_SEMICOLON),
                                        spaces(),
                                        packageName(w, Prob.EXPECTED_PACKAGE_NAME),
                                        Trailing.FORBIDDEN
                                ))
                                .value()
                ))));
    }

    Parser<Ctx, Prob, Declaration> declaration(Witness<Ctx, Prob> w) {
        Seq<Parser<Ctx, Prob, Declaration>> opts = Vector.of(
                narrowValue(requires(w)),
                narrowValue(exports(w)),
                narrowValue(uses(w))
        );

        return inContext(Ctx.PARSING_DECLARATION, oneOf(opts));
    }


    Parser<Ctx, Prob, StaticAndTransitive> staticAndTransitiveParser() {
        var w = new Witness<Ctx, Prob>();
        return Parser.of1(w, Function1.<StaticAndTransitive>identity())
                ._$(
                        oneOf(w, Vector.of(
                                Parser.of2(w, StaticAndTransitive::new)
                                        .__(token("static", Prob.EXPECTED_STATIC))
                                        ._$(succeed(true))
                                        .__(oneOrMoreWhitespace(w))
                                        ._$(oneOf(Vector.of(
                                                Parser.of0(w, true)
                                                        .__(token("transitive", Prob.EXPECTED_TRANSITIVE))
                                                        .__(oneOrMoreWhitespace(w)),
                                                Parser.of0(w, false)
                                        ))),
                                Parser.of2(w, Function2.of(StaticAndTransitive::new).reversed())
                                        .__(token("transitive", Prob.EXPECTED_TRANSITIVE))
                                        ._$(succeed(true))
                                        .__(oneOrMoreWhitespace(w))
                                        ._$(oneOf(Vector.of(
                                                Parser.of0(w, true)
                                                        .__(token("static", Prob.EXPECTED_STATIC))
                                                        .__(oneOrMoreWhitespace(w)),
                                                Parser.of0(w, false)
                                        ))),
                                succeed(new StaticAndTransitive(false, false))
                        ))
                );
    }


    Parser<Ctx, Prob, ModuleDeclaration> moduleDeclaration(Witness<Ctx, Prob> w) {
        return Parser.of2(w, ModuleDeclaration::new)
                .__(zeroOrMoreWhitespace(w))
                .__(token("module", Prob.EXPECTED_MODULE))
                .__(oneOrMoreWhitespace(w, Prob.EXPECTED_WHITESPACE))
                ._$(moduleName(w, Prob.EXPECTED_MODULE_NAME))
                .__(zeroOrMoreWhitespace(w))
                ._$(sequence(
                        new Token<>("{", Prob.EXPECTED_OPENING_BRACE),
                        new Token<>("", Prob.EXPECTED_SEMICOLON),
                        new Token<>("}", Prob.EXPECTED_CLOSING_BRACE),
                        zeroOrMoreWhitespace(w),
                        declaration(w),
                        Trailing.OPTIONAL
                ))
                .__(zeroOrMoreWhitespace(w))
                .__(end(Prob.EXPECTED_EOF))
                .value();
    }

    void main() {
        var w = new Witness<Ctx, Prob>();
        var p = moduleDeclaration(w);
        IO.println(run(p, """
                module gggh {
                // ..
                /*
                
                */
                    exports org.a;
                    uses a;
                    uses b.f;
                    requires static a.b;
                    requires transitive c.d;
                    
                    provides a.b.c with  a, b, c;

                }
                """));



        //run(float_(Prob.EXPECTED_EOF, Prob.EXPECTED_WHITESPACE), "123");
    }
}
