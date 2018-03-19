# Distributed-Ledger

Work in progress on a project to create a Distributed Ledger over HTTP in Java. No extra frameworks. 

# Part One still Todo:

* Refactor the HTTP server Get/Post methods/classes. -- Done 
* Fix URLConnection hanging. -- Done
* Rewrite Main Method to better reflect server/client creation.  -- Done
* Create a handler for the BlockChain. -- Done
* Create and hash the Blocks within the blockchain. -- Done
  
  # Part One Bugs/Improvements.
    * Bug: If a known Peer is offline and the /block handler is used, each unsuccessful UrlConnection creates its own Block
    * Improvement: The Send/Receiver parts for Block/Transaction are basically the same. Should be easy to refactor them into
                   a more overarching methods.
  
# Part Two todo:

* Continue to build the entire Ledger.
* Implement the Merkle Tree for blocks. 
* TBD.

# Guide:

* For each activation of the main class, you get the option to:
      * Write 'y' and enter port for a Standalone Node.
      * Write 'n', enter the IP you want to connect to and the port for the Node that establishes a connection from the start.
* 'IP:Port/getPeers' -- prints out the specific Node/Peers known IP/Port combinations.
* 'IP:Port/getblocks' -- getter that prints out all known Blocks for the specific Node/Peer.(Getter is without arguments)
                      -- prints out all known Blocks starting from a specific Block.(Getter with arguments).
                                                     Example: '192.168.1.121:9000/getblocks?blockId=Block_Hex_Value'
* 'IP:Port/getdata' -- getter that requires arguments. Finds specific Block and prints out its content in JSON format.
                                                     Example: '192.168.1.121:9000/getdata?blockId=Block_Hex_Value'
* 'IP:Port/block' -- Handler that sends a message to the Node to assemble and send out a Block to all known peers.
                     If a receiver Peer does not have the Block, it will starts its own UrlConnection to its known Peers.
* 'IP:Port/transaction' -- Handler that sends a message to the Node to assemble and send out a Transaction to all known peers.
                           If a receiver Peer does not have the Transaction, it will starts its own UrlConnection to its known Peers.

## Authors

* Risto Roosipuu - Initial Creator - [RistoRoosipuu](https://github.com/RistoRoosipuu)

## License

This project is licensed under the MIT license - see the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

* HTTP Client/Server based on https://www.codeproject.com/Tips/1040097/Create-a-Simple-Web-Server-in-Java-HTTP-Server
* This project uses Gson https://github.com/google/gson
