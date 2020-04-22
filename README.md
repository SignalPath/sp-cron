# SP-Cron

A small library to define functions that run on a cron schedule
using [fs2-cron](https://github.com/fthomas/fs2-cron). Using 
[redis](redis.io) to create a lock per job preventing the job
from running on multiple instances at the same time.

## Usage

Running cron jobs is as simple as creating a Runner and passing
it JobDefinitions. The following example defines a single job
which prints the time every minute. The example assumes that
`redis` defines a connection to [redis](redis.io). See the
LockingWorker section for implementing a LockingWorker.    

```                                 
val lock = new RedisLockingWorker(redis)
val runner = new Runner(lock)
val jobs = Seq(
    JobDefinition(
        "0 * * ? * * ",
        "test2",
        IO({ println(LocalDateTime.now()) }),
        45
    )
)
runner.start(jobs)
```                                                     

Likely, a job will have side effects involving futures. Because
the JobDescription expects an `IO[Unit]` the future will either
need to be resolved, through an Await for example, or by being
converted to an IO. A future can be converted to an IO with
Catbird with, `rerunnableToIO(Rerunnable.fromFuture(F))`, where
F is the Twitter Future. 

### LockingWorker

The LockingWorker trait must be implemented and provided to the
Runner. LockingWorker expects implementations of 4 redis functions:

* setNx: to set a key only if it doesn't exist. A UUID is 
generated for each instantiation of the runner which is set
as the key value.
* expire: to expire the key based on the job's ttl
* get: to get the value of the key
* eval: which is used by threads that do not secure the log
to verify a ttl is set on the key. It does this by calling
ttl on the key and calling expire if no ttl is set.

An example implementation using 
[Finatra](https://twitter.github.io/finatra/) and 
[Catbird](https://github.com/travisbrown/catbird) is provided 
below.

```
class RedisLockingWorker(redis: RedisClient) extends LockingWorker {
    val keyPrefix = "cron:namespace:"

    override def setNx(key: String, value: String): IO[Boolean] =
      redis.setNx(keyPrefix + key, value).map(b => Boolean.unbox(b))

    override def expire(key: String, ttl: Long): IO[Boolean] =
      redis.expire(keyPrefix + key, ttl).map(b => Boolean.unbox(b))

    override def get(key: String): IO[String] =
      redis.get(keyPrefix + key).map(buf => optBufToStr(buf))

    override def eval(script: String, key: String, ttl: Long): IO[Boolean] =
      redis.eval(script, Seq(keyPrefix + key), Seq(ttl.toString)).map(_ => true)

    def optBufToStr(b: Option[Buf]): String = Utf8.unapply(b.get).get
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit def futureToIO[A](f: Future[A]): IO[A] = rerunnableToIO(Rerunnable.fromFuture(f))
    implicit def strToBuf(s: String): Buf = Utf8.apply(s)
    implicit def seqStrToSeqBuf(strings: Seq[String]): Seq[Buf] = strings.map(strToBuf)
}
```