package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Abstract base class for Monte Carlo pricing of lookback options.
 *
 * <p>This class provides common utilities shared by all concrete lookback products, such as:
 * <ul>
 *     <li>Construction of monitoring times for continuously and discretely monitored payoffs.</li>
 *     <li>Pathwise computation of the running maximum and minimum of the underlying.</li>
 * </ul>
 *
 * Concrete subclasses are expected to implement {@link #getValue(double, AssetModelMonteCarloSimulationModel)}
 * to define the specific payoff (call/put, fixed/floating strike, etc.).
 */
public abstract class AbstractLookbackOption extends AbstractAssetMonteCarloProduct {


	/**
	 * Builds the array of monitoring times used to evaluate the lookback payoff.
	 *
	 * <p>If {@code discretelyTimes} is zero, the method returns all times contained in the
	 * model's {@link TimeDiscretization}, thus approximating a continuously monitored payoff.
	 * If {@code discretelyTimes} is strictly positive, the method builds a coarser grid
	 * of monitoring times by selecting (approximately) equidistant indices from the underlying
	 * time discretization.
	 *
	 * @param discretelyTimes The number of monitoring dates to be used for a discretely monitored payoff.
	 *                        If this value is {@code 0}, the full time grid of the model is used.
	 * @param model           The Monte Carlo simulation model providing the underlying time discretization.
	 * @return An array of doubles representing the monitoring times (in increasing order).
	 */
	protected double[] buildMonitoringTimes(int discretelyTimes, AssetModelMonteCarloSimulationModel model) {
		// Extract the time discretization used by the Monte Carlo simulation
		TimeDiscretization td = model.getTimeDiscretization();
        // Case 1: use all available times (approximate continuous monitoring)
        if (discretelyTimes == 0) {
            // Copy the entire time grid from the model
            double[] times = new double[td.getNumberOfTimes()];
            for (int i = 0; i < td.getNumberOfTimes(); i++) {
                times[i] = td.getTime(i);
            }
            return times;
        }
        // Case 2: construct a coarser grid with a fixed number of monitoring dates
        double[] times = new double[discretelyTimes + 1];
        for (int i = 0; i <= discretelyTimes; i++) {
            // Map the logical monitoring index i to an index on the underlying time discretization
            int index = (int) Math.round(i * (td.getNumberOfTimes() - 1) / (double) discretelyTimes);
            times[i] = td.getTime(index);
            }
		return times;
        }


	/**
	 * Computes the pathwise running maximum of the underlying process over the given monitoring times.
	 *
	 * <p>The method iterates over all monitoring times, retrieves the underlying asset value for each time,
	 * and updates a running maximum stored in a {@link RandomVariable}. The returned random variable
	 * contains, for each Monte Carlo path, the maximum of the underlying over all specified times.
	 *
	 * @param discretizedTimes An array of monitoring times over which the maximum is computed.
	 * @param model            The Monte Carlo simulation model providing asset values.
	 * @param underlyingIndex  Index of the underlying (in case of a multi-asset model).
	 * @return A {@link RandomVariable} representing, path by path, the running maximum of the underlying.
	 * @throws CalculationException If the underlying values cannot be obtained from the model.
	 */
	protected RandomVariable getMax(double[] discretizedTimes, AssetModelMonteCarloSimulationModel model, int underlyingIndex) throws CalculationException {
		// Initialize running maximum to zero for all paths (assuming non-negative underlying trajectories)
		RandomVariable maxValue = model.getRandomVariableForConstant(0.0);
		// Iterate over all monitoring times and update the running maximum
		for(double currentTime : discretizedTimes) {
			RandomVariable underlying = model.getAssetValue(currentTime, underlyingIndex);
			maxValue = maxValue.floor(underlying);
		}
		return maxValue;
	}


	/**
	 * Computes the pathwise running minimum of the underlying process over the given monitoring times.
	 *
	 * <p>The method iterates over all monitoring times, retrieves the underlying asset value for each time,
	 * and updates a running minimum stored in a {@link RandomVariable}. The returned random variable
	 * contains, for each Monte Carlo path, the minimum of the underlying over all specified times.
	 *
	 * @param discretizedTimes An array of monitoring times over which the minimum is computed.
	 * @param model            The Monte Carlo simulation model providing asset values.
	 * @param underlyingIndex  Index of the underlying (in case of a multi-asset model).
	 * @return A {@link RandomVariable} representing, path by path, the running minimum of the underlying.
	 * @throws CalculationException If the underlying values cannot be obtained from the model.
	 */
	protected RandomVariable getMin(double[] discretizedTimes, AssetModelMonteCarloSimulationModel model, int underlyingIndex) throws CalculationException {
		RandomVariable minValue = model.getRandomVariableForConstant(Integer.MAX_VALUE); // Initialize running minimum to a very large value
		// Iterate over all monitoring times and update the running minimum
		for(double currentTime : discretizedTimes) {
			RandomVariable underlying = model.getAssetValue(currentTime, underlyingIndex);
			minValue = minValue.cap(underlying);		
		}
		return minValue;
	}

}
