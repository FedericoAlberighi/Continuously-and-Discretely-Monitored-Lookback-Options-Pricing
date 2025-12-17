package it.univr.montecarlo;


import it.univr.analyticprices.AnalyticPrices;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Main test class for the lookback option project.
 *
 * <p>The class:
 * <ul>
 *     <li>Defines market parameters (spot, risk-free rate, volatility).</li>
 *     <li>Builds instances of the four types of lookback options (call/put, fixed/floating strike),
 *         both continuously and discretely monitored.</li>
 *     <li>Performs Monte Carlo valuation under the Black–Scholes model.</li>
 *     <li>Computes the corresponding analytical prices via {@link it.univr.analyticprices.AnalyticPrices}.</li>
 *     <li>Repeats the valuation under the Bachelier model for comparison.</li>
 *     <li>Prints numerical results and supports the convergence analysis of discretely monitored options.</li>
 * </ul>
 */
public class Tests {

	/**
	 * Entry point of the numerical test suite for lookback option pricing.
	 *
	 * <p>The method performs the following steps:
	 * <ul>
	 *     <li>Defines market parameters (spot, risk-free rate, volatility).</li>
	 *     <li>Constructs continuously and discretely monitored lookback options (call/put, fixed/floating strike).</li>
	 *     <li>Runs Monte Carlo simulations under the Black–Scholes model.</li>
	 *     <li>Computes analytical benchmark prices using {@link it.univr.analyticprices.AnalyticPrices}.</li>
	 *     <li>Repeats the pricing under a Bachelier model for comparison.</li>
	 * </ul>
	 *
	 */
	public static void main(String[] args) throws CalculationException {
		// ========================= MARKET PARAMETERS =========================
		// Model parameters
		// These values define the market environment for the Black–Scholes model
		double spotPrice = 100.0;
		double riskFreeRate = 0.1;
		double volatility = 0.3;

		// ========================= OPTION PARAMETERS =========================
		// Option parameters
		double maturity = 1.0;
		double strike = 100.0;
		// Discretely monitored lookbacks will use this number of monitoring dates
		int numberOfFixingsForDiscretelyMonitoredLookbacks = 100; // if = 1000 gives the same result of continuously monitored


		// ==================== TIME DISCRETIZATION ====================
		// Time discretization parameters
		// The time discretization defines the grid on which the process is simulated
		int numberOfTimeSteps = 1000;
		double initialTime = 0.0;
		double timeStep = maturity / numberOfTimeSteps;

		TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		// ==================== CONTINUOUSLY MONITORED LOOKBACKS ====================
		// Here we construct products that approximate continuously monitored lookbacks (using the full time grid)
		AbstractAssetMonteCarloProduct continuouslyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike);
		AbstractAssetMonteCarloProduct continuouslyMonitoredPutFixedStrike = new LookbackPutFixedStrike(maturity, strike);
		AbstractAssetMonteCarloProduct continuouslyMonitoredCallFloatingStrike = new LookbackCallFloatingStrike(maturity);
		AbstractAssetMonteCarloProduct continuouslyMonitoredPutFloatingStrike = new LookbackPutFloatingStrike(maturity);

		// ===================== DISCRETELY MONITORED LOOKBACKS =====================
		// Here we construct products that represent discretely monitored lookbacks (finite number of monitoring dates)
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredPutFixedStrike = new LookbackPutFixedStrike(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFloatingStrike = new LookbackCallFloatingStrike(maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredPutFloatingStrike = new LookbackPutFloatingStrike(maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);

		// ===================== MONTE CARLO SIMULATION =====================
		// Monte Carlo simulation parameters
		int numberOfPaths = 10000;
		int seed = 1897;

		// Initialization of the Brownian motion and the Black–Scholes model
		// Construction of the Monte Carlo simulation
		BrownianMotion ourDriver = new BrownianMotionFromMersenneRandomNumbers(times, 1, numberOfPaths, seed);
		MonteCarloBlackScholesModel blackScholesProcess = new MonteCarloBlackScholesModel(spotPrice, riskFreeRate, volatility, ourDriver);

		// ---- Monte Carlo (Black–Scholes) for continuously monitored lookbacks ----
		double mCPriceContinuouslyMonitoredCallFixed = continuouslyMonitoredCallFixedStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredPutFixed = continuouslyMonitoredPutFixedStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredCallFloating = continuouslyMonitoredCallFloatingStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredPutFloating = continuouslyMonitoredPutFloatingStrike.getValue(blackScholesProcess);

		// ---- Monte Carlo (Black–Scholes) for discretely monitored lookbacks ----
		double mCPriceDiscretelyMonitoredCallFixed = discretelyMonitoredCallFixedStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredPutFixed = discretelyMonitoredPutFixedStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredCallFloating = discretelyMonitoredCallFloatingStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredPutFloating = discretelyMonitoredPutFloatingStrike.getValue(blackScholesProcess);


		// ---- Analytical prices (continuous monitoring) ----
		double analyticPriceContinuouslyMonitoredCallFloating = AnalyticPrices.continuouslyMonitoredLookbackCallFloatingStrike(spotPrice, riskFreeRate, volatility, maturity);
		double analyticPriceContinuouslyMonitoredPutFloating = AnalyticPrices.continuouslyMonitoredLookbackPutFloatingStrike(spotPrice, riskFreeRate, volatility, maturity);
		double analyticPriceContinuouslyMonitoredCallFixed = AnalyticPrices.continuouslyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);
		double analyticPriceContinuouslyMonitoredPutFixed = AnalyticPrices.continuouslyMonitoredLookbackPutFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);

		// ---- Analytical prices (Broadie approximation for discrete monitoring) ----
		double approximatedBroadiePriceDiscretelyMonitoredCallFloating = AnalyticPrices.discretelyMonitoredLookbackCallFloatingStrike(spotPrice, riskFreeRate, volatility, maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredPutFloating = AnalyticPrices.discretelyMonitoredLookbackPutFloatingStrike(spotPrice, riskFreeRate, volatility, maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredCallFixed = AnalyticPrices.discretelyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredPutFixed = AnalyticPrices.discretelyMonitoredLookbackPutFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);


		// --- Comparison: call, fixed strike ---
		System.out.println("MC price of continuously monitored call with fixed strike: " + mCPriceContinuouslyMonitoredCallFixed);
		System.out.println("Analytic price of continuously monitored call with fixed strike: " + analyticPriceContinuouslyMonitoredCallFixed);
		System.out.println("MC price of discretely monitored call with fixed strike: " + mCPriceDiscretelyMonitoredCallFixed);
		System.out.println("Approximated Broadie price of discretely monitored call with fixed strike: " + approximatedBroadiePriceDiscretelyMonitoredCallFixed);

		System.out.println();

		// --- Comparison: put, fixed strike ---
		System.out.println("MC price of continuously monitored put with fixed strike: " + mCPriceContinuouslyMonitoredPutFixed);
		System.out.println("Analytic price of continuously monitored put with fixed strike: " + analyticPriceContinuouslyMonitoredPutFixed);
		System.out.println("MC price of discretely monitored put with fixed strike: " + mCPriceDiscretelyMonitoredPutFixed);
		System.out.println("Approximated Broadie price of discretely monitored put with fixed strike: " + approximatedBroadiePriceDiscretelyMonitoredPutFixed);

		System.out.println();

		// --- Comparison: call, floating strike ---
		System.out.println("MC price of continuously monitored call with floating strike: " + mCPriceContinuouslyMonitoredCallFloating);
		System.out.println("Analytic price of continuously monitored call with floating strike: " + analyticPriceContinuouslyMonitoredCallFloating);
		System.out.println("MC price of discretely monitored call with floating strike: " + mCPriceDiscretelyMonitoredCallFloating);
		System.out.println("Approximated Broadie price of discretely monitored call with floating strike: " + approximatedBroadiePriceDiscretelyMonitoredCallFloating);

		System.out.println();

		// --- Comparison: put, floating strike ---
		System.out.println("MC price of continuously monitored put with floating strike: " + mCPriceContinuouslyMonitoredPutFloating);
		System.out.println("Analytic price of continuously monitored put with floating strike: " + analyticPriceContinuouslyMonitoredPutFloating);
		System.out.println("MC price of discretely monitored put with floating strike: " + mCPriceDiscretelyMonitoredPutFloating);
		System.out.println("Approximated Broadie price of discretely monitored put with floating strike: " + approximatedBroadiePriceDiscretelyMonitoredPutFloating);

		System.out.println();




		// ===================== BACHELIER MODEL =====================
		ProcessModel ourBachelierModel = new BachelierModel(spotPrice, riskFreeRate, spotPrice*volatility);
		MonteCarloAssetModel ourBachelierModelSimulation = new MonteCarloAssetModel(ourBachelierModel, ourDriver);


		// ---- Monte Carlo results under the Bachelier model ----
		double mCPriceContinuoslyMonitoredCallFixedForBachelierModelSimulation = continuouslyMonitoredCallFixedStrike.getValue(ourBachelierModelSimulation);
		double mCPriceContinuoslyMonitoredPutFixedForBachelierModelSimulation = continuouslyMonitoredPutFixedStrike.getValue(ourBachelierModelSimulation);
		double mCPriceContinuoslyMonitoredCallFloatingForBachelierModelSimulation = continuouslyMonitoredCallFloatingStrike.getValue(ourBachelierModelSimulation);
		double mCPriceContinuoslyMonitoredPutFloatingForBachelierModelSimulation = continuouslyMonitoredPutFloatingStrike.getValue(ourBachelierModelSimulation);
		
		
		double mCPriceDiscretelyMonitoredCallFixedForBachelierModelSimulation = discretelyMonitoredCallFixedStrike.getValue(ourBachelierModelSimulation);
		double mCPriceDiscretelyMonitoredPutFixedForBachelierModelSimulation = discretelyMonitoredPutFixedStrike.getValue(ourBachelierModelSimulation);
		double mCPriceDiscretelyMonitoredCallFloatingForBachelierModelSimulation = discretelyMonitoredCallFloatingStrike.getValue(ourBachelierModelSimulation);
		double mCPriceDiscretelyMonitoredPutFloatingForBachelierModelSimulation = discretelyMonitoredPutFloatingStrike.getValue(ourBachelierModelSimulation);
		
		System.out.println();
		
		System.out.println("MC price of continuously monitored call with fixed strike of Bachelier Model: " + mCPriceContinuoslyMonitoredCallFixedForBachelierModelSimulation);
		System.out.println("MC price of discritely monitored call with fixed strike of Bachelier Model: " + mCPriceDiscretelyMonitoredCallFixedForBachelierModelSimulation);
		
		System.out.println();
		
		System.out.println("MC price of continuously monitored put with fixed strike of Bachelier Model: " + mCPriceContinuoslyMonitoredPutFixedForBachelierModelSimulation);
		System.out.println("MC price of discritely monitored put with fixed strike of Bachelier Model: " + mCPriceDiscretelyMonitoredPutFixedForBachelierModelSimulation);
		
		System.out.println();
		
		System.out.println("MC price of continuously monitored call with floating strike of Bachelier Model: " + mCPriceContinuoslyMonitoredCallFloatingForBachelierModelSimulation);
		System.out.println("MC price of discritely monitored call with floating strike of Bachelier Model: " + mCPriceDiscretelyMonitoredCallFloatingForBachelierModelSimulation);
		
		System.out.println();
		
		System.out.println("MC price of continuously monitored put with floating strike of Bachelier Model: " + mCPriceContinuoslyMonitoredPutFloatingForBachelierModelSimulation);
		System.out.println("MC price of discritely monitored put with floating strike of Bachelier Model: " + mCPriceDiscretelyMonitoredPutFloatingForBachelierModelSimulation);
		
	}
}