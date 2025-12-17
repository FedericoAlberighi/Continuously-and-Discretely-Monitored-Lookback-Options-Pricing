package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;


/**
 * Monte Carlo product implementing a fixed-strike lookback call option.
 *
 * <p>Payoff (continuously monitored version):
 * {@code max(M_T - K, 0)}, where {@code M_T = max_{t in [0,T]} S_t}.
 *
 * <p>For the discretely monitored version, the maximum is computed only on a finite set of
 * monitoring dates (a subset of the model time grid), controlled by {@code discretelyTimes}.
 *
 * <p>This class relies on the helper methods provided by {@link AbstractLookbackOption} to:
 * <ul>
 *   <li>build the monitoring times ({@link #buildMonitoringTimes(int, net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel)}),</li>
 *   <li>compute the pathwise running maximum ({@link #getMax(double[], net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel, int)}).</li>
 * </ul>
 */
public class LookbackCallFixedStrike extends AbstractLookbackOption {

	// Option maturity T
	private double maturity;
	// Fixed strike K
	private double strike;
	// Underlying index (useful for multi-asset models)
	private int underlyingIndex;
	// Number of monitoring dates for discrete monitoring (0 = use full time grid)
	private int discretelyTimes;


	/**
	 * Creates a discretely monitored fixed-strike lookback call.
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param strike          Fixed strike {@code K}.
	 * @param discretelyTimes Number of monitoring dates used to compute the running maximum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackCallFixedStrike(double maturity, double strike, int discretelyTimes) {
		this.maturity=maturity;
		this.strike=strike;
		this.underlyingIndex=0;
		this.discretelyTimes=discretelyTimes;
	}

	/**
	 * Creates a fixed-strike lookback call using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param maturity Option maturity {@code T}.
	 * @param strike   Fixed strike {@code K}.
	 */
	public LookbackCallFixedStrike(double maturity, double strike) {
		this.maturity=maturity;
		this.strike=strike;
		this.underlyingIndex=0;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a fixed-strike lookback call on a specific underlying index, using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param strike          Fixed strike {@code K}.
	 */
	public LookbackCallFixedStrike(double maturity, int underlyingIndex, double strike) {
		this.maturity=maturity;
		this.strike=strike;
		this.underlyingIndex=underlyingIndex;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a discretely monitored fixed-strike lookback call on a specific underlying index.
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param strike          Fixed strike {@code K}.
	 * @param discretelyTimes Number of monitoring dates used to compute the running maximum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackCallFixedStrike(double maturity, int underlyingIndex, double strike, int discretelyTimes) {
		this.maturity=maturity;
		this.strike=strike;
		this.underlyingIndex=underlyingIndex;
		this.discretelyTimes=discretelyTimes;
	}




	/**
	 * Evaluates the discounted value of the product at a given evaluation time.
	 *
	 * <p>The method:
	 * <ol>
	 *   <li>builds the monitoring time grid (full grid if {@code discretelyTimes == 0});</li>
	 *   <li>computes the pathwise running maximum of the underlying over the monitoring grid;</li>
	 *   <li>computes the payoff {@code max(maxUnderlying - K, 0)} at maturity;</li>
	 *   <li>discounts the payoff from maturity to {@code evaluationTime} using numeraire and Monte Carlo weights.</li>
	 * </ol>
	 *
	 * @param evaluationTime Time {@code t} at which the value is returned.
	 * @param model          Monte Carlo simulation model providing the underlying paths and numeraires.
	 * @return A {@link RandomVariable} containing the discounted payoff value path-by-path at {@code evaluationTime}.
	 * @throws CalculationException If the model cannot provide asset values/numeraire/weights for required times.
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException {
		double[] discretizedTimes = buildMonitoringTimes(discretelyTimes, model);

		// Pathwise running maximum M_T over the monitoring grid
		RandomVariable maxValue = getMax(discretizedTimes, model, underlyingIndex);
		// Payoff at maturity: max(M_T - K, 0)
		RandomVariable values = maxValue.sub(strike).floor(0.0);

		// Discount payoff from maturity to evaluationTime using numeraire and Monte Carlo weights
		final RandomVariable numeraireAtMaturity = model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights = model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable numeraireAtEvalTime = model.getNumeraire(evaluationTime);
		final RandomVariable monteCarloWeightsAtEvalTime = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);
	
		return values;
	}
	
}
