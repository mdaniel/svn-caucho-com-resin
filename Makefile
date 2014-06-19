
resin_pro = @resin_pro@
setuid_user = resin
setuid_group = resin

LIBEXEC = libexec64

all	:
	@ (cd modules/c/src; $(MAKE))
	@ if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE); \
	fi

rpm-dist	:
	@ cp conf/resin.properties conf/resin.properties.orig
	@ awk '/setuid_user/ { print "setuid_user : $(setuid_user)"; next; } \
	    /setuid_group/ { print "setuid_group : $(setuid_group)"; next; } \
	    { print $0; }' \
	    conf/resin.properties.orig > conf/resin.properties

install	: .dummy
	(cd modules/c/src; $(MAKE) install)
	sh install.sh

install-native	: .dummy
	(cd modules/c/src; $(MAKE) install)

.dummy	:

clean	:
	(cd modules/c/src; $(MAKE) clean)
	if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) clean; \
	fi
	-rm -r $(LIBEXEC)
