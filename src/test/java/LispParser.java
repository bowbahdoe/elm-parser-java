import dev.mccue.parser.elm.advanced.Parser;
import dev.mccue.parser.elm.advanced.Trailing;
import io.vavr.Function1;
import io.vavr.collection.HashSet;
import io.vavr.collection.Vector;

import static dev.mccue.parser.elm.advanced.Parser.*;

sealed interface LispValue {
    static Parser<Object, Object, LispValue> parser() {
        return Parser.of1(Function1.<LispValue>identity())
                .__(spaces())
                ._$(oneOf(Vector.of(
                        narrowValue(List.parser()),
                        narrowValue(Symbol.parser())
                )))
                .__(spaces());
    }
}

record List(Vector<LispValue> values) implements LispValue {
    static Parser<Object, Object, List> parser() {
        return Parser.of1(List::new)
                ._$(sequence(
                        token("(", "Expected open paren"),
                        spaces(),
                        token(")", "Expected closing paren"),
                        spaces(),
                        lazy(LispValue::parser),
                        Trailing.OPTIONAL
                ));
    }
}

record Symbol(String name) implements LispValue {
    static Parser<Object, Object, Symbol> parser() {
        return Parser.of1(Symbol::new)
                ._$(variable(
                        c -> !Character.isWhitespace(c) && c != '(' && c != ')',
                        c -> !Character.isWhitespace(c) && c != '(' && c != ')',
                        HashSet.empty(),
                        "Expected a Valid Symbol"
                ));
    }
}

void evaluate(LispValue lispValue) {
    switch (lispValue) {
        case Symbol symbol -> {

        }
        case List(var values) -> {
            if (values.isEmpty()) {
                throw new RuntimeException("No function in function call position");
            }
        }
    }
}



void main() {
    IO.println(
            run(LispValue.parser(), "(inc a (f  b abc c ))")
    );
}