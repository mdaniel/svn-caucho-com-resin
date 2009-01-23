PREFIX=@prefix@
LIBEXEC=@LIBEXEC@

all	: 
	(cd modules/c/src; $(MAKE))

clean	:
	(cd modules/c/src; $(MAKE) clean)
	- rm -r $(LIBEXEC)

install	:
	(cd modules/c/src; $(MAKE) install)
	if test $(PREFIX) != `pwd`; then \
	  mkdir -p $(PREFIX)/lib; \
	  mkdir -p $(PREFIX)/$(LIBEXEC); \
	  cp -r $(LIBEXEC)/* $(PREFIX)/$(LIBEXEC); \
	  cp lib/*.jar $(PREFIX)/lib; \
	  mkdir -p $(PREFIX)/bin; \
	  cp bin/* $(PREFIX)/bin; \
	  mkdir -p $(PREFIX)/webapps; \
	  cp -r webapps/* $(PREFIX)/webapps; \
	  mkdir -p $(PREFIX)/doc; \
	  cp -r doc/* $(PREFIX)/doc; \
	  mkdir -p $(PREFIX)/conf; \
	  cp conf/app-default.xml $(PREFIX)/conf/app-default.xml.orig; \
	  if test ! -r $(PREFIX)/conf/resin.xml; then \
	    cp conf/resin.xml $(PREFIX)/conf/resin.xml; \
	    cp conf/app-default.xml $(PREFIX)/conf/app-default.xml; \
	  fi; \
	fi
