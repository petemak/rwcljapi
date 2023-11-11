# How to build a backend
The Cojure way of building applicatiobs is composing them using libraries. These libraries focus on a specific aspect of the application layers (or cross cuttig concerns) and do it well. There are libraries for handling HTTP, request routing, configuration etc. 

Established list of clojures libraries:
[https://github.com/razum2um/awesome-clojure](https://github.com/razum2um/awesome-clojure)
[https://www.clojure-toolbox.com/](https://www.clojure-toolbox.com/)

## Libraries

### Project management

deps.edn ✅

### Component Management

Real world applications have a bunch of services HTTP Server, caches, stream listeners, database connections etc.

- Mount - bad global state
- Component ✅
- Integrant
- System

### Configuration

Configure a system using properties

- Aero ✅
- Config
- Environ
- 

### HTTP Server

- Aleph
- HTTP-Kit
- Jetty ✅
- Nginx-Clojure

### HTTP Adapter /Routing

- Bidi
- Reitit ✅
- Ring ✅
    - **ring-jetty9-adapter (rj9a)**
- Pedestal  ✅
  Best used with Componnt: http://pedestal.io/guides/pedestal-with-component
  Interceptor concept: http://pedestal.io/guides/what-is-an-interceptor

### Schema

- Mali ✅
- Clojure Spec
- Schema - classic library


### Database
- Datomic ✅
- XTDB

### SQL Database
- Honey-Sql
- Hug-Sql
- Postgres

#### Connection pools
- Zaxxer HikariCP

#### Migrations 
- Redgate Flyway
  https://github.com/flyway/flyway

## Testing ##

### HTTP Client
- clj-http 
  https://github.com/dakrone/clj-httpcl

## Testing

### Test frameworks ###
- clojure.test
  https://clojure.github.io/clojure/clojure.test-api.html

### Tet Runner ###
- cognitect-labs test-runner
  https://github.com/cognitect-labs/test-runner
  https://clojure.org/guides/deps_and_cli#test_runner
  
### Test Containers ###
- clj-test-containers -  lightweight wrapper around the Testcontainers Java library.
  https://github.com/testcontainers/testcontainers-clj#clj-test-containers

### Cider running tests
https://github.com/dakrone/clj-http
CIDER provides a minor-mode that automatically runs all tests for a namespace whenever you load a file (with C-c C-k).
- Togal using _M-x cider-auto-test-mode_, or_(cider-auto-test-mode 1)_
- Naming convention: _-test_

Further reading:
- https://docs.cider.mx/cider/testing/running_tests.html
- https://tbellisiv.gitbooks.io/clojure-emacs-cider-intro/content/Cider_Tour/Interactive_Coding/Tests.html

## On Java
Some libraries might require JDK 17 as a minimum. Assuming an Arch Linux distro as OS

### Check available JDKs ###
Updating Arch
  
```
$ pacman -Syyu
```
Listing available 17 packages

```
$ pacman -Ss openjdk | grep 17
```


### Installing a JDK ###
Install JDK, docs and source

```
$ sudo pacman -S jdk17-openjdk openjdk17-doc openjdk17-src
```

To check default version, use archlinux-java

```
$ archlinux-java status
```
Set 17 as default

```
$ sudo archlinux-java set java-17-openjdk
```

Check status

```
$ archlinux-java status
Available Java environments:
  java-11-openjdk
  java-17-openjdk (default)
```
