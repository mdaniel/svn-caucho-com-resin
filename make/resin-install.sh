build_dir=`sh resin-config --build-dir`

abs_resin_home=`sh resin-config --home-dir`
resin_home=${DESTDIR}${abs_resin_home}
resin_root=${DESTDIR}`sh resin-config --root-dir`
resin_log=${DESTDIR}`sh resin-config --log-dir`
resin_conf=${DESTDIR}`sh resin-config --conf-dir`

abs_usr_share=`sh resin-config --usr-share`
usr_share=${DESTDIR}${abs_usr_share}

resin_init_d=${DESTDIR}`sh resin-config --init-d`
initd_dir=${DESTDIR}`sh resin-config --init-d-dir`

libexec=`sh resin-config --libexec`

home_subdirs="$libexec bin lib"
root_subdirs_preserve="project-jars webapps webapp-jars endorsed resin-inf app-inf"
root_subdirs_overwrite=doc

conf_files="resin.xml resin.properties app-default.xml cluster-default.xml health.xml"
conf_subdirs="keys licenses"

#setuid_user=@setuid_user@
#setuid_group=@setuid_group@
#setuid_set=@setuid_set@
#user_add=@user_add@
#group_add=@group_add@

#skip_check_resin_sh=@SKIP_CHECK_RESIN_SH@

function die()
{
  echo "ABORT: $1";
  exit 42
}

function mkdir_safe()
{
  if test ! -e "$1"; then
  
    mkdir -p "$1" || die "$2 failed. Ensure $1 can be created by ${USER}, or run 'make install' as root.";

  else
    if test ! -w "$1"; then
      die "Resin $2 failed. Ensure $1 is writable by ${USER}, or run 'make install' as root."
    fi;
  fi;
}

if test "$setuid_set" = "true"; then
  cp conf/resin.properties conf/resin.properties.orig;
  awk '/setuid_user/ { print "setuid_user : $(setuid_user)"; next; }
       /setuid_group/ { print "setuid_group : $(setuid_group)"; next; }
      { print $0; }'
      conf/resin.properties.orig > conf/resin.properties;
fi

if test "$resin_root" != "$build_dir"; then
  echo "Installing Resin root $resin_root."
  
  mkdir_safe "$resin_root" "Resin root"
  
  for subdir in "${root_subdirs_preserve}"; do
    if test -e "${resin_root}/${subdir}"; then
      echo "Preserving existing Resin root subdir ${resin_root}/${subdir}.";
    else
      echo "Installing Resin root subdir ${resin_root}/${subdir}."
      
      mkdir -p "${resin_root}/${subdir}" ||
        die "Resin root subdir creation failed. Ensure ${resin_root}/${subdir} can be created by ${USER}, or run 'make install' as root."

      if cp -r ${subdir}/* ${resin_root}/${subdir}; then
        echo "updated ${resin_root}/${subdir}"
      else
        echo "skipped ${subdir}"
      fi;
    fi;
  done;
  
  for subdir in ${root_subdirs_overwrite}; do
    if test -e "${resin_root}/${subdir}"; then
      echo "Updating existing Resin root subdir ${resin_root}/${subdir}.";
    else
      echo "Installing Resin root subdir $(resin_root)/${subdir}.";
      
      mkdir -p ${resin_root}/${subdir} ||
        die "Resin root subdir creation failed. Ensure ${resin_root}/${subdir} can be created by ${USER}, or run 'make install' as root."

    fi;
    
    cp -r ${subdir}/* ${resin_root}/${subdir};
  done;
fi

if test "$resin_home" != "$build_dir"; then
  echo "Installing Resin home ${resin_home}.";
  
  mkdir_safe "${resin_home}" "Resin home"
  
  for subdir in "${home_subdirs}"; do
    if test -e "${resin_home}/${subdir}"; then
      echo "Updating existing Resin home subdir ${resin_home}/${subdir}."
    else
      echo "Installing Resin home subdir ${resin_home}/${subdir}."
      
      mkdir_safe "${resin_home}/${subdir}" "Resin home subdir"

    fi
    
    if cp -r ${subdir}/* ${resin_home}/${subdir}; then
      echo "updated ${resin_home}/${subdir}";
    else
      echo "skipped ${subdir}";
    fi;
  done;
fi

if test -e "${resin_log}"; then
  echo "Preserving existing Resin log ${resin_log}."
else
  echo "Installing Resin log ${resin_log}."

  mkdir_safe "${resin_log}" "Resin log"
fi

if test -n "${abs_usr_share}"; then
  echo "Installing Resin home symlink ${abs_usr_share}/resin.";
  if test ! -w "${usr_share}"; then
    echo "WARNING: Resin home symlink install failed. Ensure ${usr_share} is writable by ${USER}, or run 'make install' as root.";
  else
    ln -sf "${abs_resin_home}" "${usr_share}/resin" ||
      echo "WARNING: Resin home symlink creation failed. Ensure ${usr_share} is writable by ${USER}, or run 'make install' as root."

  fi;
  echo "Installing resinctl symlink ${DESTDIR}/usr/bin/resinctl.";
  mkdir -p ${DESTDIR}/usr/bin;
  ln -sf "${abs_usr_share}/resin/bin/resinctl" "${DESTDIR}/usr/bin/resinctl" ||
    echo "WARNING: resinctl symlink creation failed. Ensure ${DESTDIR}/usr/bin is writable by ${USER}, or run 'make install' as root."

fi

if test -e "${resin_home}/bin/resinctl"; then
  echo "Setting ${resin_home}/bin/resinctl executable.";
  chmod +x ${resin_home}/bin/resinctl ||
    echo "WARNING: failed to set resinctl executable. Ensure ${resin_home}/bin/resinctl is writable by ${USER}, or run 'make install' as root."

else
  die "ABORT: ${resin_home}/bin/resinctl does not exist. Resin home install may have failed.";
fi

if test -e "${resin_home}/bin/resin.sh"; then
  echo "Setting ${resin_home}/bin/resin.sh executable.";
  chmod +x ${resin_home}/bin/resin.sh ||
    echo "WARNING: failed to set resin.sh executable. Ensure $(resin_home)/bin/resin.sh is writable by ${USER}, or run 'make install' as root."

elif test "${skip_check_resin_sh}" = "false"; then
  die "${resin_home}/bin/resin.sh does not exist. Resin home install may have failed.";
fi

if test "${resin_conf}" != "${build_dir}/conf"; then
  echo "Installing Resin conf ${resin_conf}.";

  mkdir_safe "${resin_conf}" "Resin conf"
  
  for subdir in "${conf_subdirs}"; do
    if test -e "${resin_conf}/${subdir}"; then
      echo "Preserving existing Resin conf subdir ${resin_conf}/${subdir}.";
    else
      echo "Installing Resin conf subdir ${resin_conf}/${subdir}.";

      mkdir_safe "${resin_conf}/${subdir}" "Resin conf subdir"
      
      if cp -r $$subdir/* ${resin_conf}/${subdir}; then
        echo "updated ${resin_conf}/${subdir}";
      else
        echo "skipped ${subdir}";
      fi;
    fi;
  done;
  
  for file in "${conf_files}"; do
    if test -f "$(resin_conf)/${file}"; then
      echo "Preserving existing Resin conf file ${resin_conf}/${file}.";
    else
      echo "Installing Resin conf file $(resin_conf)/${file}.";
      cp conf/${file} ${resin_conf}/${file};
    fi;
  done;
fi

if test -n "${resin_init_d}"; then
  if test -f "$(resin_init_d)"; then
    echo "Preserving existing Resin init.d script ${resin_init_d} in ${initd_dir}.";
  else
    echo "Installing Resin init.d script ${resin_init_d}.";

    mkdir_safe "${initd_dir}" "Resin init.d script"
    
    cp init.d/resin "${resin_init_d}";
    chmod +x "${resin_init_d}" ||
        echo "WARNING: failed to set Resin init.d executable. Ensure $(resin_init_d) is writable by $$USER, or run 'make install' as root.";

  fi
fi

#@ (cd modules/c/src; $(MAKE) install)
#@ if test $(resin_pro) != "." -a -r $(resin_pro)/modules/c/src/Makefile; then
#  (cd $(resin_pro)/modules/c/src; $(MAKE) install)
#fi

if test "${setuid_set}" = "true"; then
  if test -n "${group_add}"; then
    echo "Creating setuid group $(setuid_group).";
    egrep '${setuid_group}:' /etc/group 1>/dev/null 2>/dev/null
    if test $? = "0"; then
      echo "setuid group $(setuid_group) already exists.";
    else
      groupadd ${setuid_group};
      if test $? != "0"; then
        echo "WARNING: failed to create the $(setuid_group) group. The group may already exist, or run 'make install' as root.";
      fi;
    fi;
  fi;
  if test -n "${user_add}"; then
    echo "Creating setuid user $(setuid_user).";
    egrep '${setuid_user}:' /etc/passwd 1>/dev/null 2>/dev/null;
    if test $? = "0"; then
      echo "setuid user $(setuid_user) already exists.";
    else
      useradd -d /nonexistent -s /bin/false -g "$setuid_group" "$setuid_user" ||
        echo "WARNING: failed to create the $(setuid_user) user. The user may already exist, or run 'make install' as root."

    fi;
  fi;
  if test -n "${user_add}"; then
    echo "Changing the owner of Resin root $(resin_root) to $(setuid_user):$(setuid_group).";
    chown -R ${setuid_user}:${setuid_group} ${resin_root}
    if test $? != "0"; then
      echo "WARNING: failed change owner of Resin root to $(setuid_user). Ensure $(resin_root) is writable by $$USER, or run 'make install' as root.";
    fi;
  fi;
  if test -n "${group_add}"; then
    echo "Changing the owner of Resin log ${resin_log} to ${setuid_user}:${setuid_group}.";
    chown -R ${setuid_user}:${setuid_group} "${resin_log}"
    if test $? != "0"; then
      echo "WARNING: failed change owner of Resin log to ${setuid_user}. Ensure $(resin_log) is writable by $$USER, or run 'make install' as root.";
    fi;
  fi;
fi
