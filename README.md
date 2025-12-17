# Continuously and Discretely Monitored Lookback Options Pricing

**Authors**: Federico Alberighi, Lorenzo Visonà Dalla Pozza, Alberto Oliva Medin

This Java Maven project implements the pricing of **Lookback Options** (path-dependent) using **Monte Carlo simulations** and **Closed-form Analytic Formulas**.

It was developed as a programming assignment for the *Computational Methods for Finance* course at the University of Verona.

The project handles both **continuously** and **discretely** monitored underlying assets, covering various Lookback option types (Call/Put, Fixed/Floating Strike). It also includes a **Control Variate** implementation to reduce the variance of the Monte Carlo estimator (Bonus task).

## Project Overview

Lookback options are path-dependent contracts whose payoff depends on the extrema (maximum or minimum) reached by the underlying asset during the option's life.

This software provides:
1.  **Monte Carlo Pricing**: A flexible engine based on `finmath-lib` to price options under Black-Scholes and Bachelier models.
2.  **Analytic Formulas**:
    * Exact prices for continuous monitoring.
    * Approximation formulas for discrete monitoring (Broadie, Glasserman, & Kou, 1999).
3.  **Variance Reduction**: A Control Variate technique using the continuous analytical price to improve the discrete MC estimator.
4.  **Convergence Analysis**: Tools to study how the price behaves as the number of monitoring dates increases.

## Implemented Features

The project supports the following payoff types:

* **Fixed Strike Call**: $\max(M_T - K, 0)$
* **Fixed Strike Put**: $\max(K - m_T, 0)$
* **Floating Strike Call**: $\max(S_T - m_T, 0)$
* **Floating Strike Put**: $\max(M_T - S_T, 0)$

Where $M_T$ is the running maximum and $m_T$ is the running minimum of the underlying asset.

### Key Capabilities
* **Dual Monitoring Mode**: The Monte Carlo products can switch between continuous monitoring (using the full simulation grid) and discrete monitoring (using a custom set of fixing dates).
* **Control Variates (Bonus)**: Implementation of `LookbackCallFixedWithBSControlVariate`, which uses the continuously monitored lookback option as a control variate to price the discretely monitored version with significantly lower standard error.
* **Model Agnostic**: While primarily tested under Black-Scholes, the architecture supports other models provided by the `finmath-lib` (e.g., Bachelier Model).

## Code Structure

### `it.univr.montecarlo`
Contains the Monte Carlo product implementations:
* `AbstractLookbackOption.java`: Base abstract class handling common logic (e.g., path-wise min/max calculation, monitoring time grids).
* `LookbackCallFixedStrike.java`, `LookbackPutFixedStrike.java`, etc.: Concrete implementations of the standard payoffs.
* `LookbackCallFixedWithBSControlVariate.java`: **(Bonus)** Implementation of the Control Variate technique for the Fixed Strike Call.

### `it.univr.analyticprices`
* `AnalyticPrices.java`: Contains static methods for:
    * Continuous monitoring exact formulas.
    * Discrete monitoring approximation formulas (Broadie, Glasserman, Kou correction).

### `src/test/java`
* `Tests.java`: Main test suite comparing Monte Carlo results (BS and Bachelier) against analytic benchmarks.
* `PlotTest.java`: Generates convergence plots (Price vs. Number of Monitoring Times).
* `ControlVarieteTest.java`: Demonstrates the efficiency of the Control Variate by comparing the standard deviation of the estimators.

## Requirements & Dependencies

* **Java JDK**: 1.8 or higher.
* **Maven**: For dependency management and build.

**Dependencies** (defined in `pom.xml`):
* `net.finmath:finmath-lib:6.0.28`: Mathematical finance library.
* `net.finmath:finmath-lib-plot-extensions`: Plotting utilities.

## How to Run

1.  **Clone the repository**
2.  **Import into IDE**: Open the project as a Maven Project in Eclipse, IntelliJ IDEA, or VS Code.
3.  **Run the Tests**:
    * **`Tests.java`**: Runs the main pricing comparison logic.
    * **`ControlVarieteTest.java`**: Outputs the variance reduction statistics.
    * **`PlotTest.java`**: Displays the convergence graphs.

## Results

The simulations demonstrate that:
1.  Monte Carlo prices converge to the analytic values as the number of paths increases.
2.  The discrete monitoring approximation (Broadie et al.) is highly accurate compared to the discrete Monte Carlo simulation.
3.  The **Control Variate** method significantly reduces the standard deviation of the estimator, allowing for faster convergence.

## References

1.  **Broadie, M., Glasserman, P., & Kou, S. G.** (1999). *Connecting discrete and continuous path-dependent options*. Finance and Stochastics.

##Ciao Fede
