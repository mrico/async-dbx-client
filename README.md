# async-dbx-client

Akka/Spray-based Dropbox API client.

The status of the currently supported &amp; upcoming features can be found [here](https://github.com/mrico/async-dbx-client/issues/1).


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

## Documentation

The latest documentation of the API can be found [here](http://mrico.github.io/async-dbx-client/api/latest/).

Examples can be found in the [test suite](src/test/scala/DbxClientSpec.scala).

### Connecting to a Dropbox account

The main entry point is the [DbxClient](http://mrico.github.io/async-dbx-client/api/latest/#asyncdbx.DbxClient). An instance of [DbxClient](http://mrico.github.io/async-dbx-client/api/latest/#asyncdbx.DbxClient) is linked to a single Dropbox account and provides access to the API.

```scala
import api._
val client = system.actorOf(Props[DbxClient])
client ! Authenticate(token)
client ! Account.GetInfo // => Response: Account.Info
```

### Keeping a local folder in sync with remote changes

The actor [DbxSync](http://mrico.github.io/async-dbx-client/api/latest/#asyncdbx.DbxSync) can be used to keep a local folder in sync with a given Dropbox account.

```scala
val sync = system.actorOf(DbxSync.props(client, "/tmp/dbx"))
// ... later ...
sync ! PoisenPill
```

### Running the test suite

Sign in to your dropbox account and [create a new app](https://www.dropbox.com/developers/apps).

Generate a new access token and put it into a file called `test-config.conf` which you create in the project folder:

```
tests {
  auth-token = "YOUR_DROPBOX_ACCESS_TOKEN"
}
```

Finally run the tests:

```bash
$> sbt test
```

## Copyright & Licence

Copyright (c) 2016 Marco Rico Gomez. Released under the MIT Licence.
