Authenticated Learned Index for Outsourced Database with Version Control Update
This repository provides a comprehensive implementation of the novel authenticated learned index structures introduced in the paper "Consistency-Aware Scalable and Authenticated Learned Index for Range Query." The implementation is designed to facilitate efficient and authenticated range queries in outsourced database environments with version control.

Features
Implements the HPVL-tree, a hybrid index framework that combines the efficiency of query-friendly PVL-tree and update-friendly PVLB-tree.
Supports dynamic authenticated queries with a version control update mechanism for consistency guarantee.
Utilizes machine learning models to optimize query processing and verification.
Provides extensive theoretical and experimental analysis to demonstrate performance improvements over state-of-the-art approaches.
Technology Stack
Java
SHA-256 for cryptographic hashing
JUnit for testing and validation
Usage
We have provided a test suite to evaluate the performance of the authenticated learned index structures. To run the tests, execute the following command:

bash
javac Test.java
java Test
This will run the test cases for index construction, querying, and verification.

Code Structure
The project is organized into several key directories, each with a specific purpose:

dataowner
This folder contains the modules related to the Data Owner (DO) entity, responsible for building the authenticated data structures (ADS), generating update information, and updating the digest to form the updated ADS with the new version.

cloudserver
This folder houses the Cloud Server (CS) related modules, which handle client query requests, execute queries over the ADS, and provide verification information for query results.

queryuser
This folder contains the modules for the Query User (QU) entity, responsible for obtaining the latest digest from the DO, initiating query requests with the queried version, and verifying the returned results from the CS.

common
This folder includes common utilities and data structures used across different modules, such as cryptographic functions and ADS implementations.

test
This folder contains the test suite for validating the functionality and performance of the authenticated learned index structures.

Getting Started
To get started with the implementation, follow these steps:

Clone the repository to your local machine.
Compile the Java files using javac.
Run the test suite using java to evaluate the performance of the index structures.
Contributing
Contributions to the project are welcome. Please submit a pull request with your improvements, and ensure that your changes are well-documented and include relevant unit tests.

License
This project is licensed under the MIT License - see the LICENSE file for details.
