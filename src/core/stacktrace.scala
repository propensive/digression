/*
    Digression, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package digression

import rudiments.*
import vacuous.*
import fulminate.*
import anticipation.*
import contingency.*

import language.experimental.captureChecking

extension (error: Throwable) def stackTrace: StackTrace = StackTrace(error)

object FqcnError:
  enum Reason:
    case InvalidChar(char: Char)
    case InvalidStart(char: Char)
    case EmptyName
    case JavaKeyword(keyword: Text)
  
  given communicable: Communicable[Reason] =
    case Reason.InvalidChar(char)    => msg"a package name may not contain the character $char"
    case Reason.InvalidStart(char)   => msg"a package name may not start with the character $char"
    case Reason.EmptyName            => msg"a package name cannot be empty"
    case Reason.JavaKeyword(keyword) => msg"a package name cannot be the Java keyword, $keyword"

case class FqcnError(name: Text, reason: FqcnError.Reason)
extends Error(msg"the class name $name is not valid because $reason")

object Fqcn:
  def valid(char: Char): Boolean =
    char >= 'A' && char <= 'Z' || char >= 'a' && char <= 'z' || char >= '0' && char <= '9' || char == '_'

  def apply(name: Text): Fqcn raises FqcnError =
    val parts = IArray.from(name.s.split("\\.").nn.map(_.nn))
    
    parts.foreach: part =>
      if part.length == 0 then raise(FqcnError(name, FqcnError.Reason.EmptyName))(())
      if Digression.javaKeywords.has(part) then raise(FqcnError(name, FqcnError.Reason.JavaKeyword(part.tt)))(())
      
      part.foreach: char =>
        if !valid(char) then raise(FqcnError(name, FqcnError.Reason.InvalidChar(char)))(())

      if part.head >= '0' && part.head <= '9'
      then raise(FqcnError(name, FqcnError.Reason.InvalidStart(part.head)))(())
    
    new Fqcn(parts.map(_.tt))
    


class Fqcn(val parts: IArray[Text]):
  def text: Text = parts.mkString(".").tt
  def className: Text = parts.last
  def packageName: Text = parts.init.mkString(".").tt

object StackTrace:
  case class Method(className: Text, method: Text)
  case class Frame(method: Method, file: Text, line: Optional[Int], native: Boolean)
  
  val legend: Map[Text, Text] = Map(
    Text("λₙ") -> Text("anonymous function"),
    Text("αₙ") -> Text("anonymous class"),
    Text("ι")  -> Text("initialization"),
    Text("ς")  -> Text("super reference"),
    Text("⋮ε") -> Text("extension method"),
    Text("ϕ")  -> Text("direct"),
    Text("⋮π") -> Text("package file"),
    Text("ⲛ")  -> Text("class initializer"),
    Text("ℓ")  -> Text("lazy initializer")
  )

  def rewrite(name: String, method: Boolean = false): Text =
    val buffer: StringBuilder = StringBuilder()
    
    inline def char(idx: Int): Optional[Char] =
      if idx < 0 || idx >= name.length then Unset else name.charAt(idx)

    @tailrec
    def recur(idx: Int, digits: Boolean = false): Text =
      inline def token(idx: Int, str: String, text: String, digits: Boolean = false): Text =
        if (0 until str.length).all { i => char(idx + i) == str(i) }
        then
          buffer.append(text)
          recur(idx + str.length, digits)
        else
          buffer.append('#')
          recur(idx + 1, digits)
      
      inline def skip(): Text = token(idx, "$", if method then "()." else "#")

      if idx >= name.length then Text(buffer.toString+(if method then "()" else ""))
      else if digits then char(idx) match
        case '0' => token(idx, "0", "₀", true)
        case '1' => token(idx, "1", "₁", true)
        case '2' => token(idx, "2", "₂", true)
        case '3' => token(idx, "3", "₃", true)
        case '4' => token(idx, "4", "₄", true)
        case '5' => token(idx, "5", "₅", true)
        case '6' => token(idx, "6", "₆", true)
        case '7' => token(idx, "7", "₇", true)
        case '8' => token(idx, "8", "₈", true)
        case '9' => token(idx, "9", "₉", true)
        case _   => recur(idx, false)
      else char(idx) match
        case '<' =>
          if (0 until 6).all { i => char(idx + i) == "<init>"(i) }
          then
            buffer.append("ⲛ")
            recur(idx + 6)
          else
            buffer.append("<")
            recur(idx + 1)
        case 'i' =>
          if (0 until 8).all { i => char(idx + i) == "initial$"(i) }
          then
            buffer.append("ι")
            recur(idx + 8)
          else
            buffer.append("i")
            recur(idx + 1)
        case 'l' =>
          if (0 until 7).all { i => char(idx + i) == "lzyINIT"(i) }
          then
            buffer.append("ℓ")
            recur(idx + 7, true)
          else
            buffer.append("l")
            recur(idx + 1)
        case 's' =>
          if (0 until 6).all { i => char(idx + i) == "super$"(i) }
          then
            buffer.append("ς")
            recur(idx + 6)
          else
            buffer.append("s")
            recur(idx + 1)
        case '$' => char(idx + 1) match
          case '_' => token(idx,           "$_avoid_name_clash_$", "′")
          case 'a' => char(idx + 2) match
            case 'm' => token(idx,         "$amp",                 "&")
            case 'n' => char(idx + 5) match
              case 'f' => token(idx,       "$anonfun",             "λ")
              case _   => token(idx,       "$anon",                "α")
            case 't' => token(idx,         "$at",                  "@")
            case _   => skip()
          case 'b' => char(idx + 2) match
            case 'a' => char(idx + 3) match
              case 'n' => token(idx,       "$bang",                "!")
              case 'r' => token(idx,       "$bar",                 "|")
              case _   => skip()
            case 's' => token(idx,         "$bslash",              "\\")
            case _   => skip()
          case 'c' => token(idx,           "$colon",               ":")
          case 'd' => char(idx + 2) match
            case 'e' => token(idx,         "$default",             "δ")
            case 'i' => char(idx + 3) match
              case 'r' => token(idx,       "$direct",              "⋮ϕ")
              case 'v' => token(idx,       "$div",                 "/")
              case _   => skip()
            case _   => skip()
          case 'e' => char(idx + 2) match
            case 'q' => token(idx,         "$eq",                  "=")
            case 'x' => token(idx,         "$extension",           "⋮ε")
            case _   => skip()
          case 'g' => token(idx,           "$greater",             ">")
          case 'h' => token(idx,           "$hash",                "#")
          case 'l' => token(idx,           "$less",                "<")
          case 'm' => token(idx,           "$minus",               "-")
          case 'p' => char(idx + 2) match
            case 'a' => token(idx,         "$package",             "⋮π")
            case 'e' => token(idx,         "$percent",             "%")
            case 'l' => token(idx,         "$plus",                "+")
            case _   => skip()
          case 'q' => token(idx,           "$qmark",               "?")
          case 't' => char(idx + 2) match
            case 'i' => char(idx + 3) match
              case 'l' => token(idx,       "$tilde",               "~")
              case 'm' => token(idx,       "$times",               "*")
              case _   => skip()
            case _  => skip()
          case 'u' => token(idx,           "$up",                  "^")
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' => recur(idx + 1, true)
          case _   => skip()
        case ch  =>
          buffer.append(ch.toString)
          recur(idx + 1)
    
    val rewritten = recur(0)

    def primitive(char: Char) = char match
      case 'Z' => "Boolean"
      case 'I' => "Int"
      case 'J' => "Long"
      case 'V' => "Unit"
      case 'S' => "Short"
      case 'F' => "Float"
      case 'C' => "Char"
      case 'D' => "Double"
      case 'L' => "Any"
      case _   => "?"

    if rewritten.s.startsWith("scala.runtime.java8.JFunction") && rewritten.s.endsWith("#sp") then
      val types: String = rewritten.s.drop(29).dropRight(3)
      types.indexOf("#mc") match
        case -1 | 0 =>
          rewritten
        
        case i =>
          Text:
            val types2 = types.drop(i + 3).to(List).map(primitive)
            if types2.length <= 2 then types2.mkString("(", " => ", ")")
            else types2.init.mkString("((", ", ", s") => ${types2.last})")
    
    else if rewritten.s.startsWith("scala.runtime.function.JProcedure") then Text:
      val n = try rewritten.s.drop(33).toInt catch case error: Exception => 0
      "("+(if n < 2 then s"Any" else List.fill(n)("Any").mkString("(", ", ", ")"))+" => Unit)"
    
    else rewritten

  def apply(exception: Throwable): StackTrace =
    val frames = List(exception.getStackTrace.nn.map(_.nn)*).map: frame =>
      StackTrace.Frame(
        StackTrace.Method(
          rewrite(frame.getClassName.nn),
          rewrite(frame.getMethodName.nn, method = true)
        ),
        Text(Option(frame.getFileName).map(_.nn).getOrElse("[no file]")),
        if frame.getLineNumber < 0 then Unset else frame.getLineNumber,
        frame.isNativeMethod
      )
    
    val cause = Option(exception.getCause)
    val fullClassName: Text = rewrite(exception.getClass.nn.getName.nn)
    val fullClass: List[Text] = List(fullClassName.s.split("\\.").nn.map(_.nn).map(Text(_))*)
    val className: Text = fullClass.last
    
    val component: Text =
      val length = fullClassName.s.length - className.s.length - 1
      Text(fullClassName.s.substring(0, 0.max(length)).nn)
    
    val message: Message = exception match
      case error: Error => error.message
      case other        => Message(Text(Option(exception.getMessage).map(_.nn).getOrElse("")))
    
    StackTrace(component, className, message, frames, cause.map(_.nn).map(StackTrace(_)).optional)

case class StackTrace
    (component: Text, className: Text, message: Message, frames: List[StackTrace.Frame],
        cause: Optional[StackTrace]):

  def crop(cutClassName: Text, cutMethod: Text): StackTrace =
    val frames2 = frames.takeWhile { f => f.method.className != cutClassName || f.method.method != cutMethod }
    StackTrace(component, className, message, frames2, cause)
  
  def drop(n: Int): StackTrace =
    StackTrace(component, className, message, frames.drop(n), cause)
  
  def dropRight(n: Int): StackTrace =
    StackTrace(component, className, message, frames.dropRight(n), cause)
