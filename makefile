# Variables de configuración
JC = javac
JVM = java
ANT ?= ant
ENCODING = UTF-8

SRC_DIR = src
BIN_DIR = build/classes
LIB_DIR = lib

CLASSPATH = $(BIN_DIR):$(LIB_DIR)/*
SOURCES = $(shell find $(SRC_DIR) -name "*.java")

.PHONY: all compile clean run-termostato termostato run-grid grid test help

all: compile

# Compilación de todas las fuentes Java
compile:
	@mkdir -p $(BIN_DIR)
	$(JC) -encoding $(ENCODING) -cp "$(LIB_DIR)/*" -d $(BIN_DIR) $(SOURCES)

# Limpieza de archivos compilados
clean:
	rm -rf build dist

# Ejecutar el lanzador del agente Termostato
run-termostato termostato: compile
	$(JVM) -cp "$(CLASSPATH)" lanzadores.LanzadorTermostato

# Ejecutar el lanzador del agente Grid
run-grid grid: compile
	$(JVM) -cp "$(CLASSPATH)" lanzadores.LanzadorGrid

# Pruebas aisladas, sin acceso a DBA Lab
test:
	$(ANT) pruebas-locales

# Ayuda rápida
help:
	@echo "Opciones disponibles en el Makefile:"
	@echo "  make compile         - Compila todo el código fuente en $(BIN_DIR)"
	@echo "  make termostato      - Compila y arranca el agente Termostato"
	@echo "  make grid            - Compila y arranca el agente Grid"
	@echo "  make test            - Ejecuta todas las pruebas locales sin VPN"
	@echo "  make clean           - Elimina los binarios compilados"
	@echo "  make help            - Muestra este mensaje de ayuda"
