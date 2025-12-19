package it.univr.analyticprices;

import net.finmath.functions.NormalDistribution;

public class AnalyticPrices {
	
	
	private static double beta = 0.5826;

	/**
	 * It returns the analytic price of a floating-strike lookback call with continuous monitoring
	 * when the underlying S has Black–Scholes dynamics.
	 *
	 * It assumes that the contract starts at zero, i.e., the "running minimum" at the start of the contract is
	 * simply S_0.
	 * 
	 * It handles smoothly the limit r → 0.
	 */
	public static double continuouslyMonitoredLookbackCallFloatingStrike(
	        double spotPrice, double interestRate, double volatility, double maturity) {

	    if (maturity <= 0.0 || volatility <= 0.0) {
	        return 0.0;
	    }

	    double sqrtMaturity = Math.sqrt(maturity);
	    double discountFactor = Math.exp(-interestRate * maturity);
	    double a = 0.5 * volatility * sqrtMaturity;

	    // Limit case: r ≈ 0 
	    if (Math.abs(interestRate) < 1e-8) {
	        double phiA = Math.exp(-0.5 * a * a) / Math.sqrt(2.0 * Math.PI);
	        double PhiA = NormalDistribution.cumulativeDistribution(a);
	        double PhiMinusA = NormalDistribution.cumulativeDistribution(-a);

	        // Limit of the σ²/(2r) term as r → 0
	        double limitTerm = spotPrice * (
	                volatility * sqrtMaturity * phiA
	                - 0.5 * volatility * volatility * maturity * PhiMinusA
	        );

	        // Base term: S0 [N(a) − N(−a)]
	        double base = spotPrice * (PhiA - PhiMinusA);

	        return base + limitTerm;
	    }

	    //General case: r ≠ 0
	    double d = (interestRate + 0.5 * volatility * volatility)
	               * maturity / (volatility * sqrtMaturity);

	    double term1 = spotPrice * NormalDistribution.cumulativeDistribution(d);
	    double term2 = -discountFactor * spotPrice
	                   * NormalDistribution.cumulativeDistribution(d - volatility * sqrtMaturity);

	    double bracket = NormalDistribution.cumulativeDistribution(
	                            -d + 2.0 * interestRate * sqrtMaturity / volatility)
	                   - Math.exp(interestRate * maturity)
	                     * NormalDistribution.cumulativeDistribution(-d);

	    double term3 = discountFactor * (volatility * volatility) / (2.0 * interestRate)
	                   * spotPrice * bracket;

	    return term1 + term2 + term3;
	}

	/**
	 * It returns the analytic price of a floating-strike lookback put with continuous monitoring
	 * when the underlying S has Black–Scholes dynamics.
	 *
	 * It assumes that the contract starts at zero, i.e., the "running maximum" at the start of the contract is
	 * simply S_0.
	 * 
	 * It handles smoothly the limit r → 0.
	 */
	public static double continuouslyMonitoredLookbackPutFloatingStrike(
	        double spotPrice, double interestRate, double volatility, double maturity) {

	    if (maturity <= 0.0 || volatility <= 0.0) {
	        return 0.0;
	    }
	    double sqrtMaturity = Math.sqrt(maturity);
	    double discountFactor = Math.exp(-interestRate * maturity);

	    // Limit case: r ≈ 0 
	    if (Math.abs(interestRate) < 1e-8) {
	        double a = 0.5 * volatility * sqrtMaturity; // a = σ√T / 2
	        double phiA = Math.exp(-0.5 * a * a) / Math.sqrt(2.0 * Math.PI);
	        double PhiA = NormalDistribution.cumulativeDistribution(a);
	        double PhiMinusA = NormalDistribution.cumulativeDistribution(-a);

	        // Limit as r → 0 of the σ²/(2r) term (analytically derived)
	        double limitTerm = spotPrice * (
	                volatility * sqrtMaturity * phiA
	              + 0.5 * volatility * volatility * maturity * PhiA
	        );

	        // Base term: -S0 Φ(-a) + S0 Φ(a)
	        double base = spotPrice * (PhiA - PhiMinusA);

	        return base + limitTerm;
	    }

	    // General case: r ≠ 0 
	    double d = (interestRate + 0.5 * volatility * volatility)
	               * maturity / (volatility * sqrtMaturity);

	    double term1 = -spotPrice * NormalDistribution.cumulativeDistribution(-d);
	    double term2 =  discountFactor * spotPrice
	                   * NormalDistribution.cumulativeDistribution(-d + volatility * sqrtMaturity);

	    double bracket = Math.exp(interestRate * maturity)
	                   * NormalDistribution.cumulativeDistribution(d)
	                   - NormalDistribution.cumulativeDistribution(d - 2.0 * interestRate * sqrtMaturity / volatility);

	    double term3 = discountFactor * (volatility * volatility) / (2.0 * interestRate)
	                   * spotPrice * bracket;

	    return term1 + term2 + term3;
	}

	
	/**
	 * It returns the analytic price of a fixed-strike lookback call with continuous monitoring
	 * when the underlying S has Black–Scholes dynamics.
	 *
	 * It assumes that the contract starts at zero, i.e., the "running maximum" at the start of the contract is
	 * simply S_0.
	 * 
	 * It handles smoothly the limit r → 0.
	 */
	public static double continuouslyMonitoredLookbackCallFixedStrike(
			double spotPrice,double riskFreeRate, double volatility, double maturity, double strike) {
		/*
		 * We compute the price via the put-call parity for lookbacks (see for example the book 
		 * "Options, Futures, and Other Derivatives" by Hull)
		 */
		double sMaxStar = Math.max(spotPrice, strike);
		double valueFloatingPut = continuouslyMonitoredLookbackPutFloatingStrike(sMaxStar, riskFreeRate,  volatility, maturity);
		return valueFloatingPut + spotPrice - strike * Math.exp(-riskFreeRate*maturity);  
	}

	/**
	 * It returns the analytic price of a fixed-strike lookback put with continuous monitoring
	 * when the underlying S has Black–Scholes dynamics.
	 *
	 * It assumes that the contract starts at zero, i.e., the "running minimum" at the start of the contract is
	 * simply S_0.
	 * 
	 * It handles smoothly the limit r → 0.
	 */
	public static double continuouslyMonitoredLookbackPutFixedStrike(
			double spotPrice,double riskFreeRate, double volatility, double maturity, double strike) {
		/*
		 * We compute the price via the put-call parity for lookbacks (see for example the book 
		 * "Options, Futures, and Other Derivatives" by Hull)
		 */
		double sMaxStar = Math.min(spotPrice, strike);
		double valueFloatingPut = continuouslyMonitoredLookbackCallFloatingStrike(sMaxStar, riskFreeRate,  volatility, maturity);
		return valueFloatingPut + strike * Math.exp(-riskFreeRate*maturity) - spotPrice;
	}
	
	
	/**
	 * Approximate price of a discretely monitored floating-strike lookback call.
	 *
	 * <p>Payoff: {@code S(T) - min_i S(t_i)}, where the minimum is sampled only at
	 * {@code numberOfFixingDates} uniformly spaced monitoring dates.</p>
	 *
	 * <p>The price is obtained via the Broadie–Glasserman–Kou (1999) continuity
	 * correction, linking the discrete-monitoring case to the continuous
	 * analytic formula.</p>
	 *
	 * @param spotPrice Initial underlying value {@code S0}.
	 * @param riskFreeRate Constant risk-free rate {@code r}.
	 * @param volatility {@code σ}.
	 * @param maturity Time to maturity {@code T}.
	 * @param numberOfFixingDates Number of discrete monitoring dates.
	 * @return Approximate price of the discretely monitored floating-strike lookback call.
	 */
	public static double discretelyMonitoredLookbackPutFloatingStrike(final double spotPrice, final double riskFreeRate, 
			final double volatility, final double maturity, final int numberOfFixingDates)  {
		double theta = beta*volatility*Math.sqrt(maturity/numberOfFixingDates);
		double V = continuouslyMonitoredLookbackPutFloatingStrike(spotPrice*Math.exp(theta), riskFreeRate, volatility, maturity);
		double V_m = Math.exp(-theta)*V+(Math.exp(-theta)-1)*spotPrice;
		return V_m;
	}
	
	public static double discretelyMonitoredLookbackCallFloatingStrike(final double spotPrice, final double riskFreeRate, 
			final double volatility, final double maturity, final int numberOfFixingDates)  {
		double theta = beta*volatility*Math.sqrt(maturity/numberOfFixingDates);
		double V = continuouslyMonitoredLookbackCallFloatingStrike(spotPrice*Math.exp(-theta), 
				riskFreeRate, volatility, maturity);
		double V_m = Math.exp(theta)*V-(Math.exp(theta)-1)*spotPrice;
		return V_m;
	}
	/**
	 * Approximate price of a discretely monitored fixed-strike lookback call.
	 *
	 * <p>Payoff: {@code max(max_i S(t_i) - K, 0)}, where the maximum is observed only
	 * at discrete monitoring dates.</p>
	 * 
	 * @param spotPrice Initial underlying value {@code S0}.
	 * @param riskFreeRate Constant risk-free rate {@code r}.
	 * @param volatility {@code σ}.
	 * @param maturity Time to maturity {@code T}.
	 * @param strike Fixed strike {@code K}.
	 * @param numberOfFixings Number of discrete monitoring dates.
	 * @return Approximate price of the discretely monitored fixed-strike lookback call.
	 */
	public static double discretelyMonitoredLookbackCallFixedStrike(double spotPrice,double riskFreeRate, 
			double volatility, double maturity, double strike, int numberOfFixingDates) {
		double V = discretelyMonitoredLookbackPutFloatingStrike(Math.max(spotPrice, strike), riskFreeRate, volatility, maturity, numberOfFixingDates);
		return V + spotPrice - Math.exp(-riskFreeRate*maturity)*strike;
	}

	public static double discretelyMonitoredLookbackPutFixedStrike(double spotPrice,double riskFreeRate, 
			double volatility, double maturity, double strike, int numberOfFixingDates) {
		double V = discretelyMonitoredLookbackCallFloatingStrike(Math.max(spotPrice, strike), riskFreeRate, volatility, maturity, numberOfFixingDates);
		double V_m = V - spotPrice + strike * Math.exp(-riskFreeRate*maturity);
		return V_m;
	}
	
}
