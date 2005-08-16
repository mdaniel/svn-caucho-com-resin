/*
 * Copyright (c) 1999-2004 Caucho Technology.  All rights reserved.
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 */

/*
 * config.c is responsible for scanning the parsed registry and grabbing
 * relevant data.
 *
 * Important data include the web-app and the servlet-mapping so any filter
 * can properly dispatch the request.
 *
 * Also, config.c needs to grab the srun and srun-backup blocks to properly
 * send the requests to the proper JVM.
 */

#ifdef WIN32
#include <winsock2.h>
#include <fcntl.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>
#include <sys/stat.h>
#include <errno.h>
#include "cse.h"

#define CACHE_SIZE 16384

#define HMUX_DISPATCH_QUERY           'q'
#define HMUX_DISPATCH_QUERY_CLUSTER   's'
#define HMUX_DISPATCH_HOST            'h'
#define HMUX_DISPATCH_WEB_APP         'a'
#define HMUX_DISPATCH_MATCH           'm'
#define HMUX_DISPATCH_IGNORE          'i'
#define HMUX_DISPATCH_ETAG            'e'
#define HMUX_DISPATCH_NO_CHANGE       'n'
#define HMUX_DISPATCH_SRUN            's'

typedef struct hash_t {
  char *host;
  int port;
  char *uri;
  resin_host_t *match_host;
  volatile int count;
  volatile int update_count;
} hash_t;

static int g_update_count;
static hash_t g_url_cache[CACHE_SIZE];

static resin_host_t *
cse_match_host_impl(config_t *config, const char *host_name,
		    int port, time_t now);

static location_t *
cse_add_unique_location(mem_pool_t *pool, web_app_t *app, char *prefix,
                        char *suffix, int is_exact, int ignore)
{
  location_t *loc;

  for (loc = app->locations; loc; loc = loc->next) {
    if (is_exact != loc->is_exact)
      continue;
    else if ((prefix == 0) != (loc->prefix == 0))
      continue;
    else if (prefix && strcmp(prefix, loc->prefix))
      continue;
    else if ((suffix == 0) != (loc->suffix == 0))
      continue;
    else if (suffix && strcmp(suffix, loc->suffix))
      continue;

    return loc;
  }

  loc = (location_t *) cse_alloc(pool, sizeof(location_t));

  memset(loc, 0, sizeof(location_t));

  loc->next = app->locations;
  app->locations = loc;

  loc->application = app;
  loc->prefix = prefix;
  loc->suffix = suffix;
  loc->is_exact = is_exact;
  loc->ignore = ignore;

  LOG(("loc %s %s %x %s\n", 
       loc->prefix ? loc->prefix : "(null)",
       loc->suffix ? loc->suffix : "(null)",
       loc->next,
       loc->ignore ? "ignore" : ""));

  return loc;
}

static web_app_t *
cse_add_web_app(mem_pool_t *pool, resin_host_t *host,
		web_app_t *applications, char *context_path)
{
  web_app_t *app;

  if (! context_path)
    context_path = "";
  else if (! strcmp(context_path, "/"))
    context_path = "";

  for (app = applications; app; app = app->next) {
    if (strcmp(context_path, app->context_path))
      continue;

    return app;
  }

  app = (web_app_t *) cse_alloc(pool, sizeof(web_app_t));

  memset(app, 0, sizeof(web_app_t));

  app->next = applications;
  applications = app;
  app->host = host;

  app->context_path = cse_strdup(pool, context_path);

  LOG(("new web-app host:%s path:%s\n", host->name, app->context_path));

  return applications;
}

/**
 * Add an application pattern to the list of recognized locations
 *
 * @param config the configuration
 * @param host the host for the pattern
 * @param prefix the web-app prefix
 *
 * @return the new application
 */
static web_app_t *
cse_add_application(mem_pool_t *pool, resin_host_t *host,
		    web_app_t *applications, char *prefix)
{
  char loc_prefix[8192];
  int i, j;

  i = 0;
  if (prefix && *prefix && *prefix != '/')
    loc_prefix[i++] = '/';
    
#ifdef WIN32
  if (prefix) {
    for (j = 0; prefix[j]; j++)
      loc_prefix[i++] = tolower(prefix[j]);
  }
#else
  if (prefix) {
    for (j = 0; prefix[j]; j++)
      loc_prefix[i++] = prefix[j];
  }
#endif
  loc_prefix[i] = 0;

  return cse_add_web_app(pool, host, applications, loc_prefix);
}

/**
 * Add a location pattern to the list of recognized locations
 *
 * @param app the containing application
 * @param pattern the url-pattern to match
 *
 * @return the new location
 */
static void
cse_add_location(mem_pool_t *pool, web_app_t *app,
		 char *pattern, char *servlet_name)
{
  char cleanPrefix[4096];
  int prefixLength;
  int cleanLength;

  char *loc_prefix = 0;
  char *loc_suffix = 0;
  int loc_is_exact = 0;

  int ignore = 0;
    
#ifdef WIN32
  if (pattern) {
    int i;
    pattern = cse_strdup(pool, pattern);
    for (i = 0; pattern[i]; i++)
      pattern[i] = tolower(pattern[i]);
  }
#endif /* WIN32 */

  cleanPrefix[0] = 0;

  if (pattern[0] && pattern[0] != '/' && pattern[0] != '*')
    strcpy(cleanPrefix, "/");

  prefixLength = strlen(cleanPrefix);
  if (prefixLength > 0 && cleanPrefix[prefixLength - 1] == '/')
    cleanPrefix[prefixLength - 1] = 0;

  if (! pattern[0]) {
    loc_prefix = cse_strdup(pool, cleanPrefix);
    loc_suffix = 0;
  }
  else if (pattern[0] == '*') {
    loc_prefix = cse_strdup(pool, cleanPrefix);
    loc_suffix = cse_strdup(pool, pattern + 1);
  }
  else {
    if (pattern[0] != '/')
      strcat(cleanPrefix, "/");
    strcat(cleanPrefix, pattern);

    cleanLength = strlen(cleanPrefix);

    if (strlen(pattern) <= 1)
      cleanPrefix[cleanLength - 1] = 0;
    else if (cleanPrefix[cleanLength - 1] != '*')
      loc_is_exact = 1;
    else if (cleanLength >= 2 && cleanPrefix[cleanLength - 2] == '/')
      cleanPrefix[cleanLength - 2] = 0;
    else if (cleanLength > 1)
      cleanPrefix[cleanLength - 1] = 0;

    loc_prefix = cse_strdup(pool, cleanPrefix);
    loc_suffix = 0;
  }

  if (servlet_name && ! strcmp(servlet_name, "plugin_ignore"))
    ignore = 1;

  cse_add_unique_location(pool, app, loc_prefix, loc_suffix,
			  loc_is_exact, ignore);
}

/**
 * Add a url-pattern to the list of matching locations.
 *
 * @param app the containing application
 * @param pattern the url-pattern to match
 *
 * @return the new location
 */
static void
cse_add_match_pattern(mem_pool_t *pool, web_app_t *app, char *pattern)
{
  cse_add_location(pool, app, pattern, "plugin_match");
}

/**
 * Adds the host.
 */
static resin_host_t *
cse_add_host_config(config_t *config, const char *host_name,
		    int port, time_t now)
{
  resin_host_t *host;

  for (host = config->hosts; host; host = host->next) {
    if (! strcmp(host_name, host->name) && host->port == port)
      return host;
  }

  host = (resin_host_t *) cse_alloc(config->p, sizeof(resin_host_t));
  memset(host, 0, sizeof(resin_host_t));
  host->config = config;
  host->canonical = host;
  host->name = cse_strdup(config->p, host_name);
  host->port = port;
  host->next = config->hosts;
  config->hosts = host;
  LOG(("cse_add_host_config %s\n", host_name));

  return host;
}

/**
 * Add a url-pattern to the list of matching locations.
 *
 * @param app the containing application
 * @param pattern the url-pattern to match
 *
 * @return the new location
 */
static void
cse_add_ignore_pattern(mem_pool_t *pool, web_app_t *app, char *pattern)
{
  cse_add_location(pool, app, pattern, "plugin_ignore");
}

/**
 * Adds a new backup to the configuration
 */
cluster_srun_t *
cse_add_host(cluster_t *cluster, const char *hostname, int port)
{
  return cse_add_cluster_server(cluster, hostname, port, "", -1, 0, 0);
}

/**
 * Adds a new backup to the configuration
 */
cluster_srun_t *
cse_add_backup(cluster_t *cluster, const char *hostname, int port)
{
  return cse_add_cluster_server(cluster, hostname, port, "", -1, 1, 0);
}

/**
 * Logging for the configuration file.
 */
void
cse_log_config(config_t *config)
{
  resin_host_t *host = config->hosts;

  for (; host; host = host->next) {
    web_app_t *app = host->applications;

    if (host != host->canonical)
      continue;

    for (; app; app = app->next) {
      location_t *loc = app->locations;
    
      for (; loc; loc = loc->next) {
	LOG(("cfg host:%s%s prefix:%s suffix:%s next:%x\n", 
	     host->name,
	     app->context_path ? app->context_path : "/",
	     loc->prefix ? loc->prefix : "null",
	     loc->suffix ? loc->suffix : "null",
	     loc->next));
      }
    }
  }
}

/**
 * Matches the host information in the config
 */
static resin_host_t *
cse_create_host(config_t *config, const char *host_name, int port)
{
  resin_host_t *host;
  resin_host_t *default_host = 0;

  for (host = config->hosts; host; host = host->next) {
    if (! strcmp(host_name, host->name) && host->port == port)
      return host;
  }

  host = (resin_host_t *) cse_alloc(config->p, sizeof(resin_host_t));
  
  memset(host, 0, sizeof(resin_host_t));
  host->config = config;
  host->canonical = host;
  host->name = cse_strdup(config->p, host_name);
  host->port = port;
  host->next = config->hosts;
  host->cluster.config = config;
  config->hosts = host;

  return host;
}

static int
read_config(stream_t *s, config_t *config, resin_host_t *host,
	    time_t now, int *p_is_change)
{
  web_app_t *web_app = 0;
  int code;
  int ch;
  char buffer[1024];
  char value[1024];
  int is_change = -1;
  mem_pool_t *pool = 0;
  cluster_t cluster;
  int live_time = -1;
  int dead_time = -1;
  char etag[sizeof(host->etag)];
  resin_host_t *old_canonical = host->canonical;
  
  memset(&cluster, 0, sizeof(cluster));
  cluster.config = config;
  cluster.round_robin_index = -1;

  strncpy(etag, host->etag, sizeof(etag));
  etag[sizeof(etag) - 1] = 0;

  host->canonical = host;
  *p_is_change = is_change;

  LOG(("hmux config %s:%d\n", host->name, host->port));
  
  while (1) {
    code = cse_read_byte(s);

    switch (code) {
    case HMUX_DISPATCH_HOST:
      if (hmux_read_string(s, buffer, sizeof(buffer)) >= 0) {
	int p;
	int port = 0;
	int ch;
	
	LOG(("hmux host %s\n", buffer));

	for (p = 0; (ch = buffer[p]); p++) {
	  if (ch == ':') {
	    port = atoi(buffer + p + 1);
	    buffer[p] = 0;
	    break;
	  }
	}
	
	if (is_change < 0)
	  is_change = 1;
	*p_is_change = is_change;

	if (strcmp(buffer, host->name) || host->port != port) {
	  resin_host_t *canonical;
	  
	  canonical = cse_create_host(config, buffer, port);
	  
	  host->canonical = canonical;
	  LOG(("hmux set canonical %s:%d -> %s:%d\n",
	       host->name, host->port,
	       buffer, port));
	}
      }
      break;
	
    case HMUX_DISPATCH_WEB_APP:
      if (! pool) {
	pool = cse_create_pool(host->config);
      }
	
      if (hmux_read_string(s, buffer, sizeof(buffer)) >= 0) {
	LOG(("hmux web-app %s\n", buffer));
	web_app = cse_add_application(pool, host, web_app, buffer);
	
	cse_add_match_pattern(pool, web_app, "/WEB-INF/*");
	cse_add_match_pattern(pool, web_app, "/META-INF/*");
      }
      break;
	
    case HMUX_DISPATCH_MATCH:
      if (hmux_read_string(s, buffer, sizeof(buffer)) > 0) {
	LOG(("hmux match %s\n", buffer));
	cse_add_match_pattern(pool, web_app, buffer);
      }
      break;
	
    case HMUX_DISPATCH_IGNORE:
      if (hmux_read_string(s, buffer, sizeof(buffer)) > 0) {
	LOG(("hmux ignore %s\n", buffer));
	cse_add_ignore_pattern(pool, web_app, buffer);
      }
      break;

    case HMUX_DISPATCH_ETAG:
      hmux_read_string(s, etag, sizeof(etag));
      LOG(("hmux etag %s\n", etag));
      break;

    case HMUX_DISPATCH_NO_CHANGE:
      host->canonical = old_canonical;
      cse_skip(s, hmux_read_len(s));
      
      LOG(("hmux no-change %s\n", host->etag));
      *p_is_change = is_change;
      break;
	
    case HMUX_HEADER:
      hmux_read_string(s, buffer, sizeof(buffer));

      ch = cse_read_byte(s);
      hmux_read_string(s, value, sizeof(value));
      LOG(("hmux header %s: %s\n", buffer, value));
      
      if (ch == HMUX_STRING) {
	if (! strcmp(buffer, "live-time"))
	  live_time = atoi(value);
	else if (! strcmp(buffer, "dead-time"))
	  dead_time = atoi(value);
	else if (! strcmp(buffer, "check-interval")) {
	  config->dependency_check_interval = atoi(value);
	  if (config->dependency_check_interval < 5)
	    config->dependency_check_interval = 5;
	  config->update_interval = atoi(value);
	}
      }
      break;
	
    case HMUX_CLUSTER:
      hmux_read_string(s, buffer, sizeof(buffer));
	
      LOG(("hmux cluster %s\n", buffer));
      break;
	
    case HMUX_SRUN:
    case HMUX_SRUN_BACKUP:
      {
	char *p;

	hmux_read_string(s, buffer, sizeof(buffer));
	
	LOG(("hmux srun %s\n", buffer));

	p = strchr(buffer, ':');
	if (p) {
	  char *host = buffer;
	  int port = 0;
	  cluster_srun_t *srun;
	    
	  *p = 0;

	  for (p++; *p; p++) {
	    if (*p >= '0' && *p <= '9')
	      port = 10 * port + *p - '0';
	  }

	  if (code == HMUX_SRUN_BACKUP)
	    srun = cse_add_backup(&cluster, host, port);
	  else
	    srun = cse_add_host(&cluster, host, port);

	  if (srun->srun && live_time > 0)
	    srun->srun->live_time = live_time;
	  if (srun->srun && dead_time > 0)
	    srun->srun->dead_time = dead_time;
	}
      }
      break;

    case HMUX_EXIT:
    case HMUX_QUIT:
      if (is_change > 0) {
	mem_pool_t *old_pool = host->pool;
	g_update_count++;
	host->applications = web_app;
	host->pool = pool;
	memcpy(&host->cluster, &cluster, sizeof(cluster));

	if (old_pool)
	  cse_free_pool(old_pool);

	strncpy(host->etag, etag, sizeof(etag));
	host->has_data = 1;
      }
      else if (pool)
	cse_free_pool(pool);

      return code == HMUX_QUIT;

    default:
      LOG(("hmux unknown %d\n", code));
      
      if (pool)
	cse_free_pool(pool);
	
      return 0;
    }
  }
}

static void
write_config(config_t *config)
{
  stream_t s;
  resin_host_t *host;
  int fd;
  char buffer[1024];

  if (! config->config_path)
    return;

  fd = creat(config->config_path, 0664);

  if (fd < 0)
    return;

  memset(&s, 0, sizeof(s));
  s.socket = fd;

  sprintf(buffer, "%d", config->update_interval);
  hmux_write_string(&s, HMUX_HEADER, "check-interval");
  hmux_write_string(&s, HMUX_STRING, buffer);

  if (config->session_cookie) {
    hmux_write_string(&s, HMUX_HEADER, "cookie");
    hmux_write_string(&s, HMUX_STRING, config->session_cookie);
  }

  if (config->session_url_prefix) {
    hmux_write_string(&s, HMUX_HEADER, "session-url-prefix");
    hmux_write_string(&s, HMUX_STRING, config->session_url_prefix);
  }

  if (config->alt_session_prefix) {
    hmux_write_string(&s, HMUX_HEADER, "alt-session-url-prefix");
    hmux_write_string(&s, HMUX_STRING, config->alt_session_prefix);
  }

  for (host = config->hosts; host; host = host->next) {
    web_app_t *web_app;
    int i;
    
    hmux_write_string(&s, HMUX_DISPATCH_HOST, host->name);

    if (host->canonical && host->canonical != host) {
      if (host->canonical->port) {
	sprintf(buffer, "%s:%d", host->canonical->name, host->canonical->port);
	hmux_write_string(&s, HMUX_DISPATCH_HOST, buffer);
      }
      else
	hmux_write_string(&s, HMUX_DISPATCH_HOST, host->canonical->name);
    }

    for (web_app = host->applications; web_app; web_app = web_app->next) {
      location_t *loc;
      hmux_write_string(&s, HMUX_DISPATCH_WEB_APP, web_app->context_path);

      for (loc = web_app->locations; loc; loc = loc->next) {
	int code = loc->ignore ? HMUX_DISPATCH_IGNORE : HMUX_DISPATCH_MATCH;

	if (loc->is_exact)
	  hmux_write_string(&s, code, loc->prefix);
	else if (loc->suffix) {
	  sprintf(buffer, "*%s", loc->suffix);
	  
	  hmux_write_string(&s, code, buffer);
	}
	else if (loc->prefix) {
	  sprintf(buffer, "%s/*", loc->prefix);
	  
	  hmux_write_string(&s, code, buffer);
	}
      }
    }

    cse_write_string(&s, HMUX_CLUSTER, "");
    
    for (i = 0; i < host->cluster.srun_size; i++) {
      cluster_srun_t *srun;
      int code;

      srun = &host->cluster.srun_list[i];

      if (! srun || ! srun->srun || ! srun->srun->hostname)
	continue;

      sprintf(buffer, "%d", srun->srun->live_time);
      hmux_write_string(&s, HMUX_HEADER, "live-time");
      hmux_write_string(&s, HMUX_STRING, buffer);

      sprintf(buffer, "%d", srun->srun->dead_time);
      hmux_write_string(&s, HMUX_HEADER, "dead-time");
      hmux_write_string(&s, HMUX_STRING, buffer);

      code = srun->is_backup ? HMUX_SRUN_BACKUP : HMUX_SRUN;

      if (srun->srun->port) {
	sprintf(buffer, "%s:%d", srun->srun->hostname, srun->srun->port);
	
	hmux_write_string(&s, code, buffer);
      }
      else {
	hmux_write_string(&s, code, srun->srun->hostname);
      }
    }

    cse_write_string(&s, HMUX_DISPATCH_ETAG, host->etag);
    
    cse_write_byte(&s, HMUX_QUIT);
  }
  cse_write_byte(&s, HMUX_EXIT);

  cse_flush(&s);

  close(fd);
}

static int
read_all_config_impl(config_t *config)
{
  stream_t s;
  resin_host_t *host;
  int fd;
  char buffer[1024];
  char value[1024];
  int code;
  int  ch;
  time_t now = 0;
  int is_change = 1;

  if (! config->config_path)
    return 0;
  
  fd = open(config->config_path, O_RDONLY);

  if (fd < 0)
    return 0;

  memset(&s, 0, sizeof(s));
  s.socket = fd;

  while ((code = cse_read_byte(&s)) >= 0) {
    switch (code) {
    case HMUX_DISPATCH_HOST:
      {
	int p;
	int port = 0;
	int ch;
	
	hmux_read_string(&s, buffer, sizeof(buffer));
	LOG(("hmux host %s\n", buffer));

	for (p = 0; (ch = buffer[p]); p++) {
	  if (ch == ':') {
	    port = atoi(buffer + p + 1);
	    buffer[p] = 0;
	    break;
	  }
	}
      
	host = cse_create_host(config, buffer, port);
	read_config(&s, config, host, 0, &is_change);
      }
      break;

    case HMUX_HEADER:
      hmux_read_string(&s, buffer, sizeof(buffer));

      ch = cse_read_byte(&s);
      hmux_read_string(&s, value, sizeof(value));
      if (ch == HMUX_STRING) {
	LOG(("hmux header %s: %s\n", buffer, value));
      }
      break;

    default:
      hmux_read_string(&s, value, sizeof(value));
      LOG(("hmux value %c: %s\n", code, buffer));
      break;
    }
  }

  close(fd);

  return 1;
}

static void
read_all_config(config_t *config)
{
  if (! read_all_config_impl(config)) {
    /* match all to ensure will not show source if can't connect. */
    resin_host_t *host = 0;
    mem_pool_t *pool = 0;
    web_app_t *web_app = 0;
    time_t now = 0;
    
    host = cse_match_host_impl(config, "", 0, now);
    pool = cse_create_pool(config);
    
    web_app = cse_add_application(pool, host, 0, "");
    host->applications = web_app;
    host->pool = pool;
	
    cse_add_match_pattern(pool, web_app, "/*");
  }
}

static int
cse_update_host_from_resin(resin_host_t *host, time_t now)
{
  stream_t s;
  char *uri = "";

  if (host->has_data)
    host->last_update = now;
    
  if (cse_open_any_connection(&s, &host->config->config_cluster, now)) {
    int code;
    int len;
    int is_change;
    
    hmux_start_channel(&s, 1);
    hmux_write_int(&s, HMUX_PROTOCOL, HMUX_DISPATCH_PROTOCOL);
    if (host->etag[0])
      hmux_write_string(&s, HMUX_DISPATCH_ETAG, host->etag);
    hmux_write_string(&s, HMUX_DISPATCH_HOST, host->name);
    hmux_write_string(&s, HMUX_DISPATCH_QUERY, uri);
    hmux_write_quit(&s);

    code = cse_read_byte(&s);
    if (code != HMUX_CHANNEL) {
      cse_close(&s, "protocol");
      return 0;
    }
    
    len = hmux_read_len(&s);

    if (read_config(&s, host->config, host, now, &is_change))
      cse_recycle(&s, time(0));
    else
      cse_close(&s, "close");

    if (is_change > 0)
      write_config(host->config);

    return 1;
  }
  else {
    LOG(("can't open any connections\n"));
  }

  return 0;
}

/**
 * Initialize the configuration.
 */
void
cse_init_config(config_t *config)
{
  LOG(("initializing\n"));

  if (! config->p)
    config->p = cse_create_pool(config);

  /*
  // XXX: need to free these now.
  */
  config->hosts = 0;
  config->error = 0;

  /*
  for (i = 0; i < CACHE_SIZE; i++) {
    if (g_url_cache[i].uri) {
      free(g_url_cache[i].uri);
      free(g_url_cache[i].host);
    }
    g_url_cache[i].uri = 0;
  }
  */
  
  config->update_count++;

  /*
  config->enable_caucho_status = 0;
  */
  config->disable_session_failover = 0;
  config->update_interval = 15;
  config->session_url_prefix = ";jsessionid=";
  config->session_cookie = "JSESSIONID";

  config->config_cluster.config = config;
  
#ifdef WIN32  
  config->work_dir = "/temp";
  mkdir("/temp");
#else
  config->work_dir = "/tmp";
#endif

  /*
  cse_add_host(&config->config_cluster, "localhost", 6802);

  cse_update_from_resin(config);
  */

  if (! config->lock) {
    config->lock = cse_create_lock(config);
    LOG(("config lock %p\n", config->lock));
    config->config_lock = cse_create_lock(config);
  }

  read_all_config(config);
}

void
cse_add_config_server(config_t *config, const char *host, int port)
{
  char buffer[1024];
  
  cse_add_host(&config->config_cluster, host, port);

  if (! config->config_path && config->work_dir) {
    sprintf(buffer, "%s/%s_%d", config->work_dir, host, port);
    
    config->config_path = strdup(buffer);

    read_all_config(config);
  }
}

/**
 * Matches the host information in the config
 */
static resin_host_t *
cse_update_host(config_t *config, resin_host_t *host, time_t now)
{
  const char *host_name = host->name;
  int port = host->port;
  
  if (now < host->last_update + config->update_interval)
    return;

  cse_update_host_from_resin(host, now);
}

/**
 * Matches the host information in the config
 */
static resin_host_t *
cse_match_host_impl(config_t *config, const char *host_name,
		    int port, time_t now)
{
  resin_host_t *host = cse_create_host(config, host_name, port);

  cse_update_host(config, host, now);

  if (host != host->canonical)
    cse_update_host(config, host->canonical, now);

  return host->canonical;
}

/**
 * Matches the host information in the config
 */
resin_host_t *
cse_match_host(config_t *config, const char *host_name, int port, time_t now)
{
  resin_host_t *host;

  if (config)
    cse_lock(config->config_lock);

  host = cse_match_host_impl(config, host_name, port, now);
  
  if (config)
    cse_unlock(config->config_lock);
  
  return host;
}

/**
 * tests if 'full' starts with 'part'
 *
 * If it's not an exact match, the next character of 'full' must be '/'.
 * That way, a servlet mapping of '/foo/ *' will match /foo, /foo/bar, but
 * not /foolish.
 */
static int
cse_starts_with(const char *full, const char *part)
{
  char ch1, ch2;

  while ((ch2 = *part++) && (ch1 = *full++) && ch1 == ch2) {
  }

  if (ch2)
    return 0;
  else if (! *full)
    return 1;
  else if (*full == '/')
    return 1;
  
#ifdef WIN32
  /* special case so web-inf. will match */
  if (full[0] != '.')
    return 0;
  else if (full[1] == 0 || full[1] == '/' || full[1] == '\\')
    return 1;
  else if (full[1] != '.')
    return 0;
  else if (full[2] == 0 || full[2] == '/' || full[2] == '\\')
    return 1;
#endif
  
  return 0;
}

static int
cse_match_suffix(const char *full, const char *suffix)
{
  int len = strlen(suffix);

  do {
    char *match = strstr(full, suffix);
    
    if (! match)
      return 0;
    
    if (! match[len] || match[len] == '/')
      return 1;
#ifdef WIN32
    {
      char ch;
      if ((ch = match[len]) == '.' || ch == ' ') {
	for (; (ch = match[len]) == '.' || ch == ' '; len++) {
	}

	if (! match[len] || match[len] == '/')
	  return 1;
      }
    }
#endif

    full = match + len;
  } while (*full);

  return 0;
}

static int
hex_to_digit(int hex)
{
  if (hex >= '0' && hex <= '9')
    return hex - '0';
  else if (hex >= 'a' && hex <= 'f')
    return hex - 'a' + 10;
  else if (hex >= 'A' && hex <= 'F')
    return hex - 'A' + 10;
  else
    return 0;
}

/**
 * Normalizes the URI, unescaping the HTTP URL encodings.
 */
static void
normalize_uri(config_t *config, const char *raw_uri,
	      char *uri, int len, int unescape)
{
  int i, k;
  int ch;
  int test_ch = config->session_url_prefix[0];
  
  k = 0;
  for (i = 0; (ch = raw_uri[i]) && i + 1 < len; i++) {
    /* strip the session_url_prefix */
    if (ch == test_ch && ! strncmp(raw_uri + i, config->session_url_prefix,
                                   config->session_url_prefix_length)) {
      break;
    }
    
    if (ch == '%' && unescape) {
      int h1 = raw_uri[i + 1];

      if (h1 == 'u') {
        ch = hex_to_digit(raw_uri[i + 2]);
        ch = 16 * ch + hex_to_digit(raw_uri[i + 3]);
        ch = 16 * ch + hex_to_digit(raw_uri[i + 4]);
        ch = 16 * ch + hex_to_digit(raw_uri[i + 5]);

        i += 4;
      }
      else {
        ch = hex_to_digit(h1);
        ch = 16 * ch + hex_to_digit(raw_uri[i + 2]);

        i += 2;
        
        if ((ch & 0xf0) == 0xc0 && raw_uri[i + 1] == '%' && i + 3 < len) {
          int ch2 = hex_to_digit(raw_uri[i + 2]);
          ch2 = 16 * ch2 + hex_to_digit(raw_uri[i + 3]);

          if (ch2 >= 0x80) {
            ch = ((ch & 0x3f) << 6) + (ch2 & 0x3f);
            i += 3;
          }
        }
      }
    }
    else if (ch == ':')
      break;
      
    if (ch == '/' && k > 0 && (uri[k - 1] == '/' || uri[k - 1] == '\\'))
      continue;

#ifdef WIN32
    if (ch >= 0 && isupper(ch))
      uri[k++] = tolower(ch);
    else if (ch >= 0)
      uri[k++] = ch;
#else
    if (ch >= 0)
      uri[k++] = ch;
#endif
  }
  
  uri[k] = 0;
}

static resin_host_t *
cse_is_match(config_t *config,
             const char *raw_host, int port, const char *raw_uri,
             int unescape, time_t now)
{
  char uri[16 * 1024];
  char host_name[1024];
  char *suburi;
  int len = sizeof(uri) - 1;
  int host_len = sizeof(host_name) - 1;
  web_app_t *app_ptr;
  web_app_t *app;
  int has_host;
  unsigned int best_len;
  location_t *loc;
  int i;
  int ch;
  resin_host_t *host;
  int is_match = 0;

  normalize_uri(config, raw_uri, uri, len, unescape);

  for (i = 0; raw_host && (ch = raw_host[i]) && i + 1 < host_len; i++) {
    if (isupper(ch))
      host_name[i] = tolower(ch);
    else
      host_name[i] = ch;
  }

  host_name[i] = 0;

  host = cse_match_host(config, host_name, port, now);

  if (! host)
    return 0;

  host = host->canonical;

  has_host = 0;
  best_len = 0;
  app = 0;

  for (app_ptr = host->applications; app_ptr; app_ptr = app_ptr->next) {
    /**
     * The uri prefix must match.
     */
    if (! cse_starts_with(uri, app_ptr->context_path))
      continue;

    if (strlen(app_ptr->context_path) < best_len)
      continue;

    LOG(("app-match host:%s%s with host:%s uri:%s\n",
         host->name,
         app_ptr->context_path ? app_ptr->context_path : "",
         host_name, uri));

    best_len = strlen(app_ptr->context_path);
    app = app_ptr;
  }

  if (! app)
    return 0;

  suburi = uri + best_len;

  is_match = 0;
  for (loc = app->locations; loc; loc = loc->next) {
    LOG(("match host:%s%s prefix:%s suffix:%s with host:%s uri:%s next:%x ignore:%d exact:%d\n", 
         host->name,
         app->context_path ? app->context_path : "null",
         loc->prefix ? loc->prefix : "null",
         loc->suffix ? loc->suffix : "null",
         host_name, uri, loc->next, loc->ignore, loc->is_exact));

    if (loc->is_exact && ! strcmp(suburi, loc->prefix)) {
    }
    
    else if (loc->is_exact)
      continue;
      
    else if (! cse_starts_with(suburi, loc->prefix))
      continue;
    
    else if (loc->suffix && ! cse_match_suffix(suburi, loc->suffix))
      continue;

    if (loc->ignore)
      is_match = -1;
    else if (! is_match)
      is_match = 1;
  }

  if (strstr(suburi, "/j_security_check"))
    return host;
  
  return is_match > 0 ? host : 0;
}


/**
 * Tests if the request matches a Resin URL.
 *
 * @param config the plugin configuration structure
 * @param host the request's host
 * @param port the request's port
 * @param url the request's uri
 */

resin_host_t *
cse_match_request(config_t *config, const char *host, int port,
                  const char *uri, int unescape, time_t now)
{
  int hash = port;
  int i;
  
  hash_t *entry;
  char *test_uri;
  char *test_host;
  int test_port;
  int test_count;
  resin_host_t *test_match_host;
  
  resin_host_t *match_host;

  if (! host)
    host = "";
  if (! uri)
    uri = "";
  
  for (i = 0; host[i]; i++)
    hash = 65531 * hash + host[i];

  for (i = 0; uri[i]; i++)
    hash = 65531 * hash + uri[i];

  if (hash < 0)
    hash = -hash;
  hash = hash % CACHE_SIZE;

  entry = &g_url_cache[hash];

  test_count = entry->count;
  test_uri = entry->uri;
  test_host = entry->host;
  test_port = entry->port;
  test_match_host = entry->match_host;

  cse_lock(config->lock);
  if (g_update_count != entry->update_count) {
  }
  else if (test_count != entry->count) {
  }
  else if (! test_uri || strcmp(test_uri, uri)) {
  }
  else if (! test_host || strcmp(test_host, host)) {
  }
  else if (test_port && test_port != port) {
  }
  else if (! test_match_host &&
	   config->last_update + config->update_interval < now) {
  }
  else if (test_match_host &&
	   test_match_host->last_update + config->update_interval < now) {
  }
  else {
    cse_unlock(config->lock);
    
    return test_match_host;
  }
  cse_unlock(config->lock);

  match_host = cse_is_match(config, host, port, uri, unescape, now);

  cse_lock(config->lock);
  entry->update_count = g_update_count;
  entry->count++;
  entry->match_host = match_host;
  if (entry->uri) {
    free(entry->host);
    free(entry->uri);
    
    entry->uri = 0;
    entry->host = 0;
  }

  LOG(("cse_match_request entry %s %s match:%s\n", host, uri, (match_host != 0) ? "yes" : "no"));
  
  entry->host = strdup(host ? host : "");
  entry->uri = strdup(uri);
  entry->port = port;
  entry->count++;
  config->last_update = now;
  cse_unlock(config->lock);

  return match_host;
}
