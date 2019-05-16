# sdm-jade
Sparse Distributed Memory implemented with JADE Agent Development Framework

## Overall Scenario
This distributed storage backs up and restores chunks of X bits of data, where X is a configurable parameter, which can take any values. The implementation is tested on X equal 1000 bit, 10000 bits, 100000 bits. The size of data chunks is configured during system initialization and remains unchanged during the system’s lifetime. It can be assumed as the fixed length of the packet in your system.

![P2P scheme](architecture/peer-to-peer-scheme.png) 

Architecturally the storage system is a hybrid peer-to-peer network with peer nodes(PeerNode) that preform the actual task of storage and reconstruction of data and the infrastructure nodes implementing helper functions(HelperNode).

The software architecture is designed to support a potentially large number of peers P (P is in the order of 10^6 – one million – nodes being active simultaneously).
The data to be stored is randomly generated sequences of X bits chunks. The system is tested on storing 10, 000 chunks of Data. The Data can enter the system from any peer node. Each peer will have a capacity of storing up to N chunks.

The entire peer-to-peer system maintains a pool of addresses for storage locations. The number of storage locations across the system is large (more than 106). This global number of storage locations (denoted as S=PN) is a configurable parameter via P and N.

Each storage location is X bins in size and is addressed by an X bits address, where X is the size of the data chunk. The addresses are X-bit binary sequences, which are RANDOMLY generated during the peer’s initialization time and remain stable during the peer’s lifetime, i.e. each peer has an NxX matrix for addressing purposes. Note that at the initialization time all storage locations are empty.

The Data is stored based on the principles of proximity/similarity between the Data chunk and the address. The proximity metric is Hamming distance (the number of bit positions in which two sequences disagree). The threshold T (in number of different bits) is a configurable parameter of the system. The incoming Data is stored only in these storage locations for which Hamming distance between Data and the location’s address is less or equal T. Thus, Data is stored in many (in the order of tens of thousand) locations on different peers.

## The procedure to storing data
The data is stored in a somewhat unconventional way. It is POSITIONWISE added to the content of all identified storage locations. While the Data is binary in nature, for the ease of computations at this step ‘0’ is encoded as “-1” and ‘1’ as “+1”, to convert data from binary format to bipolar format. Also configurable threshold (C) is implemented, constant for each position of the storage. The value of each position is in the range [-C, +C].

![Storage model](architecture/storage-model.png)

## The search procedure
The architecture design is best suited for the best match search. This means that the query to the system is a Data chunk of size X bits, which is similar to one of the stored Data chunks. The proximity is again measured by Hamming distance. The query can be generated at any peer and the network cooperatively finds the best matching stored Data. To do this the querying peer performs several iterations monitoring Hamming distance between the response of the iteration i-1 and the current iteration. The distance either converge to 0 or diverge to X/2. This is the search TERMINATION criteria.
The RESULT of the query is either the binarized Data chunk from the latest iteration if the search procedure has converged or an error code if the procedure has diverged.

## Performance testing
![Performance testing](architecture/performance.png)


