# Authenticated Learned Index Implementation

This repository contains the implementation of three authenticated learned index structures: HPVL-tree, PVL-tree, and PVLB-tree. These indexes are designed for efficient and authenticated range queries in outsourced database environments with version control.

## Features

- **HPVL-tree**: A hybrid index framework combining the efficiency of query-friendly PVL-tree and update-friendly PVLB-tree.
- **PVL-tree**: An index structure optimized for query performance using learned models.
- **PVLB-tree**: An index structure optimized for update performance with a buffer mechanism.
- Consistency guarantee through version control update mechanism.
- Support for dynamic authenticated range queries.

## Technology Stack

- Java
- SHA-256 for cryptographic hashing
- JUnit for testing and validation

## Code Structure

The project is organized into several key directories, each containing the implementation of the respective index and its associated algorithms:

### HPVL_tree_index
Contains the implementation of the HPVL-tree index, which combines the benefits of PVL-tree and PVLB-tree for enhanced query and update efficiency. Includes `HPVLChain.java` for performance testing.

### PVL_tree_index
Contains the implementation of the PVL-tree index, optimized for lightweight authenticated range queries. Includes `PVLChain.java` for performance testing.

### PVLB_tree_index
Contains the implementation of the PVLB-tree index, designed for efficient updates with a buffer mechanism. Includes `PVLBChain.java` for performance testing.

## Usage

To use the index implementations and run the performance tests, follow these steps:

1. Clone the repository to your local machine.
2. Compile the Java files using `javac`.
3. Run the test suite using `java` to evaluate the performance of the index structures.

Example compilation and execution commands:

```bash
javac HPVL_tree_index/HPVLChain.java PVL_tree_index/PVLChain.java PVLB_tree_index/PVLBChain.java
java HPVL_tree_index.HPVLChain
java PVL_tree_index.PVLChain
java PVLB_tree_index.PVLBChain
```

## Performance Testing

Each `XXChain.java` file provides a comprehensive test suite for the corresponding index:

- **Index Construction Performance**: Measures the time and resources required to build the index.
- **Query Performance**: Evaluates the efficiency of range queries on the index.
- **Verification Overhead**: Assesses the computational and communication costs associated with query verification.
- **Communication Overhead**: Measures the amount of data transmitted during query verification.

## Contributing

Contributions to the project are welcome. Please submit a pull request with your improvements, and ensure that your changes are well-documented and include relevant unit tests.
