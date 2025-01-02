/*
    Digression, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

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

import anticipation.*
import eucalyptus.*, logging.stdout
import fulminate.*
import gossamer.*
import probably.*
import rudiments.*

import unsafeExceptions.canThrowAny

case class Person(name: Text, age: Int)

object Tests extends Suite(t"Rudiments Tests"):
  def run(): Unit =
    suite(t"Exception tests"):
      test(t"Show exception"):
        try
          List(1, 2, 3).map(_ / 0)
          ???
        catch case err: Exception => err.stackTrace
      .assert()
