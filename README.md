# Virtual Platform Explorer (VIPER)
The **VI**rtual **P**latform **E**xplore**R** `viper` is a gui program used to inspect and
control SystemC simulations built with
[VCML](https://github.com/janweinstock/vcml).

[![Build Status](https://travis-ci.org/janweinstock/viper.svg?branch=master)](https://travis-ci.org/janweinstock/viper)

----
## Build
In order to build `viper` you need a Java `JDK 8` and Apache `Maven` (version 3+).

```
git clone https://github.com/janweinstock/viper.git viper
cd viper
mvn clean verify
```

After the build, your binaries can be found in `product/target/products`.

----
## License
This project is licensed under the Apache-2.0 license - see the
[LICENSE](LICENSE) file for details.