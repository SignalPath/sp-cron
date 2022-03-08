package com.signalpath.spcron

import cats.effect.implicits._
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import cron4s.Cron
import eu.timepit.fs2cron.cron4s.Cron4sScheduler
import fs2.{Stream => fs2Stream}

/**
 * The definition of a job to be run on a schedule.
 *
 * @param cronExpression A cron4s compatible cron expression.
 * @param name A unique name for the job. This will be part of the lock name.
 * @param job The work to be done.
 * @param ttl How long the lock should be held for the job execution.
 */
case class JobDefinition(
  cronExpression: String,
  name: String,
  job: IO[Unit],
  ttl: Long) {

  def cron: cron4s.CronExpr = Cron.unsafeParse(cronExpression)
}

/**
 * The job runner. Create this and call the start method to start jobs executing.
 *
 * @param lock An implemented LockingWorker.
 */
class Runner(lock: LockingWorker) {

  val runnerId: String = java.util.UUID.randomUUID().toString

  /**
   * Starts a collection of jobs which are executed on their cron schedules.
   *
   * @param jobs Jobs to be executed.
   */
  def start(jobs: Seq[JobDefinition]): Unit = {
    val cronScheduler = Cron4sScheduler.systemDefault[IO]
    val streams = jobs.map { job =>
      cronScheduler.awakeEvery(job.cron) >> worker(job.job, job.name, job.ttl)
    }
    streams
      .map(j => j.compile.drain.unsafeRunAsync((cb: Either[Throwable, Unit]) => ()))
    ()
  }

  def worker(task: IO[Unit], name: String, ttl: Long): fs2.Stream[IO, fs2.INothing] = {
    val async: IO[Unit] = Async[IO].async { cb =>
      getLock(name, ttl).flatMap { isOwner =>
        if(isOwner) task
        else IO(())
      }.unsafeRunSync()
    }
    fs2Stream.eval_(IO(async.unsafeRunAsync{
      case Left(error) =>
        throw error
        ()
      case Right(unit) => unit
    }))
  }

  def getLock(name: String, ttl: Long): IO[Boolean] = {
    lock.setNx(name, runnerId).flatMap { setnxResult =>
      if (setnxResult) {
        lock.expire(name, ttl).map(_ => true)
      } else {
        lock.get(name).flatMap { owner =>
          val ownerCheck = runnerId.equalsIgnoreCase(owner)
          lock.eval(
            "local ttl = redis.call('ttl', KEYS[1]) if ttl <= 0 then redis.call('expire', KEYS[1], ARGV[1]) end return ttl",
            name,
            ttl).map(_ => ownerCheck)
        }
      }
    }
  }
}
