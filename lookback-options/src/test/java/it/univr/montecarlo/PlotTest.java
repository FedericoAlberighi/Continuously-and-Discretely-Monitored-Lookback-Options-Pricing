package it.univr.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import it.univr.analyticprices.AnalyticPrices;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.plots.Named;
import net.finmath.plots.Plot2D;
import net.finmath.plots.Plotable2D;
import net.finmath.plots.PlotableFunction2D;
import net.finmath.plots.PlotablePoints2D;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class PlotTest {

    public static void main(String[] args) throws CalculationException, InterruptedException {

        // --- Financial Parameters ---
        double spotPrice = 100.0;
        double riskFreeRate = 0.1;
        double volatility = 0.3;
        double maturity = 0.5;
        double strike = 100.0;

        // --- Simulation Parameters ---
        int numberOfTimeSteps = 1000;
        double timeStep = maturity / numberOfTimeSteps;
        TimeDiscretization times = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);
        
        int numberOfPaths = 20000;
        int seed = 1897;
        BrownianMotion ourDriver = new BrownianMotionFromMersenneRandomNumbers(times, 1, numberOfPaths, seed);
        MonteCarloBlackScholesModel blackScholesProcess = new MonteCarloBlackScholesModel(spotPrice, riskFreeRate, volatility, ourDriver);

        // --- Reference Values Calculation (Horizontal Lines) ---
        // 1. Analytic Price
        double analyticPriceVal = AnalyticPrices.continuouslyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);
        
        // 2. Continuous Monte Carlo Price
        AbstractAssetMonteCarloProduct continuoslyMonitored = new LookbackCallFixedStrike(maturity, strike);
        double mCcontinuousPriceVal = continuoslyMonitored.getValue(blackScholesProcess);

        // --- Plot Setup ---
        
        // Dynamic lists to store points (X: steps, Y: price)
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();

        // Creation of constant lines (Reference lines)
        // We use Named<> to make the name appear in the legend
        DoubleUnaryOperator funcMcContinuous = x -> mCcontinuousPriceVal;
        DoubleUnaryOperator funcAnalytic = x -> analyticPriceVal;

        // PlotableFunction2D(xmin, xmax, points, function, style)
        // We use 2 points (start and end) because they are straight lines
        PlotableFunction2D mCPriceLine = new PlotableFunction2D(0.0, 1000.0, 2, 
                new Named<DoubleUnaryOperator>("MC Continuously Monitored", funcMcContinuous), null);

        PlotableFunction2D analyticPriceLine = new PlotableFunction2D(0.0, 1000.0, 2, 
                new Named<DoubleUnaryOperator>("Analytic Price", funcAnalytic), null);

        // Chart initialization
        Plot2D lookbackPlot = new Plot2D(0.0, 1050.0, 0, Arrays.asList());
        lookbackPlot.setXAxisLabel("Number of monitoring times");
        lookbackPlot.setYAxisLabel("Price");
        lookbackPlot.setIsLegendVisible(true);
        
        // Set Y range for a good initial visualization
        lookbackPlot.setYRange(9, mCcontinuousPriceVal + 5); 
        lookbackPlot.show();

        // --- Dynamic Loop ---
        int currentTimeSteps = 1;
        
        // Loop up to 1006 steps
        while (currentTimeSteps < 1006) {

            // Price calculation with current discrete monitoring
            AbstractAssetMonteCarloProduct discretelyMonitored = new LookbackCallFixedStrike(maturity, strike, currentTimeSteps);
            double lookbackPrice = discretelyMonitored.getValue(blackScholesProcess);

            // Add data to lists
            xData.add((double) currentTimeSteps);
            yData.add(lookbackPrice);

            // List -> Primitive Array conversion for the plot
            double[] currentXArr = xData.stream().mapToDouble(d -> d).toArray();
            double[] currentYArr = yData.stream().mapToDouble(d -> d).toArray();

            // Create the points series. 
            // The string "MC Discrete Price" will appear in the legend associated with the points.
            PlotablePoints2D pointsSeries = PlotablePoints2D.of(
                "MC Discrete Price", 
                currentXArr, 
                currentYArr, 
                null
            );

            // Update the plot with all 3 elements
            List<Plotable2D> plotables = Arrays.asList(pointsSeries, mCPriceLine, analyticPriceLine);
            lookbackPlot.update(plotables);

            // Increment and Pause
            currentTimeSteps += 10;
            Thread.sleep(100); 
        }
    }
}