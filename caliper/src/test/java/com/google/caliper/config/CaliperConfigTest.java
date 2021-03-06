/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertEquals;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Trial;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Tests {@link CaliperConfig}.
 *
 * @author gak@google.com (Gregory Kick)
 */
@RunWith(JUnit4.class)
public class CaliperConfigTest {
  @Test public void getDefaultVmConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(
        ImmutableMap.of("vm.args", "-very -special=args"));
    VmConfig defaultVmConfig = configuration.getDefaultVmConfig();
    assertEquals(new File(System.getProperty("java.home")), defaultVmConfig.javaHome());
    ImmutableList<String> expectedArgs = new ImmutableList.Builder<String>()
        .addAll(ManagementFactory.getRuntimeMXBean().getInputArguments())
        .add("-very")
        .add("-special=args")
        .build();
    assertEquals(expectedArgs, defaultVmConfig.options());
  }

  @Test public void getVmConfig_baseDirectoryAndName() throws Exception {
    File tempBaseDir = Files.createTempDir();
    File jdkHome = new File(tempBaseDir, "test");
    jdkHome.mkdir();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.baseDirectory", tempBaseDir.getAbsolutePath()));
    assertEquals(new VmConfig.Builder(jdkHome).build(),
        configuration.getVmConfig("test"));
    deleteRecursively(tempBaseDir);
  }

  @Test public void getVmConfig_baseDirectoryAndHome() throws Exception {
    File tempBaseDir = Files.createTempDir();
    File jdkHome = new File(tempBaseDir, "test-home");
    jdkHome.mkdir();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.baseDirectory", tempBaseDir.getAbsolutePath(),
        "vm.test.home", "test-home"));
    assertEquals(new VmConfig.Builder(jdkHome).build(),
        configuration.getVmConfig("test"));
    deleteRecursively(tempBaseDir);
  }

  @Test public void getVmConfig() throws Exception {
    File jdkHome = Files.createTempDir();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.args", "-a -b   -c",
        "vm.test.home", jdkHome.getAbsolutePath(),
        "vm.test.args", " -d     -e     "));
    assertEquals(
        new VmConfig.Builder(jdkHome)
            .addOption("-a")
            .addOption("-b")
            .addOption("-c")
            .addOption("-d")
            .addOption("-e")
            .build(),
        configuration.getVmConfig("test"));
    deleteRecursively(jdkHome);
  }

  @Test public void getInstrumentConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "instrument.test.class", "test.ClassName",
        "instrument.test.options.a", "1",
        "instrument.test.options.b", "excited b b excited"));
    assertEquals(
        new InstrumentConfig.Builder()
            .className("test.ClassName")
            .addOption("a", "1")
            .addOption("b", "excited b b excited")
            .build(),
        configuration.getInstrumentConfig("test"));
  }

  @Test public void getConfiguredResultProcessors() throws Exception {
    assertEquals(ImmutableSet.of(),
        new CaliperConfig(ImmutableMap.<String, String>of()).getConfiguredResultProcessors());
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "results.test.class", TestResultProcessor.class.getName()));
    assertEquals(ImmutableSet.of(TestResultProcessor.class),
        configuration.getConfiguredResultProcessors());
  }

  @Test public void getResultProcessorConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "results.test.class", TestResultProcessor.class.getName(),
        "results.test.options.g", "ak",
        "results.test.options.c", "aliper"));
    assertEquals(
        new ResultProcessorConfig.Builder()
            .className(TestResultProcessor.class.getName())
            .addOption("g", "ak")
            .addOption("c", "aliper")
            .build(),
        configuration.getResultProcessorConfig(TestResultProcessor.class));
  }

  private static final class TestResultProcessor implements ResultProcessor {
    @Override public void close() {}

    @Override public void processTrial(Trial trial) {}
  }

  /*
   * The following two methods are unsafe in the general case, but OK since we know that we're not
   * dealing with race conditions or symlinks.
   */

  private static void deleteDirectoryContents(File directory)
      throws IOException {
    checkArgument(directory.isDirectory(), "Not a directory: %s", directory);
    // Symbolic links will have different canonical and absolute paths
    if (!directory.getCanonicalPath().equals(directory.getAbsolutePath())) {
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      throw new IOException("Error listing files for " + directory);
    }
    for (File file : files) {
      deleteRecursively(file);
    }
  }

  private static void deleteRecursively(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectoryContents(file);
    }
    if (!file.delete()) {
      throw new IOException("Failed to delete " + file);
    }
  }
}
