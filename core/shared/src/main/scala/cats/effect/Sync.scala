/*
 * Copyright 2017 Typelevel
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

import simulacrum._

/**
 * A monad that can suspend the execution of side effects
 * in the `F[_]` context.
 */
@typeclass
trait Sync[F[_]] extends MonadError[F, Throwable] {
  /**
   * Suspends the evaluation of an `F` reference.
   *
   * Equivalent to `FlatMap.flatten` for pure expressions,
   * the purpose of this function is to suspend side effects
   * in `F`.
   */
  def suspend[A](thunk: => F[A]): F[A]

  /**
   * Lifts any by-name parameter into the `F` context.
   *
   * Equivalent to `Applicative.pure` for pure expressions,
   * the purpose of this function is to suspend side effects
   * in `F`.
   */
  def delay[A](thunk: => A): F[A] = suspend(pure(thunk))
}

private[effect] trait SyncInstances {

  implicit object catsSyncForEval extends Sync[Eval] {
    def pure[A](a: A): Eval[A] = Now(a)

    // TODO replace with delegation when we upgrade to cats 1.0
    def handleErrorWith[A](fa: Eval[A])(f: Throwable => Eval[A]): Eval[A] =
      suspend(try Now(fa.value) catch { case internals.NonFatal(t) => f(t) })

    // TODO replace with delegation when we upgrade to cats 1.0
    def raiseError[A](e: Throwable): Eval[A] = suspend(throw e)

    def flatMap[A, B](fa: Eval[A])(f: A => Eval[B]): Eval[B] = fa.flatMap(f)

    def tailRecM[A, B](a: A)(f: A => Eval[Either[A,B]]): Eval[B] = {
      f(a) flatMap {
        case Left(nextA) => tailRecM(nextA)(f)
        case Right(b) => pure(b)
      }
    }

    def suspend[A](thunk: => Eval[A]): Eval[A] = Eval.defer(thunk)

    override def delay[A](thunk: => A): Eval[A] = Eval.always(thunk)
  }
}

object Sync extends SyncInstances
