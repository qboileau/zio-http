package zhttp.logging

import zhttp.logging.LoggerTransport.Transport

import java.nio.file.{Files, Path, StandardOpenOption}
import java.util

/**
 * Provides a way to build and configure transports for logging. Transports are
 * used to, format and serialize LogLines and them to a backend.
 */
final case class LoggerTransport(
  name: String,
  level: LogLevel,
  format: Setup.LogFormat,
  filter: String => Boolean,
  transport: Transport,
) extends LogFrontend { self =>
  def withFormat(format: LogLine => CharSequence): LoggerTransport = self.copy(format = format)
  def withFormat(format: LogFormat): LoggerTransport               = self.copy(format = LogFormat.run(format))
  def withLevel(level: LogLevel): LoggerTransport                  = self.copy(level = level)
  def withFilter(filter: String => Boolean): LoggerTransport       = self.copy(filter = filter)
  def withName(name: String): LoggerTransport                      = self.copy(name = name)
  override def log(msg: CharSequence): Unit                        = if (filter(msg.toString)) transport.run(msg)
}

object LoggerTransport {
  def console(name: String): LoggerTransport = LoggerTransport(
    name = name,
    level = LogLevel.OFF,
    format = LogFormat.default,
    filter = _ => true,
    transport = Transport.UnsafeSync(println),
  )

  def file(name: String, filePath: Path): LoggerTransport = LoggerTransport(
    name = name,
    level = LogLevel.OFF,
    format = LogFormat.simple,
    filter = _ => true,
    transport = Transport.UnsafeFileSync(filePath),
  )

  trait Transport { self =>
    def run(line: CharSequence): Unit
  }

  object Transport {
    final case class UnsafeSync(log: CharSequence => Unit) extends Transport {
      override def run(line: CharSequence): Unit = log(line)

    }

    final case class UnsafeFileSync(path: Path) extends Transport {
      override def run(line: CharSequence): Unit = {
        Files.write(
          path,
          util.Arrays.asList(line),
          StandardOpenOption.APPEND,
          StandardOpenOption.CREATE,
        ): Unit
      }
    }

    case object Empty extends Transport {
      override def run(line: CharSequence): Unit = ()
    }
  }
}