/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 *
 * @author Scott Ferguson
 */

#ifndef WIN32
#include "ap_config_auto.h"
#endif
#include "httpd.h"
#include "http_config.h"
#include "http_request.h"
#include "http_core.h"
#include "http_protocol.h"
#include "http_log.h"
#include "http_main.h"
#include "util_script.h"
#include <stdlib.h>
#include <errno.h>

#if !defined(EAPI) && (MODULE_MAGIC_COOKIE == MODULE_MAGIC_COOKIE_EAPI)
#define EAPI 1
#endif

#include "../common/cse.h"
#include "../common/version.h"

/*
 * Apache magic module declaration.
 */
module MODULE_VAR_EXPORT caucho_module;

#define DEFAULT_PORT 6802

static char *g_error_page = 0;
static config_t *g_config = 0;
static pool *g_pool = 0;
static char session_decode[128];

void
cse_log(char *fmt, ...)
{
#ifdef DEBUG
  va_list args;
  FILE *file = fopen("/tmp/log", "a+");

  if (file) {  
    time_t now = time(0);
    struct tm *now_tm = localtime(&now);
    char date[80];
    
    strftime(date, sizeof(date), "%H:%M:%S", now_tm);
    fprintf(file, "%d:[%s] ", getpid(), date);

    va_start(args, fmt);
    vfprintf(file, fmt, args);
    va_end(args);
    fclose(file);
  }
#endif
}

void *
cse_create_lock(config_t *config)
{
#ifdef WIN32
	return CreateMutex(0, 0, 0);
#else
    return 0;
#endif
}

void
cse_free_lock(config_t *config, void *vlock)
{
}

int
cse_lock(void *lock)
{
#ifdef WIN32
  if (lock) {
    WaitForSingleObject(lock, INFINITE);
  }
  return 1;
#else
  return 1;
#endif
}

void
cse_unlock(void *lock)
{
#ifdef WIN32
	if (lock)
		ReleaseMutex(lock);
#endif
}

void
cse_error(config_t *config, char *format, ...)
{
  char buf[BUF_LENGTH];
  va_list args;

  va_start(args, format);
  vsprintf(buf, format, args);
  va_end(args);

  LOG(("ERROR: %s\n", buf));

  config->error = cse_strdup(config->p, buf);
}

void
cse_set_socket_cleanup(int socket, void *pool)
{
  LOG(("set cleanup %d\n", socket));

  if (socket > 0 && pool)
    ap_note_cleanups_for_socket(pool, socket);
}

void
cse_kill_socket_cleanup(int socket, void *pool)
{
  LOG(("kill cleanup %d\n", socket));

  if (socket > 0 && pool)
    ap_kill_cleanups_for_socket(pool, socket);
}

void *
cse_malloc(int size)
{
  return malloc(size);
}

void cse_free(void *data) {}

static void
cse_module_init(struct server_rec *server, struct pool *pool)
{
  ap_add_version_component(VERSION);
}

static void *
cse_create_server_config(pool *p, server_rec *server)
{
  config_t *config;

  if (! g_pool)
    g_pool = ap_make_sub_pool(p);

  config = (config_t *) ap_pcalloc(g_pool, sizeof(config_t));
  memset(config, 0, sizeof(config_t));

  config->web_pool = g_pool;
  cse_init_config(config);

  g_config = config;
  
  return (void *) config;
}

static void *
cse_create_dir_config(pool *p, char *dir)
{
  config_t *config;

  if (! g_pool)
    g_pool = ap_make_sub_pool(p);

  config = (config_t *) ap_pcalloc(g_pool, sizeof(config_t));
  memset(config, 0, sizeof(config_t));

  config->web_pool = g_pool;
  cse_init_config(config);

  if (! g_config)
    g_config = config;
  
  return (void *) config;
}

/**
 * Retrieves the caucho configuration from Apache
 */
static config_t *
cse_get_module_config(request_rec *r)
{
  config_t *config = 0;

  if (r->per_dir_config)
    config = (config_t *) ap_get_module_config(r->per_dir_config, &caucho_module);
  if (config)
    return config;

  if (r->server->module_config)
    config = (config_t *) ap_get_module_config(r->server->module_config,
                                               &caucho_module);

  if (config)
    return config;
  else
    return g_config;
}

static config_t *
cse_get_cmd_config(cmd_parms *cmd)
{
  config_t *config = (config_t *) ap_get_module_config(cmd->context, &caucho_module);

  if (config)
    return config;
  
  config = (config_t *) ap_get_module_config(cmd->server->module_config,
                                             &caucho_module);
  if (config)
    return config;
  else
    return g_config;
}

/**
 * Parse the ResinConfigServer configuration in the apache config file.
 */
static const char *
cse_config_server_command(cmd_parms *cmd, void *mconfig,
			  char *host_arg, char *port_arg)
{
  config_t *config = cse_get_cmd_config(cmd);
  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;

  /*
  cse_add_host(&config->config_cluster, host_arg, port);
  */
  cse_add_config_server(config->p, config, host_arg, port);

  return 0;
}

/**
 * Parse the CauchoStatus configuration in the apache config file.
 */
static const char *
cse_caucho_status_command(cmd_parms *cmd, void *pconfig, char *value)
{
  config_t *config = cse_get_cmd_config(cmd);
  
  if (! config)
    return 0;  

  if (value == 0 || ! strcmp(value, "true") || ! strcmp(value, "yes"))
    config->enable_caucho_status = 1;
  else
    config->enable_caucho_status = 0;

  return 0;
}

/**
 * Parse the CauchoConfigHost configuration in the apache config file.
 */
static const char *
cse_config_file_command(cmd_parms *cmd, void *mconfig, char *value)
{
  return "CauchoConfigFile has been replaced by ResinConfigServer.\n";
}

/**
 * Parse the server root.
 */
static const char *
cse_config_server_root(cmd_parms *cmd, void *mconfig, char *value)
{
  config_t *config = cse_get_cmd_config(cmd);
 
  config->resin_home = ap_server_root_relative(cmd->pool, value);

  return 0;
}

/**
 * Parse the CauchoHosts configuration in the apache config file.
 */
static const char *
cse_host_command(cmd_parms *cmd, void *mconfig, char *host_arg, char *port_arg)
{
  config_t *config = cse_get_cmd_config(cmd);
  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;
  resin_host_t *host;

  host = config->manual_host;
  if (! host) {
    host = cse_alloc(config->p, sizeof(resin_host_t));
    memset(host, 0, sizeof(resin_host_t));
    
    host->name = "manual";
    host->canonical = host;

    host->config = config;
    
    host->cluster.config = config;
    host->cluster.round_robin_index = -1;
    
    config->manual_host = host;
  }

  cse_add_host(config->p, &host->cluster, host_arg, port);

  return 0;
}

/**
 * Parse the CauchoBackup configuration in the apache config file.
 */
static const char *
cse_backup_command(cmd_parms *cmd, void *mconfig,
		   char *host_arg, char *port_arg)
{
  config_t *config = cse_get_cmd_config(cmd);
  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;
  resin_host_t *host;

  host = config->manual_host;
  if (! host) {
    host = cse_alloc(config->p, sizeof(resin_host_t));
    memset(host, 0, sizeof(resin_host_t));
    
    host->name = "manual";
    host->canonical = host;

    host->config = config;
    
    host->cluster.config = config;
    host->cluster.round_robin_index = -1;
    
    config->manual_host = host;
  }

  cse_add_backup(config->p, &host->cluster, host_arg, port);

  return 0;
}

/**
 * Parse the CauchoKeepalive configuration in the apache config file.
 */
static const char *
cse_keepalive_command(cmd_parms *cmd, void *mconfig, int flag)
{
  /* g_srun_keepalive = flag; */

  return 0;
}

/**
 * Parse the CauchoHosts configuration in the apache config file.
 */
static const char *
cse_error_page_command(cmd_parms *cmd, void *mconfig, char *error_page_arg)
{
  g_error_page = error_page_arg;

  return 0;
}

/**
 * Look at the request to see if Caucho should handle it.
 */
static int
cse_clean_jsessionid(request_rec *r)
{
  config_t *config = cse_get_module_config(r);
  char *uri = (char *) r->uri;
  char *new_uri;

  if (config == NULL)
    return DECLINED;

  /*
  cse_update_config(config, r->request_time);
  */

  if (*config->alt_session_url_prefix) {
    if (! strncmp(uri + 1, config->alt_session_url_prefix,
                  sizeof(config->alt_session_url_prefix))) {
      char *p = strchr(uri + 1, '/');

      if (r->request_config)
        ap_set_module_config(r->request_config, &caucho_module, uri);

      r->uri = p;

      /* strip session encoding from file */
      if (r->filename) {
        char *prefix = strstr(r->filename, config->alt_session_url_prefix);
        p = prefix ? strchr(prefix, '/') : 0;
        
        if (prefix && p) {
          memcpy(prefix, p, strlen(p));

          /* restat the new filename */
          if (stat(r->filename, &r->finfo) < 0)
            r->finfo.st_mode = 0;
        }
      }
    }
  }
  else {
    new_uri = strstr(uri, config->session_url_prefix);
    if (new_uri) {
      if (r->request_config) {
        ap_set_module_config(r->request_config, &caucho_module,
                             new_uri);
      }
    
      *new_uri = 0;
  
      /* Strip session encoding from static files. */
      if (r->filename) {
        char *url_rewrite = strstr(r->filename, config->session_url_prefix);
    
        if (url_rewrite) {
          *url_rewrite = 0;

          if (stat(r->filename, &r->finfo) < 0) {
            r->finfo.st_mode = 0;
          }
        }
      }
    }
  }

  return DECLINED;
}

/**
 * Look at the request to see if Caucho should handle it.
 */
static int
cse_dispatch(request_rec *r)
{
  config_t *config = cse_get_module_config(r);
  const char *host = ap_get_server_name(r);
  const char *uri = r->uri;
  unsigned int now = r->request_time;

  LOG(("CONF: %p\n", config));
  
  if (config == NULL)
    return DECLINED;

  /*
  cse_update_config(config, r->request_time);
  */
  
  if (config->enable_caucho_status && strstr(uri, "/caucho-status")) {
    r->handler = "caucho-status";
    return OK;
  }

  /* Check for exact virtual host match */
  if (cse_match_request(config, host, ap_get_server_port(r), uri, 0, now) ||
      r->handler && ! strcmp(r->handler, "caucho-request")) {
    r->handler = "caucho-request";
    
    LOG(("[%d] match %s:%s\n", getpid(), host ? host : "null", uri));
    return OK;
  }

  LOG(("[%d] mismatch %s:%s\n", getpid(), host ? host : "null", uri));

  return DECLINED;
}

/**
 * Gets the session index from the request
 *
 * Cookies have priority over the query
 *
 * @return -1 if no session
 */
static int
get_session_index(config_t *config, request_rec *r, int *backup)
{
  array_header *hdrs_arr = ap_table_elts(r->headers_in);
  table_entry *hdrs = (table_entry *) hdrs_arr->elts;
  int i;
  int session;
  char *uri;

  for (i = 0; i < hdrs_arr->nelts; ++i) {
    if (! hdrs[i].key || ! hdrs[i].val)
      continue;

    if (strcasecmp(hdrs[i].key, "Cookie"))
      continue;

    session = cse_session_from_string(hdrs[i].val,
                                      config->session_cookie,
                                      backup);
    if (session >= 0)
      return session;
  }

  if (r->request_config)
    uri = ap_get_module_config(r->request_config, &caucho_module);

  if (uri) {
    if (*config->alt_session_url_prefix)
      return cse_session_from_string(uri,
				     config->alt_session_url_prefix,
				     backup);
    else
      return cse_session_from_string(uri + strlen(config->session_url_prefix),
				     "", backup);
  }

  if (*config->alt_session_url_prefix) {
    return cse_session_from_string(r->uri,
				   config->alt_session_url_prefix,
				   backup);
  }
  else
    return cse_session_from_string(r->uri, config->session_url_prefix, backup);
}

/**
 * Writes request parameters to srun.
 */
static void
write_env(stream_t *s, request_rec *r, char *session_id)
{
  char buf[4096];
  int ch;
  int i;
  
  conn_rec *c = r->connection;
  const char *host;
  int port;
  int is_sub_request = 1;
  char *uri;

  /*
   * is_sub_request is always true, since we can't detect mod_rewrite
   * and mod_rewrite doesn't change the unparsed_uri.
   */
  if (is_sub_request)
    uri = r->uri; /* for mod_rewrite */
  else
    uri = r->unparsed_uri; /* #937 */
  
  for (i = 0; (ch = uri[i]) && ch != '?' && i + 1 < sizeof(buf); i++) 
    buf[i] = ch;
  if (session_id) {
    buf[i++] = *s->config->session_url_prefix;
    for (session_id++; *session_id && i + 1 < sizeof(buf); i++)
      buf[i] = *session_id++;
  }
  buf[i] = 0;
  cse_write_string(s, HMUX_URL, buf);

  cse_write_string(s, HMUX_METHOD, r->method);
  if (*s->config->alt_session_url_prefix && r->request_config) {
    char *suburi = ap_get_module_config(r->request_config, &caucho_module);

    if (suburi)
      uri = suburi;
  }
  cse_write_string(s, CSE_PROTOCOL, r->protocol);

  if (r->args)
    cse_write_string(s, CSE_QUERY_STRING, r->args);

  /* Gets the server name */
  host = ap_get_server_name(r);
  port = ap_get_server_port(r);

  cse_write_string(s, HMUX_SERVER_NAME, host);
  cse_write_string(s, CSE_SERVER_PORT, ap_psprintf(r->pool, "%u", port));

  host = ap_get_remote_host(c, r->per_dir_config, REMOTE_HOST);
  if (host)
    cse_write_string(s, CSE_REMOTE_HOST, host);
  else
    cse_write_string(s, CSE_REMOTE_HOST, c->remote_ip);

  cse_write_string(s, CSE_REMOTE_ADDR, c->remote_ip);
  cse_write_string(s, CSE_REMOTE_PORT,
		   ap_psprintf(r->pool, "%d", ntohs(c->remote_addr.sin_port)));
  
  if (c->user)
    cse_write_string(s, CSE_REMOTE_USER, c->user);
  if (c->ap_auth_type)
    cse_write_string(s, CSE_AUTH_TYPE, c->ap_auth_type);
}

static void
write_ssl_env(stream_t *s, request_rec *r)
{
  /* mod_ssl */
#ifdef EAPI
  {
    static char *vars[] = { "SSL_CLIENT_S_DN",
                            "SSL_CIPHER",
                            "SSL_CIPHER_EXPORT",
                            "SSL_PROTOCOL",
                            "SSL_CIPHER_USEKEYSIZE",
                            "SSL_CIPHER_ALGKEYSIZE",
                            0};
    char *var;
    int i;
    int v;
    
    if ((v = ap_hook_call("ap::mod_ssl::var_lookup", &var, r->pool, r->server,
                          r->connection, r, "SSL_CLIENT_CERT"))) {
      cse_write_string(s, CSE_CLIENT_CERT, var);
    }
    else if ((v = ap_hook_call("ap::mod_ssl::var_lookup", &var, r->pool, r->server,
                          r->connection, r, "SSL_CLIENT_CERTIFICATE"))) {
      cse_write_string(s, CSE_CLIENT_CERT, var);
    }

    for (i = 0; vars[i]; i++) {
      if ((v = ap_hook_call("ap::mod_ssl::var_lookup", &var,
                            r->pool, r->server, r->connection, r, vars[i]))) {
        cse_write_string(s, HMUX_HEADER, vars[i]);
        cse_write_string(s, HMUX_STRING, var);
      }
    }
  }
#endif  
}

/**
 * Writes headers to srun.
 */
static void
write_headers(stream_t *s, request_rec *r)
{
  array_header *hdrs_arr = ap_table_elts(r->headers_in);
  table_entry *hdrs = (table_entry *) hdrs_arr->elts;
  int i;

  for (i = 0; i < hdrs_arr->nelts; ++i) {
    char *key = hdrs[i].key;
    char *value = hdrs[i].val;
    
    if (! key)
      continue;

    /*
     * Content-type and Content-Length are special cased for a little
     * added efficiency.
     */
    if (! strcasecmp(key, "Content-type"))
      cse_write_string(s, CSE_CONTENT_TYPE, value);
    else if (! strcasecmp(key, "Content-length"))
      cse_write_string(s, CSE_CONTENT_LENGTH, value);
    else {
      cse_write_string(s, HMUX_HEADER, key);
      cse_write_string(s, HMUX_STRING, value);
    }
  }
}

static void
write_added_headers(stream_t *s, request_rec *r)
{
  array_header *hdrs_arr = ap_table_elts(r->subprocess_env);
  table_entry *hdrs = (table_entry *) hdrs_arr->elts;
  int i;
  int has_ssl = 0;

  for (i = 0; i < hdrs_arr->nelts; ++i) {
    char *key = hdrs[i].key;
    char *value = hdrs[i].val;
    
    if (! key)
      continue;

    /* skip leading whitespace */
    for (; isspace(*value); value++) {
    }

    if (! strcmp(key, "HTTPS") &&
	! strcasecmp(value, "on")) {
      has_ssl = 1;
      cse_write_string(s, CSE_IS_SECURE, "");
    }
    else if (*key == 'S' && ! r->connection->user &&
             ! strcmp(key, "SSL_CLIENT_DN"))
      cse_write_string(s, CSE_REMOTE_USER, value);

    cse_write_string(s, HMUX_HEADER, key);
    cse_write_string(s, HMUX_STRING, value);
  }

  if (has_ssl)
    write_ssl_env(s, r);

  if (r->prev) {
    if (r->prev->args) {
      cse_write_string(s, HMUX_HEADER, "REDIRECT_QUERY_STRING");
      cse_write_string(s, HMUX_STRING, r->prev->args);
    }
    
    if (r->prev->uri) {
      cse_write_string(s, HMUX_HEADER, "REDIRECT_URL");
      cse_write_string(s, HMUX_STRING, r->prev->uri);
    }
  }
}

/**
 * Writes a response from srun to the client
 */
static int
cse_write_response(stream_t *s, int len, request_rec *r)
{
  while (len > 0) {
    int writelen;
    int sentlen;

    if (s->read_length <= s->read_offset && cse_fill_buffer(s) < 0)
      return -1;

    writelen = s->read_length - s->read_offset;
    if (len < writelen)
      writelen = len;

    while (writelen > 0) {
      sentlen = ap_rwrite(s->read_buf + s->read_offset, writelen, r);
      if (sentlen < 0) {
	cse_close(s, "write");
	return -1;
      }

      writelen -= sentlen;
      s->read_offset += sentlen;
      len -= sentlen;
    }
  }
  
  return 1;
}

/**
 * Copy data from the JVM to the browser.
 */
static int
send_data(stream_t *s, request_rec *r, int ack, int *keepalive)
{
  int code = HMUX_QUIT;
  char buf[8193];
  char key[8193];
  char value[8193];
  int channel;
  int i;

  /* ap_reset_timeout(r); */
    
  if (cse_fill_buffer(s) < 0)
    return -1;

  /*
  code = cse_read_byte(s);
  if (code != HMUX_CHANNEL) {
    r->status = 500;
    r->status_line = "Protocol error";

    cse_close(s, "bad protocol");
    return -1;
  }
  channel = hmux_read_len(s);
  */
    
  do {
    int len;

    /* ap_reset_timeout(r); */
    
    code = cse_read_byte(s);

    if (s->socket < 0)
      return -1;

    switch (code) {
    case HMUX_CHANNEL:
      channel = hmux_read_len(s);
      LOG(("channel %d\n", channel));
      break;
      
    case HMUX_ACK:
      channel = hmux_read_len(s);
      LOG(("ack %d\n", channel));
      break;
      
    case HMUX_STATUS:
      len = hmux_read_len(s);
      cse_read_limit(s, buf, sizeof(buf), len);
      for (i = 0; buf[i] && buf[i] != ' '; i++) {
      }
      buf[i] = 0;
      r->status = atoi(buf);
      buf[i] = ' ';
      i++;
      r->status_line = ap_pstrdup(r->pool, buf);
      break;

    case HMUX_HEADER:
      len = hmux_read_len(s);
      cse_read_limit(s, key, sizeof(key), len);
      cse_read_string(s, value, sizeof(value));
      if (! strcasecmp(key, "content-type"))
	r->content_type = ap_pstrdup(r->pool, value);
      else
	ap_table_add(r->headers_out, key, value);
      break;
      
    case HMUX_META_HEADER:
      len = hmux_read_len(s);
      cse_read_limit(s, key, sizeof(key), len);
      cse_read_string(s, value, sizeof(value));
      break;

    case HMUX_DATA:
      len = hmux_read_len(s);
      if (cse_write_response(s, len, r) < 0)
	return -1;
      break;

    case HMUX_FLUSH:
      len = hmux_read_len(s);
      ap_rflush(r);
      break;

    case CSE_KEEPALIVE:
      len = hmux_read_len(s);
      *keepalive = 1;
      break;

    case CSE_SEND_HEADER:
      len = hmux_read_len(s);
      ap_send_http_header(r);
      break;

    case -1:
      break;

    case HMUX_QUIT:
    case HMUX_EXIT:
      break;
      
    default:
      len = hmux_read_len(s);
      cse_skip(s, len);
      break;
    }
  } while (code > 0 && code != HMUX_QUIT && code != HMUX_EXIT && code != ack);

  return code;
}

/**
 * handles a client request
 */
static int
write_request(stream_t *s, request_rec *r,
	      config_t *config, cluster_t *cluster, int *keepalive,
              int session_index, int backup_index,
              char *ip, char *session_id)
{
  int len;
  int code;
  int write_length;
  time_t new_time;
  time_t start_time = r->request_time;

  hmux_start_channel(s, 1);
  write_env(s, r, session_id);
  write_headers(s, r);
  write_added_headers(s, r);

  /* read post data */
  if (ap_should_client_block(r)) {
    char buf[BUF_LENGTH];
    int ack_size = s->cluster_srun->srun->send_buffer_size;
    int send_length = 0;

    while ((len = ap_get_client_block(r, buf, BUF_LENGTH)) > 0) {
      /* ap_reset_timeout(r); */
      cse_write_packet(s, HMUX_DATA, buf, len);

      send_length += len;
      
      if (ack_size <= send_length) {
	send_length = 0;
	cse_write_byte(s, HMUX_YIELD);
        code = send_data(s, r, HMUX_ACK, keepalive);
        if (code < 0 || code == HMUX_QUIT || code == HMUX_EXIT)
          break;
      }
    }
  }

  cse_write_byte(s, HMUX_QUIT);

  code = send_data(s, r, HMUX_QUIT, keepalive);

  if (code >= 0 || s->sent_data)
    return code;
  
  write_length = s->write_length; 
  if (cse_open_connection(s, cluster, session_index, backup_index,
                          r->request_time, r->pool)) {
    s->write_length = write_length;
    LOG(("retry connection %d\n", s->socket));
    
    return send_data(s, r, HMUX_QUIT, keepalive);
  }
  else {
    return HTTP_SERVICE_UNAVAILABLE;
  }
}

#ifdef WIN32

int random() { return 0; }

#endif

/**
 * Handle a request.
 */
static int
caucho_request(request_rec *r)
{
  config_t *config = cse_get_module_config(r);
  resin_host_t *host = 0;
  stream_t s;
  int retval;
  int keepalive = 0;
  int reuse;
  int session_index;
  int backup_index;
  char *ip;
  time_t now = r->request_time;
  char *session_id = 0;

  if (! config)
    return HTTP_SERVICE_UNAVAILABLE;
  
  if ((retval = ap_setup_client_block(r, REQUEST_CHUNKED_DECHUNK)))
    return retval;

  /* ap_soft_timeout("servlet request", r); */
  
  if (r->request_config && ! *config->alt_session_url_prefix &&
      ((session_id = ap_get_module_config(r->request_config, &caucho_module)) ||
       r->prev &&
       (session_id = ap_get_module_config(r->prev->request_config, &caucho_module)))) {
    /* *session_id = *config->session_url_prefix; */
  }

  session_index = get_session_index(config, r, &backup_index);
  ip = r->connection->remote_ip;
  
  if (host) {
  }
  else if (config->manual_host)
    host = config->manual_host;
  else {
    host = cse_match_host(config,
			  ap_get_server_name(r),
			  ap_get_server_port(r),
			  now);
  }

  if (! host ||
      ! cse_open_connection(&s, &host->cluster, session_index, backup_index,
                            now, r->pool)) {
    return HTTP_SERVICE_UNAVAILABLE;
  }

  reuse = write_request(&s, r, config, &host->cluster, &keepalive,
                        session_index, backup_index,
                        ip, session_id);
  /*
  ap_kill_timeout(r);
  */
  ap_rflush(r);

  if (reuse == HMUX_QUIT)
    cse_recycle(&s, now);
  else
    cse_close(&s, "no reuse");

  if (reuse == HTTP_SERVICE_UNAVAILABLE)
    return reuse;
  else
    return OK;
}

/**
 * Print the statistics for each JVM.
 */
static void
jvm_status(cluster_t *cluster, request_rec *r)
{
  int i;
  stream_t s;

  ap_rputs("<center><table border=2 width='80%'>\n", r);
  ap_rputs("<tr><th width=\"30%\">Host</th>\n", r);
  ap_rputs("    <th>Active</th>\n", r);
  ap_rputs("    <th>Pooled</th>\n", r);
  ap_rputs("    <th>Connect<br>Timeout</th>\n", r);
  ap_rputs("    <th>Live<br>Time</th>\n", r);
  ap_rputs("    <th>Dead<br>Time</th>\n", r);
  ap_rputs("</tr>\n", r);

  for (; cluster; cluster = cluster->next) {
    for (i = 0; i < cluster->srun_capacity; i++) {
      cluster_srun_t *cluster_srun = cluster->srun_list + i;
      srun_t *srun = cluster_srun->srun;
      int port;
      int pool_count;

      if (! srun)
	continue;
    
      port = srun->port;
      pool_count = ((srun->conn_head - srun->conn_tail + CONN_POOL_SIZE) %
		    CONN_POOL_SIZE);

      ap_rputs("<tr>", r);

      if (! cse_open(&s, cluster, cluster_srun, r->pool, 0)) {
	ap_rprintf(r, "<td bgcolor='#ff6666'>%d. %s:%d%s (down)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }
      else {
	ap_rprintf(r, "<td bgcolor='#66ff66'>%d. %s:%d%s (ok)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }

      /* This needs to be close, because cse_open doesn't use recycle. */
      cse_close(&s, "caucho-status");
      LOG(("close\n"));

      ap_rprintf(r, "<td align=right>%d</td><td align=right>%d</td>",
		 srun->active_sockets, pool_count);
      ap_rprintf(r, "<td align=right>%d</td><td align=right>%d</td><td align=right>%d</td>",
		 srun->connect_timeout, srun->live_time, srun->dead_time);
      ap_rputs("</tr>\n", r);
    }
  }
  ap_rputs("</table></center>\n", r);
}

static void
escape_html(char *dst, char *src)
{
  int ch;
  
  for (; (ch = *src); src++) {
    switch (ch) {
    case '<':
      *dst++ = '&';
      *dst++ = 'l';
      *dst++ = 't';
      *dst++ = ';';
      break;
      
    case '&':
      *dst++ = '&';
      *dst++ = 'a';
      *dst++ = 'm';
      *dst++ = 'p';
      *dst++ = ';';
      break;
      
    default:
      *dst++ = ch;
      break;
    }
  }

  *dst = 0;
}

/**
 * Print a summary of the configuration so users can understand what's
 * going on.  Ping the server to check that it's up.
 */
static int
caucho_status(request_rec *r)
{
  resin_host_t *host;
  web_app_t *app;
  location_t *loc;
  unsigned int now = r->request_time;
  config_t *config = cse_get_module_config(r);
 
  r->content_type = "text/html";
  /* ap_soft_timeout("caucho status", r); */
  if (r->header_only) {
    /* ap_kill_timeout(r); */

    return OK;
  }

  ap_send_http_header(r);

  ap_rputs("<html><title>Status : Caucho Servlet Engine</title>\n", r);
  ap_rputs("<body bgcolor=white>\n", r);
  ap_rputs("<h1>Status : Caucho Servlet Engine</h1>\n", r);

  if (config->error) {
    char buf[BUF_LENGTH];
    escape_html(buf, config->error);
    ap_rprintf(r, "<h2 color='red'>Error : %s</h2>\n", buf);
  }
  
  ap_rprintf(r, "<h2>Configuration Cluster</h2>\n");
  jvm_status(&config->config_cluster, r);
  
  host = config ? config->hosts : 0;
  for (; host; host = host->next) {
    if (host != host->canonical)
      continue;

    /* check updates as appropriate */
    cse_match_host(config, host->name, host->port, now);

    if (! *host->name)
      ap_rprintf(r, "<h2>Default Virtual Host</h2>\n");
    else if (host->port)
      ap_rprintf(r, "<h2>Virtual Host: %s:%d</h2>\n", host->name, host->port);
    else
      ap_rprintf(r, "<h2>Virtual Host: %s</h2>\n", host->name);
    
    jvm_status(&host->cluster, r);

    ap_rputs("<p><center><table border=2 cellspacing=0 cellpadding=2 width='80%'>\n", r);
    ap_rputs("<tr><th width=\"50%\">web-app\n", r);
    ap_rputs("    <th>url-pattern\n", r);

    app = host->applications;
    
    for (; app; app = app->next) {
      for (loc = app->locations; loc; loc = loc->next) {
	if (! strcasecmp(loc->prefix, "/META-INF") ||
	    ! strcasecmp(loc->prefix, "/WEB-INF"))
	  continue;
	
	ap_rprintf(r, "<tr bgcolor='#ffcc66'><td>%s<td>%s%s%s%s%s</tr>\n", 
		   *app->context_path ? app->context_path : "/",
		   loc->prefix,
		   ! loc->is_exact && ! loc->suffix ? "/*" : 
		   loc->suffix && loc->prefix[0] ? "/" : "",
		   loc->suffix ? "*" : "",
		   loc->suffix ? loc->suffix : "",
		   loc->ignore ? " (ignore)" : "");
      }
    }
    ap_rputs("</table></center>\n", r);
  }

  ap_rputs("<hr>", r);
  ap_rprintf(r, "<em>%s<em>", VERSION);
  ap_rputs("</body></html>\n", r);
  
  /* ap_kill_timeout(r); */

  return OK;
}

/**
 * When a child process starts, clear the srun structure so it doesn't
 * mistakenly think the old sockets areopen.
 */
static void
cse_open_child(server_rec *server, pool *p)
{
  LOG(("[%d] open child\n", getpid()));

  cse_close_all();
}

/**
 * Close all the connections cleanly when the Apache child process exits.
 *
 * @param server the Apache server object
 * @param p the Apache memory pool for the server.
 */
static void
cse_close_child(server_rec *server, pool *p)
{
  LOG(("[%d] close child\n", getpid()));

  cse_close_all();
  
  if (g_pool) {
    ap_destroy_pool(g_pool);
    g_pool = 0;
  }
  g_config = 0;
  
  LOG(("[%d] close child done\n", getpid()));
}

/*
 * Only needed configuration is pointer to resin.conf
 */
static command_rec caucho_commands[] = {
    {"ResinConfigServer", cse_config_server_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE12,
     "Adds a new configuration server."},
    {"CauchoStatus", cse_caucho_status_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE1,
     "Configures the caucho-status."},
    {"CauchoConfigFile", cse_config_file_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE1,
     "Pointer to the Caucho configuration file."},
    {"CauchoServerRoot", cse_config_server_root, NULL, RSRC_CONF|ACCESS_CONF, TAKE1,
     "The root server directory."},
    {"CauchoHost", cse_host_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE12,
     "Servlet runner host."},
    {"CauchoBackup", cse_backup_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE12,
     "Servlet runner backup."},
    {"CauchoErrorPage", cse_error_page_command, NULL, RSRC_CONF|ACCESS_CONF, TAKE1,
     "Error page when connections fail."},
    {NULL}
};

/*
 * Caucho right has two content handlers:
 * caucho-status: summary information for debugging
 * caucho-request: dispatch a Caucho request
 */
static const handler_rec caucho_handlers[] =
{
    {"caucho-status", caucho_status},
    {"caucho-request", caucho_request},
    {NULL}
};

/* 
 * module configuration
 *
 * cse_clean_jsessionid needs to be at [2] to clean up the ;jsessionid=
 * dispatch to make urls like /foo;jsessionid=aaaXXX work.
 *
 * cse_dispatch itself must be after [2] to make DirectoryIndex work.
 * cse_dispatch must be before [8] for mod_gzip to work
 */
module caucho_module =
{
    STANDARD_MODULE_STUFF,
    cse_module_init,            /* module initializer */
    cse_create_dir_config,      /* per-directory config creator */
    NULL,                       /* dir config merger */
    cse_create_server_config,   /* server config creator */
    NULL,                       /* server config merger */
    caucho_commands,            /* command table */
    caucho_handlers,            /* [7] list of handlers */
    cse_clean_jsessionid,       /* [2] filename-to-URI translation */
    NULL,                       /* [5] check/validate user_id */
    NULL,                       /* [6] check user_id is valid *here* */
    NULL,                       /* [4] check access by host address */
    cse_dispatch,               /* [7] MIME type checker/setter */
    NULL,                       /* [8] fixups */
    NULL,                       /* [10] logger */
    NULL,                       /* [3] header parser */
    cse_open_child,             /* apache child process init */
    cse_close_child,            /* apache child process exit/cleanup */
    NULL,                       /* [1] post read_request handling */
#if defined(EAPI)
    NULL,                       /* add_module */
    NULL,                       /* del_module */
    NULL,                       /* rewrite_command */
    NULL,                       /* new_connection */
#endif    
};

