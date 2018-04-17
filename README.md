# SecStore Design Project

## Getting Started

Simply clone this repository and you're good to go. The noteworthy code files are as follows:

```
* src/client/CP1Client.java - Implementation of CP1
* src/client/CP2Client.java - Implementation of CP2
* src/common/Packet.java - List of all packets used in communication
* src/common/Protocol.java - List of all supported communication protocols
```

### Prerequisites

You will need the following installed on your system:

```
* Java SE 9
* JavaFX
```

JavaFX is required in order to run the Design Challenge GUI application.

## Deployment

To run the system, you first need to start the server. All runnable files are contained within the outputs folder. You can start the server using the following command:
```
cd outputs
java -jar Server.jar
```

For the clients, the syntax is similar. CP1 implements RSA encryption while CP2 implements AES encryption. You can execute them with:
```
java -jar CP<1|2>Client.jar <file to upload>
```

Finally, for the JavaFX application, you will need to load the project into either Eclipse or IntelliJ with the Eclipse Project Integration plugin. From there you can run Main.java, which will launch the main user interface.

## Authors

* **Isaac Ashwin** - *1002151* - [ashiswin](https://github.com/ashiswin)
* **Tan Oon Tong** - *1002155* - [oonyoontong](https://github.com/oonyoontong)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

