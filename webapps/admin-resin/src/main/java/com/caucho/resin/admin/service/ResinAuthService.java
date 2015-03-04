package com.caucho.resin.admin.service;

import javax.annotation.PostConstruct;

import io.baratine.channel.ChannelContext;
import io.baratine.channel.ChannelScoped;
import io.baratine.core.Services;
import com.caucho.baratine.Remote;
import io.baratine.core.Service;

@Remote
@ChannelScoped
@Service("session:///auth")
public class ResinAuthService
{
  private ChannelContext _ctx;

  @PostConstruct
  public void init()
  {
    System.err.println(getClass().getSimpleName() + ".init0: " + hashCode());

    _ctx = Services.getCurrentSystem(ChannelContext.class);

    System.err.println(getClass().getSimpleName() + ".init1: " + _ctx);
  }

  public boolean login(String user, String passHash)
  {
    System.err.println(getClass().getSimpleName() + ".login0: " + user + " . " + passHash + " . " + hashCode());

    try {
      _ctx.login(user, new String[] {});

    }
    catch (Exception e) {
      e.printStackTrace();
    }

    System.err.println(getClass().getSimpleName() + ".login1");

    return true;
  }
}
