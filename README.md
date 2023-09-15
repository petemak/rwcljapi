# How to build a backend
List of Lojures libraries 
[https://github.com/razum2um/awesome-clojure](https://github.com/razum2um/awesome-clojure)
[https://www.clojure-toolbox.com/](https://www.clojure-toolbox.com/)

## Project management

deps.edn ✅

### Component Management

Real world applications have a bunch of services HTTP Server, caches, stream listeners, database connections etc.

- Mount - bad global state
- Component
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

### Schema

- Mali
- Clojure Spec
- Schema - classic library

### Routing Client

- clj-http

### Database
- Datomic
- XTDB

### SQL Database
- Honey-Sql
- Hug-Sql
