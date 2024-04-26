# Distributed Processing Management System :computer:

The system is designed to accept compute tasks from clients and execute them on a pool of processes. Each request from a client includes the name of the task to be executed (e.g., image compression), a payload with the parameters for the computation (e.g., a set of images and a compression ratio), and the directory name to store results.

Compute tasks are submitted to a front-end, which is implemented as a basic REST/Web app. Tasks are scheduled for execution on a set of processes, with each process handling one task at a time. If no process is available, tasks may need to wait. Processes may fail and be restarted, but clients are notified only once when the task completes and the results are successfully stored on the disk.

The full report is available [here](Report.pdf).

## Implementation

The solution was implemented with **Akka** for the system backend, while the client was developed as a simple web page using JavaScript and HTML. The backend consists of a main server actor responsible for task processing by routing messages to a pool of working actors, each specialized in a different type of task. These workers do not handle data saving themselves but rely on their child actors to do so.

This approach allows for the decoupling of task reception, task handling, and data saving, thanks to the use of different actors that are unaffected by each other's failures. Together, they create an easily scalable and maintainable system.

Using the HTTP protocol, the client remains unaware of the technology used by the server and will not be affected by any future changes.

<p align="center">
  <img src="https://github.com/MarcoBendinelli/Distributed-Compute-Management-System/assets/79930488/828807f4-d549-4ce5-ad80-0aa76ef5c326" alt="Architecture Diagram" width="800">
</p>

## Team members
| Name and Surname | Github |
:---: | :---: 
| Marco Bendinelli | [@MarcoBendinelli](https://github.com/MarcoBendinelli) |
| Matteo Beltrante | [@Beltrante](https://github.com/Beltrante) |
| Simone Berasi | [@SimoneBerasi](https://github.com/SimoneBerasi) |

## Guide

### How to run Back-End
To run the backend simply compile the code and run the jar with: 

   ```shell
    > java -jar <package-name>.jar
```
### How to run Client
To run the client open the html file for login in a browser

### Defaults

The server is by default on 127.0.0.1:8600 in case of modifications it is sufficient to change them in this places:
- ```src/main/java/server/StringUtils.java```
- ```src/main/java/client/js/utils.js```

Also JDK used is 11
