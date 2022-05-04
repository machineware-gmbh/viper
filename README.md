# Virtual Platform Explorer (VIPER)
The **Vi**rtual **P**latform **E**xplore**R** `ViPER` is a gui program used to inspect and
control SystemC simulations built with
[`vcml`](https://github.com/janweinstock/vcml).

[![Build Status](https://github.com/machineware-gmbh/viper/actions/workflows/maven.yml/badge.svg)](https://github.com/machineware-gmbh/viper/actions/workflows/maven.yml)

----
## Build
In order to build `ViPER` you need a Java `JDK 11` and Apache `Maven` (version 3+).

```
git clone https://github.com/janweinstock/viper
cd viper
mvn clean verify
```

After the build, your binaries can be found in `product/target/products`.

----
## Pictures

<a href="pictures/uart.png"><img src="pictures/uart.png" alt="viper uart view, Windows" width="400" /></a>
<a href="pictures/smp2.png"><img src="pictures/smp2.png" alt="viper smp cpu view, Ubuntu" width="400" /></a>

----
## License
This project is licensed under the Apache-2.0 license - see the
[LICENSE](LICENSE) file for details.
