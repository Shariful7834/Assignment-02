# Self-assessment Task 2 – Industrial Process Simulation (Java)
# Md Shariful Islam  - 7213424

## Overview
This project implements the **UML model** provided in the task using **Object-Oriented Programming in Java**.  
It simulates industrial processes in a warehouse, where operations are performed by **Autonomous Guided Vehicles (AGVs)**.

## Implemented Classes

### 1. `IndustrialProcess`
- Holds a list of operations (`IOperation`).
- Provides methods to:
  - Calculate total **process duration**.
  - List required **AGVs**.
  - Calculate total **energy consumption**.

### 2. `IOperation` (Interface)
- Defines an operation with:
  - `ID`, `Description`, `NominalTime`, `Resources (AGVs)`.
- Methods: `setData()`, `getData()`, `getDurationMinutes()`.

### 3. `BaseOperation` (Abstract Class)
- Implements common parts of `IOperation`.
- Stores metadata and AGV assignments.

### 4. `LoadUnloadOperation` (Concrete Operation)
- Models loading/unloading tasks with fixed duration.

### 5. `TransportOperation` (Concrete Operation)
- Models transport tasks.
- Duration = **distance ÷ AGV speed** (+ overhead).

### 6. `AGV`
- Represents an **autonomous vehicle**.
- Attributes: `battery`, `consumption`, `chargingTime`, `speed`, `position`.
- Provides **energy usage calculation**.

## Simulation
- Two example processes created:
  - **Inbound Receiving** (Dock → Transport → Storage).
  - **Outbound Picking** (Rack → Transport → Packing).
- Each process prints:
  - Operation details (time, AGVs).
  - Total duration.
  - Distinct AGVs required.
  - Total energy consumed.

- A **batch simulation** combines processes and prints overall statistics.

## Sample Output
