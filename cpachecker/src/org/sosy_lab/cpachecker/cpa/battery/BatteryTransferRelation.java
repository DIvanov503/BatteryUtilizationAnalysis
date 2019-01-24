package org.sosy_lab.cpachecker.cpa.battery;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidComponentException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;

@Options(prefix = "battery")
public class BatteryTransferRelation implements TransferRelation {

  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;

  private static final String CLASS_PREFIX = "org.sosy_lab.cpachecker.cpa.battery";

  @Option(secure=true, required=true, name = "c", description="c parameter of KiBaM")
  private double c;

  @Option(secure=true, required=true, name = "k", description="k parameter of KiBaM")
  private double k;

  @Option(secure=true, required=false, name="PathUpdate",
      description="path update mode to use")
  private String pathUpdateName = ForgetfulPathUpdate.class.getCanonicalName();

  private PathUpdate pathUpdate;

  @Option(secure=true, required=false, name="PowerConsumptionGetter",
      description="power consumption getter to use")
  private String powerConsumptionGetterName = PragmaPowerConsumptionGetter.class.getCanonicalName();

  private PowerConsumptionGetter powerConsumptionGetter;

  private BatteryTransferRelation(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
  }

  public BatteryTransferRelation(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException {
    this(pLogger, pShutdownNotifier);
    pConfig.inject(this);
    Class<?> optionClass;
    try {
      optionClass = Classes.forName(pathUpdateName, CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Option battery.PathUpdate is set to unknown path update mode " + pathUpdateName, e);
    }
    Object obj;
    obj = newInterfaceInstance(PathUpdate.class, optionClass, pConfig);
    if ((obj == null) || !(obj instanceof PathUpdate)) {
      throw new InvalidComponentException(optionClass, "PathUpdate",
            "Create method did not return a PathUpdate instance.");
    }
    pathUpdate = (PathUpdate)obj;

    try {
      optionClass = Classes.forName(powerConsumptionGetterName, CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Option battery.PowerConsumptionGetter is set to unknown power consumption getter " + powerConsumptionGetterName, e);
    }
    obj = newInterfaceInstance(PowerConsumptionGetter.class, optionClass, pConfig);
    if ((obj == null) || !(obj instanceof PowerConsumptionGetter)) {
      throw new InvalidComponentException(optionClass, "PowerConsumptionGetter",
          "Create method did not return a PowerConsumptionGetter instance.");
    }
    powerConsumptionGetter = (PowerConsumptionGetter)obj;
  }

  private Object newInterfaceInstance(Class<?> targetInterface, Class<?> targetClass, Configuration pConfig)
      throws CPAException {

    Method createMethod;
    try {
      createMethod = targetClass.getMethod("create", Configuration.class);

    } catch(NoSuchMethodException e) {
      throw new InvalidComponentException(targetClass, targetInterface.getSimpleName(), "No public static method \"create\" with one parameter Configuration.");
    }
    if (!Modifier.isStatic(createMethod.getModifiers())) {
      throw new InvalidComponentException(targetClass, targetInterface.getSimpleName(), "Create method is not static.");
    }

    String exception = Classes.verifyDeclaredExceptions(createMethod, CPAException.class, InvalidConfigurationException.class);
    if (exception != null) {
      throw new InvalidComponentException(targetClass, targetInterface.getSimpleName(), "Create method declares the unsupported checked exception " + exception + " .");
    }

    Object targetObj;
    try {
      targetObj = createMethod.invoke(null, pConfig);

    } catch (IllegalAccessException e) {
      throw new InvalidComponentException(targetClass, targetInterface.getSimpleName(), "Create method is not public.");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class);

      throw new UnexpectedCheckedException("instantiation of PathUpdate in BatteryCPA", cause);
    }

    return targetObj;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    List<CFANode> nodes = ReachingDefUtils.getAllNodesFromCFA();
    if (nodes == null) {
      throw new CPATransferException("CPA not properly initialized.");
    }
    List<AbstractState> successors = new ArrayList<>();
    for (CFANode node : nodes) {
      for (CFAEdge cfaedge : CFAUtils.leavingEdges(node)) {
        shutdownNotifier.shutdownIfNecessary();
        successors.addAll(getAbstractSuccessors(pState, cfaedge));
      }
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);
    return getAbstractSuccessors(pState, pCfaEdge);
  }

  private Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge) throws CPATransferException {

    logger.log(Level.FINE, "Compute successor for ", pState, "along edge", pCfaEdge);

    if (!(pState instanceof BatteryState)) { throw new CPATransferException(
        "Unexpected type of abstract state. The transfer relation is not defined for this type"); }

    if (pCfaEdge == null) { throw new CPATransferException(
        "Expected an edge along which the successors should be computed"); }

    if (pState == BatteryState.topElement) {
      return Collections.singleton(pState);
    }

    BatteryState result;

    result = handleEdge((BatteryState) pState, pCfaEdge);
    return Collections.singleton(result);
  }

  private BatteryState handleEdge(BatteryState pState, CFAEdge pCfaEdge) {
    List<CFAEdge> path = pState.path;
    ListIterator<CFAEdge> i = path.listIterator(path.size());
    double transfer = computeTransfer(pState, pCfaEdge);
    BatteryState newState;
    while(i.hasPrevious()) {
      if (i.previous().equals(pCfaEdge)) {
        double limit = computeLimit(path, i.nextIndex());
        path = pathUpdate.update(path, i, pCfaEdge);
        newState = new BatteryState(transfer, path);
        newState = newState.join(new BatteryState(limit, path));
        return newState.join(pState);
      }
    }
    path = new ArrayList<CFAEdge>(path);
    path.add(pCfaEdge);
    newState = new BatteryState(transfer, path);
    return newState.join(pState);
  }

  private double computeLimit(List<CFAEdge> path,
                              int index) {
    boolean nonZeroDuration = false;
    double e_sum, e_t, result;
    PowerConsumption pc = powerConsumptionGetter.getPowerConsumption(path.get(index));
    nonZeroDuration |= pc.duration > 0;
    e_sum = Math.exp(-k * pc.duration);
    result = pc.intensity * (1 - e_sum);
    for (int j = path.size() - 1; j > index; --j) {
      pc = powerConsumptionGetter.getPowerConsumption(path.get(j));
      nonZeroDuration |= pc.duration > 0;
      e_t = Math.exp(-k * pc.duration);
      result += pc.intensity * (1 - e_t) * e_sum;
      e_sum *= e_t;
    }
    if (nonZeroDuration) {
      return result / (c * k * (1 - e_sum));
    }
    else {
      return 0;
    }
  }

  private double computeTransfer(BatteryState pState, CFAEdge edge) {
    PowerConsumption pc = powerConsumptionGetter.getPowerConsumption(edge);
    double e_kt = Math.exp(-k * pc.duration);
    return pState.deltaH * e_kt + pc.intensity * (1 - e_kt) / (c * k);
  }

  @Override
  public @Nullable Collection<? extends AbstractState> strengthen(
      AbstractState state, List<AbstractState> otherStates, CFAEdge cfaEdge, Precision precision)
      throws CPATransferException, InterruptedException {
    return Collections.singleton(state);
  }


}
