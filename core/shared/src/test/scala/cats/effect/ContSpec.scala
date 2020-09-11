/*
 * Copyright 2020 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats
package effect

import org.specs2.specification.core.Execution
import org.specs2.execute._

import cats.implicits._
import scala.concurrent.duration._

class ContSpec extends BaseSpec { outer =>

  def realNoTimeout[A: AsResult](test: => IO[A]): Execution =
    Execution.withEnvAsync(_ => test.unsafeToFuture()(runtime()))


  // TODO move these to IOSpec. Generally review our use of `ticked` in IOSpec
  // various classcast exceptions and/or ByteStack going out of bound
  "get resumes" in realNoTimeout {
    def cont =
      IO.cont[String].flatMap { case (get, resume) =>
        IO(resume(Right("success"))) >> get
      }

    def execute(times: Int, i: Int = 0): IO[Success] =
      if (i == times) IO(success)
      else cont.flatMap(r => IO(r mustEqual "success")) >> execute(times, i + 1)

    execute(100000)
  }

  "callback resumes" in realNoTimeout {
    def cont: IO[Unit] =
      IO.cont[Unit] flatMap { case (get, resume) =>
        for {
          _ <- IO(println("start"))
          _ <- {
            for {
              _ <- IO.sleep(200.millis)
              _ <- IO(println("sleep"))
              _ <- IO(resume(Right(())))
            } yield ()
          }.start
          fib <- get
        } yield ()
      }

    cont.as(true).flatMap { res =>
      IO {
        res must beTrue
      }
    }
  }

}
