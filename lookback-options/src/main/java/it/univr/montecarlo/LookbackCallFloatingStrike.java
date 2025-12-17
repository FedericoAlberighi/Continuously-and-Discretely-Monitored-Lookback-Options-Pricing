package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
//import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
//import net.finmath.time.TimeDiscretization;

/**
 * Monte Carlo product implementing a floating-strike lookback call option.
 *
 * <p>Payoff (continuously monitored version):
 * {@code max(S_T - m_T, 0)}, where {@code m_T = min_{t in [0,T]} S_t}.
 *
 * <p>For the discretely monitored version, the running minimum {@code m_T} is computed only on a finite set of
 * monitoring dates (a subset of the model time grid), controlled by {@code discretelyTimes}.
 *
 * <p>This class relies on helper methods provided by {@link AbstractLookbackOption} to:
 * <ul>
 *   <li>build the monitoring time grid ({@link #buildMonitoringTimes(int, net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel)}),</li>
 *   <li>compute the pathwise running minimum ({@link #getMin(double[], net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel, int)}).</li>
 * </ul>
 */
public class LookbackCallFloatingStrike extends AbstractLookbackOption {
	// Option maturity T
	private double maturity;
	// Underlying index (useful for multi-asset models)
	private int underlyingIndex;
	// Number of monitoring dates for discrete monitoring (0 = use full time grid)
	private int discretelyTimes;

	/**
	 * Creates a discretely monitored floating-strike lookback call on the first underlying (index 0).
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param discretelyTimes Number of monitoring dates used to compute the running minimum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackCallFloatingStrike(double maturity, int discretelyTimes) {
		this.maturity=maturity;
		this.underlyingIndex=0;
		this.discretelyTimes=discretelyTimes;
	}

	/**
	 * Creates a floating-strike lookback call on the first underlying (index 0) using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param maturity Option maturity {@code T}.
	 */
	public LookbackCallFloatingStrike(double maturity) {
		this.maturity=maturity;
		this.underlyingIndex=0;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a floating-strike lookback call on a specific underlying index using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param maturity        Option maturity {@code T}.
	 */
	public LookbackCallFloatingStrike(int underlyingIndex, double maturity) {
		this.maturity=maturity;
		this.underlyingIndex=underlyingIndex;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a discretely monitored floating-strike lookback call on a specific underlying index.
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param discretelyTimes Number of monitoring dates used to compute the running minimum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackCallFloatingStrike(double maturity, int underlyingIndex, int discretelyTimes) {
		this.maturity=maturity;
		this.underlyingIndex=underlyingIndex;
		this.discretelyTimes=discretelyTimes;
	}

	/**
	 * Evaluates the discounted value of the product at a given evaluation time.
	 *
	 * <p>The method:
	 * <ol>
	 *   <li>builds the monitoring time grid (full grid if {@code discretelyTimes == 0});</li>
	 *   <li>computes the pathwise running minimum of the underlying over the monitoring grid;</li>
	 *   <li>computes the payoff {@code max(S_T - m_T, 0)} at maturity;</li>
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
		// Pathwise running minimum m_T over the monitoring grid
		RandomVariable minValue = getMin(discretizedTimes, model, underlyingIndex);
		// Underlying value at maturity S_T
		RandomVariable finalValue = model.getAssetValue(model.getTimeIndex(maturity), underlyingIndex);
		// Payoff at maturity: max(S_T - m_T, 0)
		RandomVariable values = finalValue.sub(minValue).floor(0.0);

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
