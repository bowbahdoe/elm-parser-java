import com.google.common.base.Stopwatch;
import dev.mccue.parser.elm.advanced.Parser;
import dev.mccue.parser.elm.advanced.Trailing;
import io.vavr.collection.Seq;

import static dev.mccue.parser.elm.advanced.Parser.*;
import static dev.mccue.parser.elm.advanced.Parser.symbol;

record Point(int x, int y) {}

Parser<Object, Object, Point> point = Parser.of2( Point::new)
        .__(symbol("(", "Missing open Paren"))
        .__(spaces())
        ._$(int_("Expecting an int", "Invalid int"))
        .__(spaces())
        .__(symbol(",", "Missing a comma"))
        .__(spaces())
        ._$(int_("Expecting an int", "Invalid int"))
        .__(spaces())
        .__(symbol(")", "Missing Closing Paren"))
        .value();


void main() throws Exception {
    var contents = Files.readString(Path.of("points.txt"));
    var points = sequence(
            token("[", "Missing open square brace"),
            token(";", ""),
            token("]", "Missing close brace"),
            spaces(),
            point,
            Trailing.OPTIONAL
    );

    for (int i = 0; i < 10; i++) {
        var sw = Stopwatch.createStarted();
        var result = run(
                points, contents
        );

        IO.println(sw.elapsed());
        if (result.isRight()) {
            IO.println("OK");
        }
        else {
            IO.println("ERR");
        }
    }




}
