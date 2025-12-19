package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
//import net.finmath.functions.*;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import it.univr.analyticprices.AnalyticPrices;

/**
 * Monte Carlo product implementing a fixed-strike lookback call option priced with a control variate.
 *
 * <p>This product prices a <em>discretely monitored</em> fixed-strike lookback call via Monte Carlo and
 * applies a control variate technique to reduce the estimator variance.
 *
 * <h3>Notation</h3>
 * <ul>
 *   <li>{@code Z}: Monte Carlo estimator of the discretely monitored lookback call (target payoff).</li>
 *   <li>{@code Y}: control variate (here: the continuously monitored lookback call evaluated on the same paths).</li>
 *   <li>{@code muY}: analytical expectation {@code E[Y]} under Black–Scholes (closed-form lookback price).</li>
 * </ul>
 *
 * <h3>Control variate estimator</h3>
 * The returned estimator is
 * {@code Z_cv = Z - c (Y - muY)}, where {@code c = Cov(Z,Y)/Var(Y)} is estimated empirically from the same
 * Monte Carlo sample.
 *
 * <p>If the provided simulation model is not a Black–Scholes model, the class falls back to the standard
 * Monte Carlo estimator {@code Z} (no variance reduction).
 */
public class LookbackCallFixedWithBSControlVariate extends AbstractLookbackOption {

	// Option maturity T
	private double maturity;
	// Fixed strike K
	private double strike;
	// Number of monitoring dates used for the discrete lookback payoff
	private int discretelyTimes;
	// Underlying index (useful for multi-asset models)
	private Integer underlyingIndex;

	/**
	 * Creates a control-variate lookback call (fixed strike) on the first underlying (index 0).
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param strike          Fixed strike {@code K}.
	 * @param discretelyTimes Number of monitoring dates used for the discretely monitored payoff.
	 */
	public LookbackCallFixedWithBSControlVariate(double maturity, double strike, int discretelyTimes) {
		this.maturity=maturity;
		this.strike=strike;
		this.discretelyTimes=discretelyTimes;
		this.underlyingIndex=0;
	}

	/**
	 * Creates a control-variate lookback call (fixed strike) on a given underlying index.
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param underlyingIndex 
	 * @param strike          Fixed strike {@code K}.
	 * @param discretelyTimes Number of monitoring dates used for the discretely monitored payoff.
	 */
	public LookbackCallFixedWithBSControlVariate(double maturity, int underlyingIndex, double strike, int discretelyTimes) {
		this.maturity=maturity;
		this.underlyingIndex=underlyingIndex;
		this.strike=strike;
		this.discretelyTimes=discretelyTimes;
	}

	/**
	 * Computes the analytical Black–Scholes price used as {@code muY = E[Y]} in the control variate.
	 *
	 * <p>In this implementation {@code Y} is chosen as the continuously monitored fixed-strike lookback call.
	 * Hence {@code muY} is given by the corresponding closed-form formula implemented in
	 * {@link it.univr.analyticprices.AnalyticPrices}.
	 *
	 * @param evaluationTime Evaluation time (the method computes the price at time 0).
	 * @param model          Monte Carlo simulation model (used to read the spot from the simulated asset).
	 * @param processModel   Black–Scholes process model (used to read r and sigma).
	 * @return The analytical price of the continuously monitored fixed-strike lookback call.
	
	 */
	public double computeAnalyticValue(double evaluationTime, AssetModelMonteCarloSimulationModel model,
			BlackScholesModel processModel) throws CalculationException {

		// Read spot S0 from the simulation model
		double spotPrice = model.getAssetValue(0, underlyingIndex).doubleValue();
		// Read r and sigma from the Black–Scholes model
		double riskFreeRate = processModel.getRiskFreeRate().doubleValue();
		double volatility = processModel.getVolatility().doubleValue();
		// Closed-form price of the continuously monitored lookback call (fixed strike)
		return AnalyticPrices.continuouslyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);
	}

	/**
	 * Returns the discounted value of the control-variate estimator at the given evaluation time.
	 *
	 * <p>The method:
	 * <ol>
	 *   <li>Prices the discretely monitored lookback call via standard Monte Carlo (target estimator {@code Z}).</li>
	 *   <li>Checks whether the underlying model is Black–Scholes; if not, returns {@code Z}.</li>
	 *   <li>Builds the control variate {@code Y} as the continuously monitored lookback call on the same paths.</li>
	 *   <li>Computes {@code muY} using the closed-form Black–Scholes formula.</li>
	 *   <li>Estimates the optimal coefficient {@code c} and returns {@code Z_cv = Z - c (Y - muY)}.</li>
	 * </ol>
	 *
	 * @param evaluationTime Time {@code t} at which the value is returned.
	 * @param model          Monte Carlo simulation model providing paths, numeraires and weights.
	 * @return A {@link RandomVariable} representing the control-variate estimator path-by-path.
	
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Target product Z: discretely monitored fixed-strike lookback call
		LookbackCallFixedStrike stdLoockBackCallFixedStrike = new LookbackCallFixedStrike(maturity, underlyingIndex, strike, discretelyTimes);

		// Standard Monte Carlo estimator for the target payoff
		RandomVariable Z = stdLoockBackCallFixedStrike.getValue(0.0, model);

		// If the model is not Black–Scholes, return Z (no control variate available)
		if(!(((MonteCarloAssetModel) model).getModel() instanceof BlackScholesModel)) {
			System.out.println("The model is not Black-Scholes: we are going to perform the standard Monte Carlo");
			return Z;
		}

		// Retrieve the Black–Scholes process model
		BlackScholesModel processModel = (BlackScholesModel) ((MonteCarloAssetModel) model).getModel();
		// muY = E[Y]: analytical expectation of the control variate under Black–Scholes
		double blackSholesPrice = computeAnalyticValue(0.0, model, processModel);
		// Convert muY to a constant RandomVariable
		RandomVariable muY = model.getRandomVariableForConstant(blackSholesPrice);

		// Control variate Y: continuously monitored fixed-strike lookback call
		LookbackCallFixedStrike countinousLoockBackCallFixedStrike = new LookbackCallFixedStrike(maturity, underlyingIndex, strike);
		RandomVariable Y = countinousLoockBackCallFixedStrike.getValue(0.0, model);

		// Estimate optimal coefficient c = Cov(Z,Y)/Var(Y) from the sample
		double covariance = Z.covariance(Y).doubleValue();
		double variance = Y.variance().doubleValue();
		double optimalC = covariance/variance;

		// c (Y - muY)
		RandomVariable termToSubtract = Y.sub(muY).mult(optimalC);

		// Control variate estimator: Z_cv = Z - c (Y - muY)
		RandomVariable Zc = Z.sub(termToSubtract);
		
		return Zc;
	}
	
	

}
