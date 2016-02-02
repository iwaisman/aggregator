package example

// Created by idan on 9/12/15.

import akka.actor.{ Actor, ActorRef, PoisonPill }


import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Success, Try }

object AggregatorActor {
  type InitialTasks[T] = ExecutionContext ⇒ Seq[Future[T]]

  case class Begin[T](timeout: FiniteDuration, init: InitialTasks[T])
}
abstract class AggregatorActor[T, R] extends Actor {
  //logger.info("TICK: "+context.system.scheduler.asInstanceOf[LightArrayRevolverScheduler].TickDuration)

  import AggregatorActor._
  type Task = Future[T]

  private case class CompleteTask(arriving: Task, arrivingResult: Try[T])
  private case object Conclude

  import context.dispatcher

  private var tasks: Set[Task] = Set.empty
  private var aggregatedTasks: Set[Task] = Set.empty
  private var client: Option[ActorRef] = None
  private var resultComplete = false

  private def allTasksComplete = (tasks diff aggregatedTasks).isEmpty

  /** To be implemented by subclass. Is applied to every incoming result.
   *  @param arrivingResult  Incoming result of a task
   */
  protected def aggregate(arrivingResult: Try[T]): Unit = {}

  /** Implemented by subclass. Finalizes result.
   *  @return The ultimate result
   */
  protected def prepareResult: R

  protected def completeTasks: Seq[Try[T]] = tasks.toSeq.map(_.value).collect{ case Some(t) ⇒ t }
  protected def successfulTasks: Seq[T] = completeTasks.collect{ case Success(t) ⇒ t }

  /** Only use within aggregate!
   *  Registers a new task which will NOT be run if registration occurs after result is returned.
   *  @param taskF Function that returns a Task
   */
  protected def registerTerminator(taskF: () ⇒ Task): Unit =
    if (!resultComplete) registerContinuation(taskF())

  /** Only use within aggregate!
   *  Registers a new task which WILL be run even if registration occurs after result is returned.
   *  @param task A Task
   */
  protected def registerContinuation(task: Task): Unit = {
    tasks += task
    task onComplete (arriving ⇒ self ! CompleteTask(task, arriving))
  }

  private def possiblyConclude() = if (tasks forall (_.isCompleted)) conclude()
  private def conclude() = {
    if (!resultComplete) {
      resultComplete = true
      client.get ! prepareResult
    }
    if (allTasksComplete) self ! PoisonPill
  }

  private def beginTimeout(timeout: FiniteDuration): Unit =
    context.system.scheduler.scheduleOnce(timeout, self, Conclude)

  override def receive: Receive = {
    case CompleteTask(task, arriving) ⇒
      aggregate(arriving)
      aggregatedTasks += task
      possiblyConclude()

    case Begin(timeout, initialTasks: InitialTasks[T @unchecked]) ⇒
      if (client.isEmpty) {
        client = Some(sender())
        beginTimeout(timeout)
        initialTasks(context.dispatcher).foreach(registerContinuation)
      }
      else
        sender ! new IllegalStateException("Aggregator already started!")

    case Conclude ⇒ conclude()
  }
}

abstract class SimpleAggregatorActor[T] extends AggregatorActor[T, Seq[T]] {
  override protected def prepareResult: Seq[T] = successfulTasks
}