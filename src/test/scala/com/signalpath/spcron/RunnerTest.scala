package com.signalpath.spcron

import cats.effect.{ContextShift, IO, Timer}
import eu.timepit.fs2cron.ScheduledStreams
import eu.timepit.fs2cron.cron4s.Cron4sScheduler
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class RunnerTest extends AnyFunSpec with MockFactory with Matchers {

  describe("getLock") {
    describe("when the key does not exist") {
      it("should set a ttl on key and return true") {
        val mockLock = mock[LockingWorker]
        (mockLock.setNx _).expects("test", *).returning(IO(true)).once()
        (mockLock.expire _).expects("test", 42).returning(IO(true)).once()
        val cron = new Runner(mockLock)
        val actual = cron.getLock("test", 42).unsafeRunSync()
        actual should equal(true)
      }
    }

    describe("when the key already exists") {
      it("should return true when key is owned by runner") {
        val mockLock = mock[LockingWorker]
        val cron = new Runner(mockLock)
        (mockLock.setNx _).expects("test", *).returning(IO(false)).once()
        (mockLock.get _).expects("test").returning(IO(cron.runnerId)).once()
        (mockLock.eval _).expects(*, "test", 42).returning(IO(true)).once()
        val actual = cron.getLock("test", 42).unsafeRunSync()
        actual should equal(true)
      }

      it("should return false when key is owned by a different runner") {
        val mockLock = mock[LockingWorker]
        val cron = new Runner(mockLock)
        (mockLock.setNx _).expects("test", *).returning(IO(false)).once()
        (mockLock.get _).expects("test").returning(IO("nope")).once()
        (mockLock.eval _).expects(*, "test", 42).returning(IO(true)).once()
        val actual = cron.getLock("test", 42).unsafeRunSync()
        actual should equal(false)
      }
    }
  }

  describe("worker") {
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    describe("when it secures the lock") {
      it("should run the job") {
        var wasRun = false
        val j = IO({
          wasRun = true
          ()
        })
        val job = JobDefinition("* * * ? * *", "test", j, 1)
        val mockLock = mock[LockingWorker]
        val cron = new Runner(mockLock)
        (mockLock.setNx _).expects(job.name, *).returning(IO(true)).atLeastOnce()
        (mockLock.expire _).expects(job.name, job.ttl).returning(IO(true)).atLeastOnce()
        val runner = new Runner(mockLock)
        val worker = runner.worker(job.job, job.name, job.ttl)
        val schedStreams = new ScheduledStreams(Cron4sScheduler.systemDefault[IO])
        (schedStreams.awakeEvery(job.cron) >> worker).compile.drain.unsafeRunTimed(Duration(1, "second"))
        wasRun should equal(true)
      }
    }
    describe("when it does not secure the lock") {
      it("should not run the job") {
        var wasRun = false
        val j = IO({
          wasRun = true
          ()
        })
        val job = JobDefinition("* * * ? * *", "test", j, 1)
        val mockLock = mock[LockingWorker]
        val cron = new Runner(mockLock)
        (mockLock.setNx _).expects(job.name, *).returning(IO(false)).atLeastOnce()
        (mockLock.get _).expects(job.name).returning(IO("nope")).atLeastOnce()
        (mockLock.eval _).expects(*, job.name, job.ttl).returning(IO(true)).atLeastOnce()
        val runner = new Runner(mockLock)
        val worker = runner.worker(job.job, job.name, job.ttl)
        val schedStreams = new ScheduledStreams(Cron4sScheduler.systemDefault[IO])
        (schedStreams.awakeEvery(job.cron) >> worker).compile.drain.unsafeRunTimed(Duration(1, "second"))
        wasRun should equal(false)
      }
    }
  }

}
