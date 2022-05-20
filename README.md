# Java Rabbit Rolodex

A sample project that reveals the secrets of Field-Level Encryption with the Couchbase Java SDK.

This code accompanies the Couchbase Connect 2021 presentation, [Keeping Secrets with Field-Level Encryption (YouTube)](https://www.youtube.com/watch?v=QQ8t4i8ai0s). 

Requires:
* JDK 1.8 or later
* Couchbase Server running on localhost
* A Couchbase user called `Administrator` with password `password`
* A Couchbase bucket called `default`

Edit `app/src/main/java/com/couchbase/example/encryption/App.java` if you'd like to use different Couchbase credentials or bucket.

To run the example:

    ./gradlew run
