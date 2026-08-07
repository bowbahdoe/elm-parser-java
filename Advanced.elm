module Parser.Advanced exposing
  ( Parser, DeadEnd, inContext, Token(..)

 , andThen, problem
  , oneOf, map, backtrackable, commit, token
  , sequence, Trailing(..), loop, Step(..)
  , spaces, lineComment, multiComment, Nestable(..)
  , getChompedString, chompIf, chompWhile, chompUntil, chompUntilEndOr, mapChompedString


  )


import Char
import Elm.Kernel.Parser
import Set



