package org.sosy_lab.cpachecker.cpa.battery;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.battery")
public class BatteryCPA extends AbstractCPA implements ProofCheckerCPA {

  private LogManager logger;

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN", "IGNORECALLSTACK"},
      description="which merge operator to use for BatteryCPA")
  private String mergeType = "JOIN";

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "JOIN", "IGNORECALLSTACK"},
      description="which stop operator to use for BatteryCPA")
  private String stopType = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BatteryCPA.class);
  }

  private BatteryCPA(LogManager logger, Configuration config, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException, CPAException {
    super(
        DelegateAbstractDomain.getInstance(),
        new BatteryTransferRelation(logger, config, shutdownNotifier));
    config.inject(this);
    this.logger = logger;
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());
      case "IGNORECALLSTACK":
        return new MergeIgnoringCallstack();
      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    switch (stopType) {
      case "SEP":
        return new StopSepOperator(getAbstractDomain());
      case "JOIN":
        return new StopJoinOperator(getAbstractDomain());
      case "IGNORECALLSTACK":
        return new StopIgnoringCallstack();
      default:
        throw new AssertionError("unknown stop operator");
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    logger.log(Level.FINE, "Start extracting all declared variables in program.",
        "Distinguish between local and global variables.");
    return new BatteryState();
  }
}
