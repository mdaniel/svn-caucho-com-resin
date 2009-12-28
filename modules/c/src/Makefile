SHELL=/bin/sh
srcdir = .
top_srcdir = ../../..
plugins=common resin_os

all	: plugins

plugins	:
	for dir in $(plugins); do (cd $$dir; $(MAKE)); done

clean	:
	for dir in common apache1 apache2 $(plugins); do \
	  (cd $$dir; $(MAKE) clean); \
        done

install	:
	for dir in $(plugins); do (cd $$dir; $(MAKE) install); done

