JAR  = target/Compilador-1.0-SNAPSHOT.jar
MAIN = br.edu.ifsc.compiladores.Main

INPUT_PATH ?= teste.txt
FLAGS ?=

.PHONY: build clean run test

# Compiles the project and generates the .jar
build:
	mvn clean package -DskipTest

# Cleans target/
clean:
	mvn clean

# example: make run INPUT_PATH=teste.txt
# example: make run FLAGS=-v INPUT_PATH="test_batch/ok_teste.txt test_batch/lex_x.txt"
run:
	java -cp $(JAR) $(MAIN) $(FLAGS) $(INPUT_PATH)

test: package
	java -cp $(JAR) $(MAIN) test_batch/

# Exemplos de uso:
# make run INPUT_PATH=test_batch/                                  					  # pasta inteira
# make run FLAGS=-v INPUT_PATH=test_batch/ok_teste.txt               			 	  # com tokens
# make run INPUT_PATH="test_batch/ok_teste.txt test_batch/sint_dec_ordem_errada.txt"  # vários arquivos