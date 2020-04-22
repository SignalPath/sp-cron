package com.signalpath.spcron

import cats.effect.IO

trait LockingWorker {

  def setNx(key: String, value: String): IO[Boolean]
  def expire(key: String, ttl: Long): IO[Boolean]
  def get(key: String): IO[String]
  def eval(script: String, key: String, ttl: Long): IO[Boolean]

}
