/**
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper.runner;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentSpec;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.caliper.util.Stdout;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Prints the results as they come in.
 * Prints all results to be processed by ReBench.
 * And, prints a summary.
 * Is based on ConsoleResultProcessor. 
 */
final class ReBenchConsoleResultProcessor implements ResultProcessor {
  private final PrintWriter stdout;

  private Set<InstrumentSpec> instrumentSpecs = Sets.newHashSet();
  private Set<VmSpec> vmSpecs = Sets.newHashSet();
  private Set<BenchmarkSpec> benchmarkSpecs = Sets.newHashSet();
  private int numMeasurements = 0;

  @Inject ReBenchConsoleResultProcessor(@Stdout PrintWriter stdout) {
    this.stdout = stdout;
  }

  @Override public void processTrial(Trial trial) {
    instrumentSpecs.add(trial.instrumentSpec());
    Scenario scenario = trial.scenario();
    vmSpecs.add(scenario.vmSpec());
    benchmarkSpecs.add(scenario.benchmarkSpec());
    numMeasurements += trial.measurements().size();
    
    reportMeasurements(scenario.benchmarkSpec(), trial.measurements());
  }
  
  public void reportMeasurements(BenchmarkSpec spec, ImmutableList<Measurement> measurements) {
	for (Measurement measurement : measurements) {
	  stdout.printf("Measurement (%s) for %s in %s: %f%s%n",
					measurement.description(),
					spec.methodName(),
					spec.className(),
					measurement.value().magnitude() / measurement.weight(),
					measurement.value().unit());
	}
  }

  @Override public void close() {
    stdout.printf("Collected %d measurements from:%n", numMeasurements);
    stdout.printf("  %d instrument(s)%n", instrumentSpecs.size());
    stdout.printf("  %d virtual machine(s)%n", vmSpecs.size());
    stdout.printf("  %d benchmark(s)%n", benchmarkSpecs.size());
    stdout.flush();
  }
}
