import dev.mccue.parser.elm.advanced.Parser;
import dev.mccue.parser.elm.advanced.Token;
import dev.mccue.parser.elm.advanced.Trailing;
import dev.mccue.parser.elm.advanced.Witness;
import io.vavr.collection.Vector;

import static dev.mccue.parser.elm.advanced.Parser.*;

record Point(int x, int y) {}

sealed interface Expr {}
record Int(int value) implements Expr {}
record Abs(Expr expr) implements Expr {}
record Add(Expr a, Expr b) implements Expr {}


Parser<Object, Object, Int> parseInt() {
    return map(Int::new, int_("Expected an Int", "Invalid Int"));
}

Parser<Object, Object, Abs> parseAbs() {
    return Parser.of1(Abs::new)
            .__(token("|", "Expected opening Absolute value | mark"))
            .__(spaces())
            ._$(lazy(this::parseExpr))
            .__(spaces())
            .__(token("|", "Expected closing Absolute value | mark"));
}

Parser<Object, Object, Add> parseAdd() {
    return Parser.of2(Add::new)
            ._$(lazy(this::parseExpr))
            .__(spaces())
            .__(token("+", "Expected + sign"))
            .__(spaces())
            ._$(lazy(this::parseExpr));
}

Parser<Object, Object, Expr> parseExpr() {
    return oneOf(Vector.of(
            backtrackable(narrowValue(parseAdd())),
            narrowValue(parseInt()),
            narrowValue(parseAbs())
    ));
}

Parser<Object, Object, Expr> parseTopLevelExpr() {
    return Parser.of0(parseExpr())
            .__(spaces())
            .__(end("Expected end of string"));
}
void main() {
    IO.println(
            run(parseTopLevelExpr(), "|12| + |34|")
    );
}
