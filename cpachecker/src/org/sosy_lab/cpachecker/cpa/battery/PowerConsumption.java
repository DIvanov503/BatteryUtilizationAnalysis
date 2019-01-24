package org.sosy_lab.cpachecker.cpa.battery;

import com.google.common.base.Objects;

public class PowerConsumption {
  public double duration;
  public double intensity;

  public PowerConsumption(double duration, double intensity){
    this.duration = duration;
    this.intensity = intensity;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PowerConsumption other = (PowerConsumption) obj;
    return (duration == other.duration) && (intensity == other.intensity);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(duration, intensity);
  }
}
