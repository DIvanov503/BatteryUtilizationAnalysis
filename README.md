# Battery Utilization Analysis
Battery utilization analysis implemented in [CPAchecker](https://cpachecker.sosy-lab.org/)

## Installation
For installing Battery Utilization Analysis, copy the contents from `cpachecker/` to the [CPAchecker](https://github.com/sosy-lab/cpachecker/) project and follow the instructions for CPAchecker. Running `ant` should suffy for compiling the project.

## Configuration
Configuration is done with a configuration file like `cpachecker/config/batteryARG.properties`. Battery Utilization Analysis has the following options that can be specified in the configuation file:

 - `battery.c`: parameter `c` of KiBaM, must be a real value between 0 and 1
 - `battery.k`: parameter `k` of KiBaM, must be a positive real value
 - `battery.PathUpdate`: the mode of updating the path, can be set to `ForgetfulPathUpdate` and `NonforgetfulPathUpdate`
 - `battery.PowerConsumptionGetter`: the way the analysis gets current intensity and duration, can be set to `FunctionalPowerConsumptionGetter`, `RandomPowerConsumptionGetter`, or `PragmaPowerConsumptionGetter`
 - `FunctionalPowerConsumptionGetter` takes the values from the procedure call to `extern void __VERIFIER_power(double i, double t)`, which has to be declared in the input source file. The name of the procedure can be changed with the parameter `powerConsumptionName` (must be a string literal). See `cpachecker/test/programs/battery/example.c` for an example of use.
 - `RandomPowerConsumptionGetter` assigns to each CFA egde a positive normally distributed random variable with zero mean and unary variance. Boolean parameter `battery.useSeed` and long parameter `battery.seed` allow to set a specific seed for the random number generator.
 - `PragmaPowerConsumptionGetter` allow to specify the power consumption with pragmas of the syntax `#pragma power [i|t] <VALUE> [t|i] <VALUE>`. Such pragmas define power consumption of the statements in the next line.
 - Both `FunctionalPowerConsumptionGetter` and `PragmaPowerConsumptionGetter` take optional parameters `battery.defaultIntensity` `battery.defaultDuration.`

## Running
Once CPAchecker is compiled together with Battery Utilization Analysis, one can run the analysis on a single file, e.g., with

`./scripts/cpa.sh -config ./config/batteryARG.properties ./test/programs/battery/example.c`

or on a benchmark suite, e.g., with

`./scripts/benchmark.py --container ./test/test-sets/integration-battery.xml`

See CPAchecker documentation for more information on the output.

## Authors
[Dmitry Ivanov](mailto:d.ivanov@tuhh.de), [Sibylle Schupp](mailto:schupp@tuhh.de)
