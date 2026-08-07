import dev.mccue.parser.elm.advanced.Parser;
import dev.mccue.parser.elm.advanced.Witness;
import io.vavr.Function1;

import static dev.mccue.parser.elm.advanced.Parser.*;

record Point(double x, double y) {}

enum Err {
    MISSING_COMMA
}
enum Ctx {

}


Parser<Ctx, Err, Point> point = Parser.of2(new Witness<Ctx, Err>(), Point::new)
        .__(symbol("(", Err.MISSING_COMMA))
        .__(chompWhile(c -> c == ' ' || c == '\n' || c == '\r' || c == '\t'))
        ._$(float_(Err.MISSING_COMMA, Err.MISSING_COMMA))
        .__(chompWhile(c -> c == ' ' || c == '\n' || c == '\r' || c == '\t'))
        .__(symbol(",", Err.MISSING_COMMA))
        .__(chompWhile(c -> c == ' ' || c == '\n' || c == '\r' || c == '\t'))
        ._$(float_(Err.MISSING_COMMA, Err.MISSING_COMMA))
        .__(chompWhile(c -> c == ' ' || c == '\n' || c == '\r' || c == '\t'))
        .__(symbol(")", Err.MISSING_COMMA))
        .value();

void main() {

    var p = Parser.of1(Function1.<Double>identity())
            .__(spaces())
            ._$(float_(Err.MISSING_COMMA, Err.MISSING_COMMA))
            .__(spaces())
            .__(end(Err.MISSING_COMMA));
    IO.println(

            run(p, " 1.  5  ")
    );

}

/*
type alias Point =
  { x : Float
  , y : Float
  }

point : Parser Point
point =
  succeed Point
    |. symbol "("
    |. spaces
    |= float
    |. spaces
    |. symbol ","
    |. spaces
    |= float
    |. spaces
    |. symbol ")"
 */