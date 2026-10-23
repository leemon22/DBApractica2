#Variables
LIB = lib/jade.jar
BIN = build/classes

.PHONY: compile clean

# Compilar el proyecto con Ant
compile:
	ant compile

# Limpiar archivos compilados
clean:
	ant clean
