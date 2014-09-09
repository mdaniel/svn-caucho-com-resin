ACLOCAL_AMFLAGS=-I m4
abs_resin_home=@resin_home@
resin_home=$(DESTDIR)@resin_home@
resin_conf=$(DESTDIR)@resin_conf@
resin_root=$(DESTDIR)@resin_root@
resin_log=$(DESTDIR)@resin_log@
resin_init_d=$(DESTDIR)@resin_init_d@
initd_dir=$(DESTDIR)@initd_dir@
usr_share=$(DESTDIR)@usr_share@
abs_usr_share=@usr_share@
resin_version=@resin_version@
resin_pro=@resin_pro@
build_dir=@build_dir@
setuid_user=@setuid_user@
setuid_group=@setuid_group@
setuid_set=@setuid_set@
user_add=@user_add@
group_add=@group_add@
LIBEXEC=@LIBEXEC@
skip_check_resin_sh=@SKIP_CHECK_RESIN_SH@
root_subdirs_preserve=project-jars webapps webapp-jars endorsed resin-inf app-inf
root_subdirs_overwrite=doc
home_subdirs=@LIBEXEC@ bin lib
conf_files=resin.xml resin.properties app-default.xml cluster-default.xml health.xml
conf_subdirs=keys licenses

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

install	:
	@ if test "$(setuid_set)" = "true"; then \
	  cp conf/resin.properties conf/resin.properties.orig; \
	  awk '/setuid_user/ { print "setuid_user : $(setuid_user)"; next; } \
	      /setuid_group/ { print "setuid_group : $(setuid_group)"; next; } \
	      { print $0; }' \
	      conf/resin.properties.orig > conf/resin.properties; \
	fi
	@ if test "$(resin_root)" != "$(build_dir)"; then \
	  echo "Installing Resin root $(resin_root)."; \
	  if test ! -e "$(resin_root)"; then \
	    mkdir -p $(resin_root); \
	    if test $$? != "0"; then \
	      echo "ABORT: Resin root install failed. Ensure $(resin_root) can be created by $$USER, or run 'make install' as root."; \
	      exit 42; \
	    fi; \
	  else \
	    if test ! -w "$(resin_root)"; then \
	      echo "ABORT: Resin root install failed. Ensure $(resin_root) is writable by $$USER, or run 'make install' as root."; \
	      exit 42; \
	    fi; \
	  fi; \
	  for subdir in $(root_subdirs_preserve); do \
	    if test -e "$(resin_root)/$$subdir"; then \
	      echo "Preserving existing Resin root subdir $(resin_root)/$$subdir.";\
	    else \
	      echo "Installing Resin root subdir $(resin_root)/$$subdir."; \
	      mkdir -p $(resin_root)/$$subdir; \
	      if test $$? != "0"; then \
	        echo "ABORT: Resin root subdir creation failed. Ensure $(resin_root)/$$subdir can be created by $$USER, or run 'make install' as root."; \
	        exit 42; \
	      elif cp -r $$subdir/* $(resin_root)/$$subdir; then \
	        echo "updated $(resin_root)/$$subdir"; \
	      else \
	        echo "skipped $$subdir"; \
	      fi; \
	    fi; \
	  done; \
	  for subdir in $(root_subdirs_overwrite); do \
	    if test -e "$(resin_root)/$$subdir"; then \
	      echo "Updating existing Resin root subdir $(resin_root)/$$subdir.";\
	    else \
	      echo "Installing Resin root subdir $(resin_root)/$$subdir."; \
	      mkdir -p $(resin_root)/$$subdir; \
	      if test $$? != "0"; then \
	        echo "ABORT: Resin root subdir creation failed. Ensure $(resin_root)/$$subdir can be created by $$USER, or run 'make install' as root."; \
	        exit 42; \
	      fi; \
	    fi; \
	    cp -r $$subdir/* $(resin_root)/$$subdir; \
	  done; \
	fi
	@ if test "$(resin_home)" != "$(build_dir)"; then \
	  echo "Installing Resin home $(resin_home)."; \
	  if test ! -e "$(resin_home)"; then \
	    mkdir -p $(resin_home); \
	    if test $$? != "0"; then \
	      echo "ABORT: Resin home install failed. Ensure $(resin_home) can be created by $$USER, or run 'make install' as root."; \
	      exit 42; \
	    fi; \
	  else \
	    if test ! -w "$(resin_home)"; then \
	      echo "ABORT: Resin home install failed. Ensure $(resin_home) is writable by $$USER, or run 'make install' as root."; \
	      exit 42; \
	    fi; \
	  fi; \
	  for subdir in $(home_subdirs); do \
	    if test -e "$(resin_home)/$$subdir"; then \
	      echo "Updating existing Resin home subdir $(resin_home)/$$subdir.";\
	    else \
	      echo "Installing Resin home subdir $(resin_home)/$$subdir."; \
	      mkdir -p $(resin_home)/$$subdir; \
	      if test $$? != "0"; then \
	        echo "ABORT: Resin home subdir creation failed. Ensure $(resin_home)/$$subdir can be created by $$USER, or run 'make install' as root."; \
	        exit 42; \
	      fi; \
	    fi; \
	    if cp -r $$subdir/* $(resin_home)/$$subdir; then \
	      echo "updated $(resin_home)/$$subdir"; \
            else \
              echo "skipped $$subdir"; \
	    fi; \
	  done; \
	fi
	@ if test -e "$(resin_log)"; then \
	  echo "Preserving existing Resin log $(resin_log)."; \
	else \
	  echo "Installing Resin log $(resin_log)."; \
	  mkdir -p $(resin_log); \
	  if test $$? != "0"; then \
	    echo "ABORT: Resin log install failed. Ensure $(resin_log) can be created by $$USER, or run 'make install' as root."; \
	    exit 42; \
	  fi; \
	fi
	@ if test -n "$(abs_usr_share)"; then \
	  echo "Installing Resin home symlink $(abs_usr_share)/resin."; \
	  if test ! -w "$(usr_share)"; then \
	    echo "WARNING: Resin home symlink install failed. Ensure $(usr_share) is writable by $$USER, or run 'make install' as root."; \
	  else \
	    ln -sf $(abs_resin_home) $(usr_share)/resin; \
	    if test $$? != "0"; then \
	      echo "WARNING: Resin home symlink creation failed. Ensure $(usr_share) is writable by $$USER, or run 'make install' as root."; \
	    fi; \
	  fi; \
	  echo "Installing resinctl symlink $(DESTDIR)/usr/bin/resinctl."; \
	  mkdir -p $(DESTDIR)/usr/bin; \
	  ln -sf "$(abs_usr_share)/resin/bin/resinctl" "$(DESTDIR)/usr/bin/resinctl"; \
	  if test $$? != "0"; then \
	    echo "WARNING: resinctl symlink creation failed. Ensure $(DESTDIR)/usr/bin is writable by $$USER, or run 'make install' as root."; \
	  fi; \
	fi
	@ if test -e "$(resin_home)/bin/resinctl"; then \
	  echo "Setting $(resin_home)/bin/resinctl executable."; \
	  chmod +x $(resin_home)/bin/resinctl; \
	  if test $$? != "0"; then \
	    echo "WARNING: failed to set resinctl executable. Ensure $(resin_home)/bin/resinctl is writable by $$USER, or run 'make install' as root."; \
	  fi; \
	else \
	  echo "ABORT: $(resin_home)/bin/resinctl does not exist. Resin home install may have failed."; \
	  exit 42; \
	fi
	@ if test -e "$(resin_home)/bin/resin.sh"; then \
	  echo "Setting $(resin_home)/bin/resin.sh executable."; \
	  chmod +x $(resin_home)/bin/resin.sh; \
	  if test $$? != "0"; then \
	    echo "WARNING: failed to set resin.sh executable. Ensure $(resin_home)/bin/resin.sh is writable by $$USER, or run 'make install' as root."; \
	  fi; \
	elif test "$(skip_check_resin_sh)" = "false"; then \
	  echo "ABORT: $(resin_home)/bin/resin.sh does not exist. Resin home install may have failed."; \
	  exit 42; \
	fi
	@ if test "$(resin_conf)" != "$(build_dir)/conf"; then \
	  echo "Installing Resin conf $(resin_conf)."; \
	  if test ! -e "$(resin_conf)"; then \
	    mkdir -p $(resin_conf); \
	    if test $$? != "0"; then \
	      echo "ABORT: Resin conf install failed. Ensure $(resin_conf) can be created by $$USER, or run 'make install' as root."; \
	      exit 42; \
	    fi; \
	  fi; \
	  if test ! -w "$(resin_conf)"; then \
	    echo "ABORT: Resin conf install failed. Ensure $(resin_conf) is writable by $$USER, or run 'make install' as root."; \
	    exit 42; \
	  fi; \
	  for subdir in $(conf_subdirs); do \
	    if test -e "$(resin_conf)/$$subdir"; then \
	      echo "Preserving existing Resin conf subdir $(resin_conf)/$$subdir."; \
	    else \
	      echo "Installing Resin conf subdir $(resin_conf)/$$subdir."; \
	      mkdir -p $(resin_conf)/$$subdir; \
	      if test $$? != "0"; then \
	        echo "ABORT: Resin conf subdir creation failed. Ensure $(resin_conf)/$$subdir can be created by $$USER, or run 'make install' as root."; \
	        exit 42; \
	      elif cp -r $$subdir/* $(resin_conf)/$$subdir; then \
	        echo "updated $(resin_conf)/$$subdir"; \
	      else \
	        echo "skipped $$subdir"; \
	      fi; \
	    fi; \
	  done; \
	  for file in $(conf_files); do \
	    if test -f "$(resin_conf)/$$file"; then \
	      echo "Preserving existing Resin conf file $(resin_conf)/$$file.";\
	    else \
	      echo "Installing Resin conf file $(resin_conf)/$$file."; \
	      cp conf/$$file $(resin_conf)/$$file; \
	    fi; \
	  done; \
	fi
	@ if test -n "$(resin_init_d)"; then \
	  if test -f "$(resin_init_d)"; then \
	    echo "Preserving existing Resin init.d script $(resin_init_d) in $(initd_dir)."; \
	  else \
	    echo "Installing Resin init.d script $(resin_init_d)."; \
	    mkdir -p $(initd_dir); \
	    if test ! -w "$(initd_dir)"; then \
	      echo "WARNING: Resin init.d script install failed. Ensure $(init_dir) is writable by $$USER, or run 'make install' as root."; \
	    else \
	      cp init.d/resin $(resin_init_d); \
	      chmod +x "$(resin_init_d)"; \
	      if test $$? != "0"; then \
	        echo "WARNING: failed to set Resin init.d executable. Ensure $(resin_init_d) is writable by $$USER, or run 'make install' as root."; \
	      fi; \
	    fi; \
	  fi; \
	fi
	@ (cd modules/c/src; $(MAKE) install)
	@ if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  (cd $(resin_pro)/modules/c/src; $(MAKE) install) \
	fi
	@ if test "$(setuid_set)" = "true"; then \
	  if test -n "$(group_add)"; then \
	    echo "Creating setuid group $(setuid_group)."; \
	    egrep '$$$(setuid_group):' /etc/group 1>/dev/null 2>/dev/null; \
	    if test $$? = "0"; then \
	      echo "setuid group $(setuid_group) already exists."; \
	    else \
	      groupadd $(setuid_group); \
	      if test $$? != "0"; then \
	        echo "WARNING: failed to create the $(setuid_group) group. The group may already exist, or run 'make install' as root."; \
	      fi; \
	    fi; \
	  fi; \
	  if test -n "$(user_add)"; then \
	    echo "Creating setuid user $(setuid_user)."; \
	    egrep '$$$(setuid_user):' /etc/passwd 1>/dev/null 2>/dev/null; \
	    if test $$? = "0"; then \
	      echo "setuid user $(setuid_user) already exists."; \
	    else \
	      useradd -d /nonexistent -s /bin/false -g $(setuid_group) $(setuid_user); \
	      if test $$? != "0"; then \
	        echo "WARNING: failed to create the $(setuid_user) user. The user may already exist, or run 'make install' as root."; \
	      fi; \
	    fi; \
	  fi; \
	  if test -n "$(user_add)"; then \
	    echo "Changing the owner of Resin root $(resin_root) to $(setuid_user):$(setuid_group)."; \
	    chown -R $(setuid_user):$(setuid_group) $(resin_root); \
	    if test $$? != "0"; then \
	      echo "WARNING: failed change owner of Resin root to $(setuid_user). Ensure $(resin_root) is writable by $$USER, or run 'make install' as root."; \
	    fi; \
	  fi; \
	  if test -n "$(group_add)"; then \
	    echo "Changing the owner of Resin log $(resin_log) to $(setuid_user):$(setuid_group)."; \
	    chown -R $(setuid_user):$(setuid_group) $(resin_log); \
	    if test $$? != "0"; then \
	      echo "WARNING: failed change owner of Resin log to $(setuid_user). Ensure $(resin_log) is writable by $$USER, or run 'make install' as root."; \
	    fi; \
	  fi; \
	fi

clean	:
	(cd modules/c/src; $(MAKE) clean)
	if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then \
	  cd $(resin_pro)/modules/c/src; $(MAKE) clean; \
	fi
	-rm -r $(LIBEXEC)
