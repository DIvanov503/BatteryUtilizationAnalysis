package org.sosy_lab.cpachecker.cpa.battery;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

@Options(prefix = "battery")
public class PragmaPowerConsumptionGetter implements PowerConsumptionGetter {

  private Map<CFAEdge, PowerConsumption> powerConsumptionPerEdge = new HashMap<>();

  private static Pattern pragmaPattern = Pattern.compile("\\s*#pragma\\s*power\\s*([it])\\s*"
      + "([-+]?\\d*\\.?\\d*(?:[eE][-+]?\\d+)?)\\s*([it])\\s*"
      + "([-+]?\\d*\\.?\\d*(?:[eE][-+]?\\d+)?)\\s*");

  @Option(secure=true, required=false, name = "defaultDuration", description="default t")
  double defaultDuration = 0.0;

  @Option(secure=true, required=false, name = "defaultIntensity", description="default i")
  double defaultIntensity = 0.0;

  public PragmaPowerConsumptionGetter(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public static PragmaPowerConsumptionGetter create(Configuration pConfig)
      throws InvalidConfigurationException {
    return new PragmaPowerConsumptionGetter(pConfig);
  }

  @Override
  public PowerConsumption getPowerConsumption(CFAEdge edge) {
    PowerConsumption powerConsumption;
    powerConsumption = powerConsumptionPerEdge.get(edge);
    if (powerConsumption != null) {
      return powerConsumption;
    }
    int lineNumber = edge.getLineNumber();
    if (lineNumber > 1 && edge.getRawStatement().length() > 0) {
      String line = null;
      try (BufferedReader br = Files
          .newBufferedReader(Paths.get(edge.getFileLocation().getFileName()),
              Charset.defaultCharset())) {
        for (int j = 0; j < lineNumber; ++j) {
          line = br.readLine();
        }
      } catch (IOException pE) {
        throw new IOError(pE);
      }
      Matcher matcher = pragmaPattern.matcher(line);
      if (matcher.matches()) {
        double i, t;
        if (matcher.group(1).equals("i") && matcher.group(3).equals("t")) {
          i = Double.parseDouble(matcher.group(2));
          t = Double.parseDouble(matcher.group(4));
        } else if (matcher.group(1).equals("t") && matcher.group(3).equals("i")) {
          i = Double.parseDouble(matcher.group(4));
          t = Double.parseDouble(matcher.group(2));
        } else {
          throw new AssertionError("pragma at " + edge.getFileLocation().getFileName() + ":"
              + lineNumber + " does not specify the duration and intensity in the format: "
              + "#pragma power [i|t] <value> [t|i] <value>");
        }
        powerConsumption = new PowerConsumption(t, i);
        powerConsumptionPerEdge.put(edge, powerConsumption);
        return powerConsumption;
      }
    }
    powerConsumption = new PowerConsumption(defaultDuration, defaultIntensity);
    powerConsumptionPerEdge.put(edge, powerConsumption);
    return powerConsumption;
  }

}
