JAVA := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/java,java)
JAVAC := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/javac,javac)

.PHONY: compile test demo clean
compile:
	@mkdir -p build
	@"$(JAVAC)" --release 17 -encoding UTF-8 -d build src/BitInspector.java

test: compile
	@"$(JAVAC)" --release 17 -encoding UTF-8 -cp build -d build test/BitInspectorTest.java
	@"$(JAVA)" -cp build BitInspectorTest

demo: compile
	@sh samples/demo.sh

clean:
	rm -rf build
