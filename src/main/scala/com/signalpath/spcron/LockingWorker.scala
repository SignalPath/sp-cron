package com.signalpath.spcron

import cats.effect.IO

/**
 * Defines the redis functions needed by this library. Abstracted to all implementation of your redis
 * library of choice.
 */
trait LockingWorker {

  /**
   * Redis setNx command.
   *
   * @param key Redis key name.
   * @param value Value to set in the key. Will be the runner id.
   * @return True if key was set.
   */
  def setNx(key: String, value: String): IO[Boolean]

  /**
   * Redis expire command.
   *
   * @param key Redis key name.
   * @param ttl Duration of time, in seconds, the key should exist for.
   * @return True if successfully set.
   */
  def expire(key: String, ttl: Long): IO[Boolean]

  /**
   * Redis get command.
   *
   * @param key Redis key name.
   * @return String value stored in the key.
   */
  def get(key: String): IO[String]

  /**
   * Redis eval command.
   *
   * @param script "String lua script to execute. Will execute a ttl and optionally an expire command.
   * @param key Redis key name.
   * @param ttl Duration of time, in seconds, the key should exist for.
   * @return True if successful.
   */
  def eval(script: String, key: String, ttl: Long): IO[Boolean]

}
