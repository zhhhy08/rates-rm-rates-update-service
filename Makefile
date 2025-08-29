-include .env

PROJECTNAME := $(shell basename "$(PWD)")

.PHONY: clean say_hello start

## start: 
start:
	@echo "  >  $(PROJECTNAME) is available at $(SAAS)"

## say_hello: 
say_hello:
	@echo "Hello, I'm a PHONY target!"

## clean: 
clean:
	@echo "Cleaning up..."

.PHONY: help
all: help
help: Makefile
	@echo
	@echo " Choose a command run in "$(PROJECTNAME)":"
	@echo
	@sed -n 's/^##//p' $< | column -t -s ':' |  sed -e 's/^/ /'
	@echo