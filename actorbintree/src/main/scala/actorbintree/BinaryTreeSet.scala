/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import actorbintree.BinaryTreeNode.{CopyFinished, CopyTo}
import akka.actor._
import akka.event.Logging

import scala.collection.immutable.Queue
import scala.util.Random

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {
  import BinaryTreeSet._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional (used to stash incoming operations during garbage collection)
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case operation: Operation => root ! operation
    case GC => {
      val newRoot = createRoot
      root ! CopyTo(newRoot)
      context become garbageCollecting(newRoot)
    }
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case operation: Operation => pendingQueue :+=  operation
    case CopyFinished =>
      root ! PoisonPill
      root = newRoot
      pendingQueue foreach( root ! _)
      pendingQueue = Queue.empty[Operation]
      context become normal
    case GC =>
  }

}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  /**
   * Acknowledges that a copy has been completed. This message should be sent
   * from a node to its parent, when this node and all its children nodes have
   * finished being copied.
   */
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode],  elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal
  val log = Logging(context.system, this)
  // optional

  private def getPosition(em: Int) = if (em > elem) Right else Left

  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case Insert(requester, id , em) =>

      if (elem == em) {
        removed = false
        requester ! OperationFinished(id)
      } else {
        val position = getPosition(em)
        subtrees get position match {
          case Some(actorRef) => actorRef ! Insert(requester, id , em)
          case None => {
            subtrees += position -> context.actorOf(props(em, false))
            requester ! OperationFinished(id)
          }
        }
      }
    case Contains (requester, id, em) =>
      if (elem == em) {
        requester ! ContainsResult(id, !removed)
      } else {
        val position = getPosition(em)
        subtrees get position match {
          case Some(actorRef) => actorRef ! Contains(requester, id, em)
          case None => requester ! ContainsResult(id, false)
        }
      }

    case Remove(requester, id, em) =>
      if (elem == em) {
        removed = true
        requester ! OperationFinished(id)
      } else {
        val position = getPosition(em)
        subtrees get position match {
          case Some(actorRef) => actorRef ! Remove(requester, id, em)
          case None => requester ! OperationFinished(id)
        }
      }

     case CopyTo(newRoot) => {
       if (removed && subtrees.isEmpty) {
         context.parent ! CopyFinished
         self ! PoisonPill
       } else {
         val expected = subtrees.values.toSet
         expected foreach  (_ ! CopyTo(newRoot))
         context become copying(expected, removed)
         if (!removed) newRoot ! Insert(self, Random.nextInt(), elem)
       }
     }
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(_) =>
      if (expected.isEmpty) context.parent ! CopyFinished
      else context become copying(expected, true)
    case CopyFinished =>
      val remaining = expected - sender
      if (remaining.isEmpty && insertConfirmed) context.parent ! CopyFinished
      else context become copying(remaining, insertConfirmed)
  }


}
