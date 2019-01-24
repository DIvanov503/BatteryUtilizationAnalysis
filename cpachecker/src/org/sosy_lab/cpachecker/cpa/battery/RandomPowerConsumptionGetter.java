package org.sosy_lab.cpachecker.cpa.battery;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

@Options(prefix = "battery")
public class RandomPowerConsumptionGetter implements PowerConsumptionGetter {

  private Map<CFAEdge, PowerConsumption> powerConsumptionPerEdge = new HashMap<>();

  private Random randomNumberGenerator;

  @Option(secure=true, required=false, name = "useSeed", description="whether or not to use the seed")
  private boolean useSeed = false;

  @Option(secure=true, required=false, name = "seed", description="randomness seed")
  private long seed = 1;

  RandomPowerConsumptionGetter(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (useSeed) {
      randomNumberGenerator = new Random(seed);
    } else {
      randomNumberGenerator = new Random();
    }
  }

  public static RandomPowerConsumptionGetter create(Configuration pConfig)
      throws InvalidConfigurationException {
    return new RandomPowerConsumptionGetter(pConfig);
  }

  @Override
  @SuppressWarnings("MathAbsoluteRandom")
  public PowerConsumption getPowerConsumption(CFAEdge edge) {
    PowerConsumption powerConsumption;
    powerConsumption = powerConsumptionPerEdge.get(edge);
    if (powerConsumption != null) {
      return powerConsumption;
    }
    double i, t;
    i = Math.abs(randomNumberGenerator.nextGaussian());
    t = Math.abs(randomNumberGenerator.nextGaussian());
    powerConsumption = new PowerConsumption(t, i);
    powerConsumptionPerEdge.put(edge, powerConsumption);
    return powerConsumption;
  }

}
