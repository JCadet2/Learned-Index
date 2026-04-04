JAVAC = javac
JAVA = java

all:
	$(JAVAC) LearnedIndexes.java

run:
	$(JAVA) LearnedIndexes

clean:
	-del *.class