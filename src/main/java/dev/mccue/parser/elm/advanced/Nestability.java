package dev.mccue.parser.elm.advanced;

/// Used to help distinguish
/// between unnestable `/*` `*/` comments like in JS and nestable `{-` `-}`
/// comments like in Elm.
public enum Nestability {
    NOT_NESTABLE,
    NESTABLE
}
