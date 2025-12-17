package it.univr.montecarlo;


import it.univr.analyticprices.AnalyticPrices;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.stochastic.RandomVariable;

/**
 * Test class for the application of control variates to the pricing of lookback options.
 *
 * <p>The class compares:
 * <ul>
 *     <li>Standard Monte Carlo pricing of a discretely monitored lookback call with fixed strike.</li>
 *     <li>Monte Carlo pricing with a Black–Scholes based control variate
 *         ({@link LookbackCallFixedWithBSControlVariate}).</li>
 *     <li>Analytical prices obtained from {@link it.univr.analyticprices.AnalyticPrices}.</li>
 * </ul>
 *
 * It also reports the empirical mean and standard deviation of the Monte Carlo estimator
 * with and without control variate, together with the variance reduction ratio.
 */
public class ControlVarieteTest {

	/**
	 * Runs a numerical experiment to assess the effectiveness of a control variate
	 * for a discretely monitored lookback call with fixed strike.
	 *
	 * <p>The method:
	 * <ul>
	 *     <li>Defines a Black–Scholes model and a time discretization.</li>
	 *     <li>Constructs a continuously and a discretely monitored lookback call (fixed strike).</li>
	 *     <li>Constructs a corresponding control variate product.</li>
	 *     <li>Prices the contracts via Monte Carlo with and without control variate.</li>
	 *     <li>Prints analytical prices, empirical means, standard deviations and variance reduction.</li>
	 * </ul>
	 *
	 * @param args Command-line arguments (not used).
	 * @throws CalculationException If an error occurs during the Monte Carlo valuation.
	 */
	public static void main(String[] args) throws CalculationException {
		// ========================= MARKET PARAMETERS =========================
		// Spot, risk-free rate and volatility define the Black–Scholes market environment
		double spotPrice = 100.0;
		double riskFreeRate = 0.1;
		double volatility = 0.3;

		// ========================= OPTION PARAMETERS =========================
		// Maturity, strike and number of fixings for the discretely monitored lookback
		double maturity = 0.5;
		double strike = 100.0;
		int numberOfFixingsForDiscretelyMonitoredLookbacks = 1000;

		// ==================== TIME DISCRETIZATION PARAMETERS ====================
		int numberOfTimeSteps = 10000;
		double initialTime = 0.0;
		double timeStep = maturity / numberOfTimeSteps;

		TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);
		// Continuous lookback (approximated on the full time grid) and discretely monitored lookback
		AbstractAssetMonteCarloProduct continuouslyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike);
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);

		// ===================== MONTE CARLO SIMULATION PARAMETERS =====================
		// Brownian motion driver and Black–Scholes Monte Carlo model
		int numberOfPaths = 20000;
		int seed = 1897;
		BrownianMotion ourDriver = new BrownianMotionFromMersenneRandomNumbers(times, 1, numberOfPaths, seed);

		MonteCarloBlackScholesModel blackScholesProcess = new MonteCarloBlackScholesModel(spotPrice, riskFreeRate, volatility, ourDriver);

		// Lookback call with Black–Scholes-based control variate
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFixedWithCV = new
				LookbackCallFixedWithBSControlVariate(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);

		// Price of the discretely monitored lookback call using the control variate estimator
		double mcPriceDiscretelyMonitoredCallFixedWithCV = discretelyMonitoredCallFixedWithCV.getValue(blackScholesProcess);
		System.out.println("Control Variates: " + mcPriceDiscretelyMonitoredCallFixedWithCV);

		// Standard Monte Carlo prices (continuous vs discrete monitoring)
		double mCPriceContinuouslyMonitoredCallFixed = continuouslyMonitoredCallFixedStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredCallFixed = discretelyMonitoredCallFixedStrike.getValue(blackScholesProcess);

		System.out.println("Continous " + mCPriceContinuouslyMonitoredCallFixed);
		System.out.println("Discrete " + mCPriceDiscretelyMonitoredCallFixed);
		// Analytical benchmark price for the discretely monitored lookback call (fixed strike)
		System.out.println("Analytic fromula: " + AnalyticPrices.discretelyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks));

		// Path-wise payoffs used to estimate mean and variance with and without control variate
		RandomVariable payoff = discretelyMonitoredCallFixedStrike.getValue(0.0, blackScholesProcess);
		System.out.println("Expected value of MC: " + payoff.getAverage());
		RandomVariable payoffCV = discretelyMonitoredCallFixedWithCV.getValue(0.0, blackScholesProcess);
		System.out.println("Expected value of CV: " + payoffCV.getAverage());

		// Comparison of dispersion (standard deviation) and variance reduction ratio
		System.out.println("Standard deviation of MC: " + payoff.getStandardDeviation());
		System.out.println("Standard deviation of CV: " + payoffCV.getStandardDeviation());
		System.out.println("Ratio: " + payoff.getStandardDeviation()/payoffCV.getStandardDeviation());

		
		
	}

}
