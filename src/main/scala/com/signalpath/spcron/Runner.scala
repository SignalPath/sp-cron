package com.signalpath.spcron

import cats.effect.{Async, IO, Timer}
import cron4s.Cron
import eu.timepit.fs2cron.awakeEveryCron
import fs2.{Stream => fs2Stream}

import scala.concurrent.ExecutionContext

case class JobDefinition(
  cronExpression: String,
  name: String,
  job: IO[Unit],
  ttl: Long) {

  def cron: cron4s.CronExpr = Cron.unsafeParse(cronExpression)
}

class Runner(lock: LockingWorker) {

  val runnerId = java.util.UUID.randomUUID().toString

  def start(jobs: Seq[JobDefinition]): Unit = {
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    val streams = jobs.map { job =>
      awakeEveryCron[IO](job.cron) >> worker(job.job, job.name, job.ttl)
    }
    streams
      //.dropRight(1)
      .map(j => j.compile.drain.unsafeRunAsync((cb: Either[Throwable, Unit]) => Unit))
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
