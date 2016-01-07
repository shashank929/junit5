= The JUnit5 Launcher API

One of the prominent goals of JUnit 5 is to make the interface between JUnit
and its programmatic clients – build tools and IDEs – more powerful and stable.
The purpose is to decouple the internals of discovering and executing tests
from all the filtering and configuration that's necessary from the outside.

For the prototype we came up with the concept of a `Launcher` that
can be used to discover, filter, and execute JUnit tests. Moreover, we
added a mechanism to allow third party test libraries – like Spock, Cucumber,
and FitNesse – to plug into JUnit 5's launching infrastructure.

The launching API is in the https://github.com/junit-team/junit-lambda/tree/prototype-1/junit-launcher[junit-launcher] project.

An example consumer of the launching API is our https://github.com/junit-team/junit-lambda/blob/prototype-1/junit-console/src/main/java/org/junit/gen5/console/ConsoleRunner.java[Console Runner]
in the https://github.com/junit-team/junit-lambda/tree/prototype-1/junit-console[junit-console] project.

== Discovering Tests

Introducing _test discovery_ as a dedicated feature of JUnit itself will (hopefully)
free IDEs and build tools from most of the difficulties they had to go through
to identify test classes and test methods in the past.

Usage Example:

[source,java]
----
import static org.junit.gen5.engine.TestPlanSpecification.*;

TestPlanSpecification specification = build(
    forPackage("com.mycompany.mytests"),
    forClass(MyTestClass.class)
).filterWith(classNameMatches("*Test").or(classNameMatches("Test*")));

TestPlan plan = new Launcher().discover(specification);
----

There's currently the possibility to search for classes, methods,
all classes in a package, or even all tests in the classpath. Discovery
takes place across all participating test engines.

The resulting test plan is basically a hierarchical (and read-only)
description of all engines, classes, and test methods that fit
the `specification` object. The client can traverse the tree, retrieve
details about a node, and get a link to the original source (like class,
method, or file position). Every node in the test plan tree has a
_unique ID_ that can be used to invoke a particular test or group of
tests.

== Running Tests

There are two ways to execute tests. Clients can either use the same
test specification object as in the discovery phase, or – to speed
things up a bit – pass in the prepared `TestPlan` object from a previous
discovery step. Test progress and result reporting can be achieved
through a https://github.com/junit-team/junit-lambda/blob/prototype-1/junit-launcher/src/main/java/org/junit/gen5/launcher/TestPlanExecutionListener.java[TestPlanExecutionListener]:

[source,java]
----
TestPlanSpecification specification = build(
    forPackage("com.mycompany.mytests"),
    forClass(MyTestClass.class)
).filterWith(classNameMatches("*Test").or(classNameMatches("Test*")));

Launcher launcher = new Launcher();
TestPlanExecutionListener listener = createListener();
launcher.registerTestPlanExecutionListener(listener);

launcher.execute(specification);
----

There's currently no result object, but you can easily use
a listener to aggregate the final results in an object of your own.
For an example see the https://github.com/junit-team/junit-lambda/blob/prototype-1/junit-console/src/main/java/org/junit/gen5/console/TestSummaryReportingTestListener.java[TestSummaryReportingTestListener].

== Plugging in Your Own Test Engine

The prototype currently provides two `TestEngine` implementations out of the box:

* https://github.com/junit-team/junit-lambda/tree/prototype-1/junit5-engine[junit5-engine]: The core of the current prototype.
* https://github.com/junit-team/junit-lambda/tree/prototype-1/junit4-engine[junit4-engine]: A thin layer on top of JUnit 4 to allow running "old" tests with the launcher infrastructure.

Third parties may also contribute their own `TestEngine` by implementing the interfaces in the https://github.com/junit-team/junit-lambda/tree/prototype-1/junit-engine-api[junit-engine-api] project and _registering_ their engine. Engine registration is currently supported via Java's `java.util.ServiceLoader` mechanism. For example, the `junit5-engine` project registers its `JUnit5TestEngine` https://github.com/junit-team/junit-lambda/blob/prototype-1/junit5-engine/src/main/resources/META-INF/services/org.junit.gen5.engine.TestEngine[here].