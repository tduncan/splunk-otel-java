package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SdkShutdownHookTest {
  private final SdkShutdownHook shutdownHook = new SdkShutdownHook();

  @Test
  void shutdownClosesAddedCloseable() {
    var thingToClose = new SuccessfulCloseable();
    shutdownHook.add(thingToClose);
    shutdownHook.shutdown();

    assertThat(thingToClose.closed).isTrue();
  }

  @Test
  void shutdownClosesMultipleAddedCloseable() {
    var one = new SuccessfulCloseable();
    var two = new SuccessfulCloseable();

    shutdownHook.add(one);
    shutdownHook.add(two);
    shutdownHook.shutdown();

    assertThat(one.closed).isTrue();
    assertThat(two.closed).isTrue();
  }

  @Test
  void shutdownReportsSuccessAllCloseablesCloseSuccessfully() {
    shutdownHook.add(new SuccessfulCloseable());
    shutdownHook.add(new SuccessfulCloseable());

    var result = shutdownHook.shutdown();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shutdownReportsFailureWhenCloseableFailsToClose() {
    shutdownHook.add(new ExceptionThrowingCloseable());

    var result = shutdownHook.shutdown();
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  void shutdownReportsFailureWhenAtLeaseCloseableFailsToClose() {
    shutdownHook.add(new SuccessfulCloseable());
    shutdownHook.add(new ExceptionThrowingCloseable());
    shutdownHook.add(new SuccessfulCloseable());

    var result = shutdownHook.shutdown();
    assertThat(result.isSuccess()).isFalse();
  }

  private static class SuccessfulCloseable implements Closeable {
    private boolean closed;

    @Override
    public void close() {
      closed = true;
    }
  }

  private static class ExceptionThrowingCloseable implements Closeable {
    @Override
    public void close() throws IOException {
      throw new IOException();
    }
  }
}
