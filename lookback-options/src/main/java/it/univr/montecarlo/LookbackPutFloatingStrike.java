package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
//import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
//import net.finmath.time.TimeDiscretization;

/**
 * Monte Carlo product implementing a floating-strike lookback put option.
 *
 * <p>Payoff (continuously monitored version):
 * {@code max(M_T - S_T, 0)}, where {@code M_T = max_{t in [0,T]} S_t} and {@code S_T} is the terminal value.
 *
 * <p>For the discretely monitored version, the running maximum {@code M_T} is computed only on a finite set of
 * monitoring dates (a subset of the model time grid), controlled by {@code discretelyTimes}.
 *
 * <p>This class relies on helper methods provided by {@link AbstractLookbackOption} to:
 * <ul>
 *   <li>build the monitoring time grid ({@link #buildMonitoringTimes(int, AssetModelMonteCarloSimulationModel)}),</li>
 *   <li>compute the pathwise running maximum ({@link #getMax(double[], AssetModelMonteCarloSimulationModel, int)}).</li>
 * </ul>
 */
public class LookbackPutFloatingStrike extends AbstractLookbackOption {

	// Option maturity T
	private double maturity;
	// Underlying index (useful for multi-asset models)
	private int underlyingIndex;
	// Number of monitoring dates for discrete monitoring (0 = use full time grid)
	private int discretelyTimes;

	/**
	 * Creates a discretely monitored floating-strike lookback put on the first underlying (index 0).
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param discretelyTimes Number of monitoring dates used to compute the running maximum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackPutFloatingStrike(double maturity, int discretelyTimes) {
		this.maturity=maturity;
		this.underlyingIndex=0;
		this.discretelyTimes=discretelyTimes;
	}

	/**
	 * Creates a floating-strike lookback put on the first underlying (index 0) using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param maturity Option maturity {@code T}.
	 */
	public LookbackPutFloatingStrike(double maturity) {
		this.maturity=maturity;
		this.underlyingIndex=0;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a floating-strike lookback put on a specific underlying index using the full model time grid
	 * (i.e., an approximation of continuous monitoring).
	 *
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param maturity        Option maturity {@code T}.
	 */
	public LookbackPutFloatingStrike(int underlyingIndex, double maturity) {
		this.maturity=maturity;
		this.underlyingIndex=underlyingIndex;
		this.discretelyTimes=0;
	}

	/**
	 * Creates a discretely monitored floating-strike lookback put on a specific underlying index.
	 *
	 * @param maturity        Option maturity {@code T}.
	 * @param underlyingIndex Index of the underlying to be used in the simulation model.
	 * @param discretelyTimes Number of monitoring dates used to compute the running maximum.
	 *                        If {@code 0}, the full model time grid is used (continuous-monitoring approximation).
	 */
	public LookbackPutFloatingStrike(double maturity, int underlyingIndex, int discretelyTimes) {
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
	 *   <li>computes the pathwise running maximum of the underlying over the monitoring grid;</li>
	 *   <li>retrieves the terminal underlying value {@code S_T};</li>
	 *   <li>computes the payoff {@code max(M_T - S_T, 0)} at maturity;</li>
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
		// Underlying value at maturity S_T
		RandomVariable finalValue = model.getAssetValue(model.getTimeIndex(maturity), underlyingIndex);
		// Payoff at maturity: max(M_T - S_T, 0)
		RandomVariable values = maxValue.sub(finalValue).floor(0.0);

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

