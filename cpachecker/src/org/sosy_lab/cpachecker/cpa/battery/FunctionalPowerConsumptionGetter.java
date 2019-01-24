package org.sosy_lab.cpachecker.cpa.battery;

import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.StatementEdge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

@Options(prefix = "battery")
public class FunctionalPowerConsumptionGetter implements PowerConsumptionGetter {

  private Map<CFAEdge, PowerConsumption> powerConsumptionPerEdge = new HashMap<>();

  @Option(secure=true, required=false, name = "defaultDuration", description="default t")
  double defaultDuration = 0.0;

  @Option(secure=true, required=false, name = "defaultIntensity", description="default i")
  double defaultIntensity = 0.0;

  @Option(secure=true, required=false, name = "powerConsumptionName", description="name of the function designating power consumption")
  String powerConsumptionName = "__VERIFIER_power";

  PowerExtractor powerExtractor = new PowerExtractor();

  FunctionalPowerConsumptionGetter(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public static FunctionalPowerConsumptionGetter create(Configuration pConfig)
      throws InvalidConfigurationException {
    return new FunctionalPowerConsumptionGetter(pConfig);
  }

  @Override
  public PowerConsumption getPowerConsumption(CFAEdge edge) {
    PowerConsumption powerConsumption;
    powerConsumption = powerConsumptionPerEdge.get(edge);
    if (powerConsumption != null) {
      return powerConsumption;
    }
    if(edge.getEdgeType() == StatementEdge) {
      CStatement statement = ((CStatementEdge)edge).getStatement();
      powerConsumption = statement.accept(powerExtractor);
    } else {
      powerConsumption = new PowerConsumption(defaultDuration, defaultIntensity);
    }
    powerConsumptionPerEdge.put(edge, powerConsumption);
    return powerConsumption;
  }

  public class PowerExtractor implements CStatementVisitor<PowerConsumption, NoException>,
                                         CExpressionVisitor<Double, NoException> {
    @Override
    public PowerConsumption visit(CExpressionStatement pIastExpressionStatement) {
      return new PowerConsumption(defaultDuration, defaultIntensity);
    }

    @Override
    public PowerConsumption visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {
      return new PowerConsumption(defaultDuration, defaultIntensity);
    }

    @Override
    public PowerConsumption visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {
      pIastFunctionCallAssignmentStatement.toASTString();
      return new PowerConsumption(defaultDuration, defaultIntensity);
    }

    @Override
    public PowerConsumption visit(CFunctionCallStatement pIastFunctionCallStatement) {
      CFunctionCallExpression functionalCallExpression = pIastFunctionCallStatement.getFunctionCallExpression();
      AExpression functionNameExpression = functionalCallExpression.getFunctionNameExpression();
      if (functionNameExpression instanceof AIdExpression && ((AIdExpression)functionNameExpression).getName().equals(powerConsumptionName)) {
        List<CExpression> parameterExpressions = functionalCallExpression.getParameterExpressions();
        if (parameterExpressions.size() != 2) {
          return new PowerConsumption(defaultDuration, defaultIntensity);
        }
        CExpression intencityExpression = parameterExpressions.get(0), durationExpression = parameterExpressions.get(1);
        return new PowerConsumption(intencityExpression.accept(this), durationExpression.accept(this));
      }
      return new PowerConsumption(defaultDuration, defaultIntensity);
    }

    @Override
    public Double visit(CBinaryExpression pIastBinaryExpression) {
      switch (pIastBinaryExpression.getOperator()) {
        case MULTIPLY:
          return pIastBinaryExpression.getOperand1().accept(this) * pIastBinaryExpression.getOperand2().accept(this);
        case DIVIDE:
          return pIastBinaryExpression.getOperand1().accept(this) / pIastBinaryExpression.getOperand2().accept(this);
        case MODULO:
          return pIastBinaryExpression.getOperand1().accept(this) % pIastBinaryExpression.getOperand2().accept(this);
        case PLUS:
          return pIastBinaryExpression.getOperand1().accept(this) + pIastBinaryExpression.getOperand2().accept(this);
        case MINUS:
          return pIastBinaryExpression.getOperand1().accept(this) - pIastBinaryExpression.getOperand2().accept(this);
        default:
          return 0.0;
      }
    }

    @Override
    public Double visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      return pIastFloatLiteralExpression.getValue().doubleValue();
    }

    @Override
    public Double visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      return pIastIntegerLiteralExpression.getValue().doubleValue();
    }

    @Override
    public Double visit(CUnaryExpression pIastUnaryExpression) {
      switch (pIastUnaryExpression.getOperator()) {
        case MINUS:
          return -pIastUnaryExpression.getOperand().accept(this);
        default:
          return 0.0;
      }
    }

    @Override
    public Double visit(CCastExpression pIastCastExpression) { return 0.0; }

    @Override
    public Double visit(CCharLiteralExpression pIastCharLiteralExpression){ return 0.0; }

    @Override
    public Double visit(CStringLiteralExpression pIastStringLiteralExpression) { return 0.0; }

    @Override
    public Double visit(CTypeIdExpression pIastTypeIdExpression) { return 0.0; }

    @Override
    public Double visit (CImaginaryLiteralExpression PIastLiteralExpression) { return 0.0; }

    @Override
    public Double visit(CAddressOfLabelExpression pAddressOfLabelExpression) { return 0.0; }

    @Override
    public Double visit(CArraySubscriptExpression pIastArraySubscriptExpression) { return 0.0; }

    @Override
    public Double visit(CFieldReference pIastFieldReference){ return 0.0; }

    @Override
    public Double visit(CIdExpression pIastIdExpression) { return 0.0; }

    @Override
    public Double visit(CPointerExpression pointerExpression) { return 0.0; }

    @Override
    public Double visit(CComplexCastExpression complexCastExpression) { return 0.0; }
  }

}
