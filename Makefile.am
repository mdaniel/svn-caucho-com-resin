ACLOCAL_AMFLAGS=-I m4
resin_home=$(DESTDIR)@resin_home@
resin_conf=$(DESTDIR)@resin_conf@
resin_root=$(DESTDIR)@resin_root@
resin_log=$(DESTDIR)@resin_log@
resin_init_d=$(DESTDIR)@resin_init_d@
usr_share=$(DESTDIR)@usr_share@
resin_version=@resin_version@
resin_pro=@resin_pro@
build_dir=@build_dir@
LIBEXEC=@LIBEXEC@

all	:
	(cd modules/c/src; $(MAKE))
	if test -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE); \
	fi

rpm-dist	:
	@ cp conf/resin.properties conf/resin.properties.orig
	@ awk '/setuid_user/ { print "setuid_user : resin"; next; } \
	       /setuid_group/ { print "setuid_group : resin"; next; } \
               { print $0; }' \
	conf/resin.properties.orig > conf/resin.properties

install	:
	@ if test "$(resin_root)" != "$(build_dir)"; then \
	  echo "Installing Resin root directory $(resin_root)."; \
	  if test -e "$(resin_root)"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite Resin webapps $(resin_root)/webapps"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    mkdir -p $(resin_root)/webapps; \
	    cp -r $(CP_U_FLAG) webapps/* $(resin_root)/webapps; \
	  fi; \
	  mkdir -p $(resin_root)/keys; \
	  mkdir -p $(resin_root)/licenses; \
	  mkdir -p $(resin_root)/doc; \
	  mkdir -p $(resin_root)/project-jars; \
	  cp -r $(CP_U_FLAG) doc/* $(resin_root)/doc; \
	  cp -r $(CP_U_FLAG) project-jars/* $(resin_root)/project-jars; \
	fi
	@ if test "$(resin_home)" != "$(build_dir)"; then \
	  echo "Installing Resin home directory $(resin_home)"; \
	  mkdir -p $(resin_home)/bin; \
	  mkdir -p $(resin_home)/lib; \
	  mkdir -p $(resin_home)/endorsed; \
	  mkdir -p $(resin_home)/webapp-jars; \
	  mkdir -p $(resin_home)/$(LIBEXEC); \
	  cp bin/* $(resin_home)/bin; \
	  cp lib/*.jar $(resin_home)/lib; \
	  cp -r $(CP_U_FLAG) endorsed $(resin_home); \
	  cp -r $(CP_U_FLAG) webapp-jars/* $(resin_home)/webapp-jars; \
	  if test -w "$(usr_share)"; then \
	    ln -sf $(resin_version) $(usr_share)/resin; \
	    mkdir -p $(DESTDIR)/usr/bin; \
	    ln -sf @usr_share@/resin/bin/resinctl $(DESTDIR)/usr/bin/resinctl; \
	  fi; \
	fi
	@ if test -e "$(resin_log)"; then \
	  mkdir -p $(resin_log); \
	fi
	@ chmod +x $(resin_home)/bin/resinctl
	@ if test "$(resin_conf)" != "$(build_dir)/conf"; then \
	  mkdir -p $(resin_conf); \
	  if test -f "$(resin_conf)/resin.xml"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_conf)/resin.xml"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    cp conf/resin.xml $(resin_conf)/resin.xml; \
	  fi; \
	  if test -f "$(resin_conf)/resin.properties"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_conf)/resin.properties"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    cp conf/resin.properties $(resin_conf)/resin.properties; \
	  fi; \
	  if test -f "$(resin_conf)/app-default.xml"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_conf)/app-default.xml"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    cp conf/app-default.xml $(resin_conf)/app-default.xml; \
	  fi; \
	  if test -f "$(resin_conf)/cluster-default.xml"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_conf)/cluster-default.xml"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    cp conf/cluster-default.xml $(resin_conf)/cluster-default.xml; \
	  fi; \
	  if test -f "$(resin_conf)/health.xml"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_conf)/health.xml"; \
	    echo "-------------------------------------------------------"; \
	  else \
	    cp conf/health.xml $(resin_conf)/health.xml; \
	  fi; \
	  mkdir -p "$(resin_conf)/resin-inf"; \
	fi
	@ if test -n "$(resin_init_d)" -a "$(resin_home)" != "$(build_dir)"; then \
	  if test -f "$(resin_init_d)"; then \
	    echo "-------------------------------------------------------"; \
	    echo "Will not overwrite $(resin_init_d)"; \
	    echo "-------------------------------------------------------"; \
	  elif test "$(resin_init_d)" != "$(DESTDIR)"; then \
	    mkdir -p `dirname $(resin_init_d)`; \
	    cp $(build_dir)/init.d/resin $(resin_init_d); \
	    chmod +x $(resin_init_d) && echo "$(resin_init_d) installed."; \
	  fi; \
	fi
	@ (cd modules/c/src; $(MAKE) install)
	@ if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  (cd $(resin_pro)/modules/c/src; $(MAKE) install); \
	fi

clean	:
	(cd modules/c/src; $(MAKE) clean)
	if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) clean; \
	fi
	-rm -r $(LIBEXEC)
