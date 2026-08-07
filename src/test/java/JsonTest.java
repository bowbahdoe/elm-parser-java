import dev.mccue.json.*;
import dev.mccue.parser.elm.advanced.*;
import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import static dev.mccue.parser.elm.advanced.Parser.*;

enum Ctx {}

enum Prob {
    EXPECTED_OPENING_BRACE,
    EXPECTED_CLOSING_BRACE,
    EXPECTED_OPENING_SQUARE_BRACE,
    EXPECTED_CLOSING_SQUARE_BRACE,
    EXPECTED_NUMBER,
    EXPECTED_TRUE,
    EXPECTED_FALSE,
    EXPECTED_NULL,
    EXPECTED_COMMA,
    INVALID_NUMBER,

    EXPECTED_OPENING_QUOTE,
    EXPECTED_CLOSING_QUOTE,
    EXPECTED_COLON
}

Parser<Object, Object, JsonString> stringParser() {
    record State(Vector<Character> chars) {}

    return map(JsonString::of, Parser.of1(Function1.<String>identity())
            .__(token("\"", "Expected opening quote"))
            ._$(loop(new Witness<>(), Vector.<String>empty(), state -> oneOf(
                    Vector.of(
                            map(
                                    s -> Step.loop(state.append(s)),
                                    getChompedString(chompIf(Character::isLetterOrDigit, 3))
                            ),
                            backtrackable(map(_ -> Parser.succeed(Step.done(String.join("", state))),
                                    token("\"", "Expected closing quote for string")))
                    )
            )))
            .__(token("\"", "Expected closing quote")));
}


Parser<Object, Object, Tuple2<JsonString, Json>> mapEntry() {
    return Parser.of2((JsonString s, Json v) -> Tuple.of(s, v))
            .__(spaces())
            ._$(stringParser())
            .__(spaces())
            .__(token(":", Prob.EXPECTED_COLON))
            .__(spaces())
            ._$(narrowValue(jsonParser()));
}

Parser<Object, Object, JsonObject> objectParser() {
    return map(entries -> {
        var obj = JsonObject.builder();
        entries.forEach(t -> obj.put(t._1.toString(), t._2));
        return obj.build();
    }, Parser.of1(Function1.<Vector<Tuple2<JsonString, Json>>>identity())
            .__(token("{", Prob.EXPECTED_OPENING_BRACE))
            .__(spaces())
            ._$(sequence(
                    new Token<>("", Prob.EXPECTED_OPENING_BRACE),
                    new Token<>(",", Prob.EXPECTED_COMMA),
                    new Token<>("}", Prob.EXPECTED_CLOSING_BRACE),
                    spaces(),
                    mapEntry(),
                    Trailing.FORBIDDEN
            )));
}

Parser<Object, Object, JsonNumber> numberParser() {
    return number(
            Either.right(JsonNumber::of),
            Either.right(JsonNumber::of),
            Either.right(JsonNumber::of),
            Either.right(JsonNumber::of),
            Either.right(JsonNumber::of),
            Prob.INVALID_NUMBER,
            Prob.EXPECTED_NUMBER
    );
}

Parser<Object, Object, JsonBoolean> booleanParser() {
    return oneOf(Vector.of(
            map(_ -> JsonBoolean.of(true), token("true", Prob.EXPECTED_TRUE)),
            map(_ -> JsonBoolean.of(false), token("false", Prob.EXPECTED_FALSE))
    ));
}

Parser<Object, Object, JsonNull> nullParser() {
    return map(_ -> JsonNull.instance(), token("null", Prob.EXPECTED_TRUE));
}

Parser<Object, Object, Json> jsonParser() {
    return lazy(() -> oneOf(Vector.of(
            narrowValue(booleanParser()),
            narrowValue(nullParser()),
            narrowValue(numberParser()),
            narrowValue(stringParser()),
            narrowValue(arrayParser()),
            narrowValue(objectParser())
    )));
}

Parser<Object, Object, JsonArray> arrayParser() {
    return map(v -> JsonArray.of(v.asJava()), sequence(
            new Token<>("[", Prob.EXPECTED_OPENING_SQUARE_BRACE),
            new Token<>(",", Prob.EXPECTED_COMMA),
            new Token<>("]", Prob.EXPECTED_CLOSING_SQUARE_BRACE),
            spaces(),
            jsonParser(),
            Trailing.FORBIDDEN
    ));
}

void main() {
    IO.println(run(jsonParser(), "[true, false, null, {\"\": 123}, [true, true], 555, 99, false, [], [[[]]]]"));
}