package dev.mccue.parser.elm.advanced;

/// Witness object to aid generic resolution in the context
/// of a parsing session.
///
/// Java has a bit of trouble tracking this and this is
/// a low rent way to thread it through multiple expressions.
///
/// Is it needed? NOPE!, but it is fun.
@SuppressWarnings("unused")
public record Witness<C, X>() {
}
