all:
ifeq ($(OS),Windows_NT)
	# assume windows
	javac -Xlint -cp ".;lucene-4.3.0/*" -g *.java
else
	# assume Linux
	javac -cp ".:lucene-4.3.0/*" -g *.java
endif
