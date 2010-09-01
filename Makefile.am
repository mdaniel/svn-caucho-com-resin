ACLOCAL_AMFLAGS=-I m4
resin_home=$(DESTDIR)@resin_home@
resin_conf=$(DESTDIR)@resin_conf@
resin_root=$(DESTDIR)@resin_root@
resin_log=$(DESTDIR)@resin_log@
resin_init_d=$(DESTDIR)@resin_init_d@
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
	@ if test "$(resin_home)" != $(abs_builddir); then \
	  mkdir -p $(resin_home)/bin; \
	  mkdir -p $(resin_home)/keys; \
	  mkdir -p $(resin_home)/licenses; \
	  mkdir -p $(resin_home)/lib; \
	  mkdir -p $(resin_home)/$(LIBEXEC); \
	  cp bin/* $(resin_home)/bin; \
	  cp lib/*.jar $(resin_home)/lib; \
	fi
	@ if test "$(resin_root)" != $(abs_builddir); then \
		if test -e $(resin_root); then\
			echo "-------------------------------------------------------";\
			echo "Will not overwrite Resin root directory ($(resin_root))";\
			echo "-------------------------------------------------------";\
		else\ 
			mkdir -p $(resin_root)/webapps; \
			cp -r $(CP_U_FLAG) webapps/* $(resin_root)/webapps; \
			mkdir -p $(resin_root)/doc; \
			cp -r $(CP_U_FLAG) doc/* $(resin_root)/doc; \
			mkdir -p $(resin_root)/project-jars; \
			cp -r $(CP_U_FLAG) project-jars/* $(resin_root)/project-jars; \
			mkdir -p $(resin_log); \
		fi;\
	fi
	@ if test "$(resin_conf)" != $(abs_builddir)/conf; then \
	  mkdir -p $(resin_conf); \
		if test -f "$(resin_conf)/resin.xml"; then\
			echo "-------------------------------------------------------";\
			echo "Will not overwrite $(resin_conf)/resin.xml";\
			echo "-------------------------------------------------------";\
		else\
			cp conf/resin.xml $(resin_conf)/resin.xml; \
	  fi;\
		if test -f "$(resin_conf)/app-default.xml"; then\
			echo "-------------------------------------------------------";\
			echo "Will not overwrite $(resin_conf)/app-default.xml";\
			echo "-------------------------------------------------------";\
		else\
			cp conf/app-default.xml $(resin_conf)/app-default.xml; \
		fi;\
	fi
	(cd modules/c/src; $(MAKE) install)
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
		(cd $(resin_pro)/modules/c/src; $(MAKE) install) \
	fi
	@ if test -n "$(resin_init_d)" -a "$(resin_home)" != $(abs_builddir); then \
		if test -f "$(resin_init_d)"; then\
			echo "-------------------------------------------------------";\
			echo "Will not overwrite $(resin_init_d)";\
			echo "-------------------------------------------------------";\
		elif test "$(resin_init_d)" != "$(DESTDIR)"; then\
			mkdir -p `dirname $(resin_init_d)`;\
			cp $(abs_builddir)/init.d/resin $(resin_init_d); \
			chmod +x $(resin_init_d) && echo "$(resin_init_d) installed."; \
	  fi; \
	fi
