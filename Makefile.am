ACLOCAL_AMFLAGS=-I m4
resin_home=@resin_home@
resin_conf=@resin_conf@
resin_root=@resin_root@
resin_init_d=@resin_init_d@
resin_pro=@resin_pro@
LIBEXEC=@LIBEXEC@

all	: 
	(cd modules/c/src; $(MAKE))
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE); \
	fi

clean	:
	(cd modules/c/src; $(MAKE) clean)
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) clean; \
	fi
	- rm -r $(LIBEXEC)

install	:
	if test $(resin_home) != `pwd`; then \
	  mkdir -p $(resin_home)/licenses; \
	  mkdir -p $(resin_home)/lib; \
	  mkdir -p $(resin_home)/$(LIBEXEC); \
	  cp lib/*.jar $(resin_home)/lib; \
	fi; \
	if test $(resin_root) != `pwd`; then \
	  mkdir -p $(resin_root)/webapps; \
	  cp -r $(CP_U_FLAG) webapps/* $(resin_root)/webapps; \
	  mkdir -p $(resin_root)/doc; \
	  cp -r $(CP_U_FLAG) doc/* $(resin_root)/doc; \
	  mkdir -p $(resin_log); \
	fi; \
	if test $(resin_conf) != `pwd`/conf; then \
	  mkdir -p $(resin_conf); \
	  cp $(CP_U_FLAG) conf/app-default.xml $(resin_conf)/app-default.xml; \
	  cp $(CP_U_FLAG) conf/resin.xml $(resin_conf)/resin.xml; \
	fi; \
	(cd modules/c/src; $(MAKE) install)
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) install; \
	fi
	if test -n "${resin_init_d}"; then \
	  cp $(CP_U_FLAG) init.d/resin $(resin_init_d); \
	  chmod +x $(resin_init_d); \
	fi;

