# async-dbx-client

Akka/Spray-based Dropbox API client.


## Getting Started using sbt

Since the project is not published yet, you have to build your
own assembly from the sources.

Just clone the project and publish the assembly to your local repository using sbt:

```
git clone https://github.com/mrico/async-dbx-client.git
sbt publish-local
```

Then you can add the dependency to your sbt build configuration:

```
libraryDependencies += "eu.mrico" %% "async-dbx-client" % "0.1.0"
```

### Documentation

[http://mrico.github.io/async-dbx-client/api/latest/](API documentation)


## Copyright & Licence

Copyright (c) 2013 Marco Rico Gomez. Released under the MIT Licence.
