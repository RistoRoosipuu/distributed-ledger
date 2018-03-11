# Distributed-Ledger

Work in progress on a project to create a Distributed Ledger over HTTP in Java. No extra frameworks. 

# Part One still Todo:

* Refactor the HTTP server Get/Post methods/classes. 
* Fix URLConnection hanging.
* Rewrite Main Method to better reflect server/client creation.  -- Semi-done
* Create a handler for the BlockChain.
* Create and hash the Blocks within the blockchain. -- Done

# Part Two todo:

* Continue to build the entire Ledger.
* Implement the Merkle Tree for blocks. 
* TBD.

# Guide:

* Write url usages <--- Document how commands work

## Authors

* Risto Roosipuu - Initial Creator - [RistoRoosipuu](https://github.com/RistoRoosipuu)

## License

This project is licensed under the MIT license - see the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

* HTTP Client/Server based on https://www.codeproject.com/Tips/1040097/Create-a-Simple-Web-Server-in-Java-HTTP-Server
* This project uses Gson https://github.com/google/gson
