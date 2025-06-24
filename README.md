
# Minesweeper Solver and Analyzer using Top-Down Dynamic Programming and Greedy Algorithms
### A supporting codebase for the paper titled "Improving Decision-Making Accuracy in Minesweeper Solvers through a Combination of Greedy Algorithms and Local Pattern Analysis based on Dynamic Programming"

#### IF2211 Algorithm Strategies - 2025

## Author
* Name          : Sabilul Huda
* NIM           : 13523072
* Student Email : 13523072@std.stei.itb.ac.id
* Personal Email: sabilulhuda060106@gmail.com

## Table of Contents

- [About The Project](#about-the-project)

- [Project Structure](#project-structure)

- [Prerequisites](#prerequisites)

- [Installation and Compilation](#installation-and-compilation)

- [For Windows](#for-windows)

- [For Linux / macOS](#for-linux--macos)

- [Usage](#usage)

- [Running a Single Game (Main.java)](#running-a-single-game-mainjava)

- [Running the Analytics Tool (Analytics.java)](#running-the-analytics-tool-analyticsjava)

- [Configuration File Format](#configuration-file-format)

  

## About The Project

<div  align="justify">

This project provides a comprehensive platform for solving and analyzing the game of Minesweeper. Developed as a practical implementation for an Algorithm Strategies course paper, this Java-based application demonstrates and compares different solving techniques. The project is composed of four main components:

</div>

  

*  **Minesweeper Game Engine**: A fully functional, command-line-based Minesweeper game that serves as the environment for the solvers. It handles board generation, game state, and enforces all game rules.

*  **Hybrid Solver (Greedy + DP)**: The primary solver module that combines a fast Greedy algorithm for deterministic moves with a powerful Top-Down Dynamic Programming (via memoized recursion) approach to solve complex, ambiguous local patterns as Constraint Satisfaction Problems (CSP).

*  **Classic Greedy Solver**: A baseline solver provided for performance comparison. It follows simple deterministic rules and resorts to random guessing when no certain moves are available.

*  **Analytics Program**: A separate tool designed to run a solver on a specific configuration multiple times to gather statistical data, such as win rate, average time, and average steps.

  

## Project Structure

To ensure a clean workspace, the project is organized into the following structure. All commands should be run from the **root directory**.
```bash
.
├── analytics // analytics log can be found here
├── bin // compiled program (.class)
├── compile.bat
├── compile.sh
├── README.md
├── result // result for a minesweeper configuration
│   ├── greedy
│   └── hybrid
├── run_analytics.bat
├── run_analytics.sh
├── run_main.bat
├── run_main.sh
├── src
│   ├── Analytics.java
│   ├── Cell.java
│   ├── GreedySolver.java
│   ├── ISolver.java
│   ├── Main.java
│   ├── MinesweeperGame.java
│   ├── MinesweeperSolver.java
│   └── TeePrintStream.java
└── test // test configuration

```
## Setup and Usage
This project is designed to be run from the command line using helper scripts for compilation and execution.

### Prerequisites
* Java Development Kit (JDK) 11 or higher.

### 1. Initial Setup
Clone or download this repository to your local machine. All the necessary source code is located in the `/src` directory.

### 2. Compilation
Before running any program, you must compile the source code. The compiled `.class` files will be placed in the `/bin` directory.

* **For Linux / macOS:**
    First, make the script executable (you only need to do this once):
    ```bash
    chmod +x compile.sh
    ```
    Then, run the compilation script:
    ```bash
    ./compile.sh
    ```

* **For Windows:**
    Simply run the batch file:
    ```bash
    compile.bat
    ```

### 3. Running the Programs
After successful compilation, you can run the main program or the analytics tool.

#### Running a Single Game (for detailed observation)
This program runs a single, detailed game session, printing every step to the console and saving the full log to a file.

* **For Linux / macOS:**
    ```bash
    ./run_main.sh
    ```
* **For Windows:**
    ```bash
    run_main.bat
    ```
You will be prompted to enter the test case name and choose a solver. The output log will be saved in `result/hybrid/` or `result/greedy/`.

#### Running the Analytics Tool (for statistical analysis)
This program runs the simulation multiple times to gather performance statistics like win rate, average time, and average steps.

* **For Linux / macOS:**
    ```bash
    ./run_analytics.sh
    ```
* **For Windows:**
    ```bash
    run_analytics.bat
    ```
You will be prompted to enter the test case, solver choice, number of trials, and a name for the output report file, which will be saved in the `analytics/` directory.
## Usage
This project is designed to be run from the command line using helper scripts for compilation and execution. Make sure you have completed the setup steps before running the programs.

### Prerequisites
* Java Development Kit (JDK) 11 or higher.

### Step 1: Compile the Project
Before running any program for the first time (or after making changes to the `.java` files), you must compile the source code. The compiled `.class` files will be placed in the `/bin` directory.

* **For Linux / macOS:**
    First, make the script executable (you only need to do this once):
    ```bash
    chmod +x compile.sh
    ```
    Then, run the compilation script:
    ```bash
    ./compile.sh
    ```

* **For Windows:**
    Simply run the batch file:
    ```bash
    compile.bat
    ```

### Step 2: Run the Programs
After successful compilation, you can run the main program or the analytics tool.

#### Running a Single Game (for detailed observation)
This program runs a single, detailed game session, printing every step to the console and saving the full log to a file.

* **For Linux / macOS:**
    ```bash
    ./run_main.sh
    ```
* **For Windows:**
    ```bash
    run_main.bat
    ```
You will be prompted to enter the test case name and choose a solver. The output log will be saved in `result/hybrid/` or `result/greedy/`.

#### Running the Analytics Tool (for statistical analysis)
This program runs the simulation multiple times to gather performance statistics like win rate, average time, and average steps.

* **For Linux / macOS:**
    ```bash
    ./run_analytics.sh
    ```
* **For Windows:**
    ```bash
    run_analytics.bat
    ```
You will be prompted to enter the test case, solver choice, number of trials, and a name for the output report file, which will be saved in the `analytics/` directory.

### Configuration File Format
For both programs (`Main` and `Analytics`), you need to create configuration files inside the `test/` directory. For a test case named `case1`, create a file named `test/case1.txt`.

The format must be exactly three lines as follows:

```txt
{width} {height}
{number_of_bombs}
{start_coordinate_X} {start_coordinate_Y}
```
Description:
* Line 1: Two space-separated integers representing the board's width and height.
* Line 2: A single integer representing the total number of bombs on the board.
* Line 3: Two space-separated integers representing the X and Y coordinates of the first move.

Example File (```test/case1.txt```):

```bash
20 20
40
10 10
```

This configuration will create a 20x20 board with 40 bombs, and the first move will be at coordinate (10, 10).