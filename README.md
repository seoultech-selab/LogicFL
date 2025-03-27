# LogicFL

This repository provides the core LogicFL implementation on Java.  

### Steps to Run

* You need to create ``config.properties`` file for configuration first. Please refer to a sample in ``src/test/resources/config.propertes``.
* To collect necessary information LogicFL should be executed in the following order.
  - ``CoverageAnalyzer``: it will obtain coverage information.
  - ``StaticAnalyzer``: it will collect facts from covered lines.
  - ``DynamicAnalyzer``: it will collect facts during test execution.
  - ``FaultLocalizer``: lastly it provides the fault localization results.

#### Requirements

To execute LogicFL, the followings are necessary.

* Java 17 or above.
* [SWI-Prolog](https://www.swi-prolog.org/) installed with [JPL](https://jpl7.org/). 
    - ``FaultLocalizer`` requires both of them installed properly.
    - We recommend to install SWI-Prolog with PPA on Linux machine (check [here](https://www.swi-prolog.org/build/PPA.html)).
        - If it is properly installed, you can find ``libjpl.so`` file under ``/usr/lib/swi-prolog/lib/x86_64-linux``.
