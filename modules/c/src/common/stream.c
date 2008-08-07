/*
 * Copyright (c) 1999-2006 Caucho Technology.  All rights reserved.
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

#include <stdlib.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>

#ifdef WIN32
#include <winsock2.h>
#else
#include <sys/types.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#endif

#ifdef OPENSSL
/* SSLeay stuff */
#include <openssl/rsa.h>       
#include <openssl/crypto.h>
#include <openssl/x509.h>
#include <openssl/pem.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#endif

#include "cse.h"

#define WINDOWS_READ_TIMEOUT 3600
#define DEFAULT_PORT 6802
#define DEAD_TIME 120
#define LIVE_TIME 10
#define CONNECT_TIMEOUT 2

/**
 * Opening method for non-ssl.
 */
static int
std_open(stream_t *stream)
{
  return stream->socket >= 0;
}

static int
poll_read(int fd, int s)
{
	if (s <= 0)
		return 1;

	while (s > 0) {
		fd_set read_set;
		struct timeval timeout;
		int sec = s;
		int result;

		FD_ZERO(&read_set);
		FD_SET(fd, &read_set);

		if (sec >= 3600)
			sec = 3600;

		timeout.tv_sec = sec;
		timeout.tv_usec = 0;

		result = select(fd + 1, &read_set, 0, 0, &timeout);
		if (0)
			return result;
		if (result != 0)
			return result;		

		s -= sec;
	}

	return 0;
}

/**
 * Read for non-ssl.
 */
static int
std_read(stream_t *s, void *buf, int length)
{
#ifdef WIN32
	int timeout = WINDOWS_READ_TIMEOUT;

  if (s->cluster_srun && s->cluster_srun->srun->read_timeout > 0)
	  timeout = s->cluster_srun->srun->read_timeout;

  if (poll_read(s->socket, timeout) <= 0) {
	  cse_close(s, "timeout");
    return -1;
  }
#endif

  return recv(s->socket, buf, length, 0);
}

/**
 * Write for non-ssl.
 */
static int
std_write(stream_t *s, const void *buf, int length)
{
  return send(s->socket, buf, length, 0);
}

/**
 * Close for non-ssl.
 */
static int
std_close(int socket, void *ssl)
{
  return closesocket(socket);
}

#ifdef OPENSSL

/**
 * Opening method for ssl.
 */
static int
ssl_open(stream_t *stream)
{
  SSL_CTX *ctx = stream->cluster_srun->srun->ssl;
  SSL *ssl;

  if (stream->socket < 0)
    return 0;
  
  ssl = SSL_new(ctx);
  
  if (! ssl) {
    close(stream->socket);
    stream->socket = -1;
    ERR(("%s:%d:ssl_open(): can't allocate ssl\n", __FILE__, __LINE__));
    return 0;
  }
  
  SSL_set_fd(ssl, stream->socket);
  
  if (SSL_connect(ssl) < 0) {
    closesocket(stream->socket);
    stream->socket = -1;
    SSL_free(ssl);
    ERR(("%s:%d:ssl_open(): can't connect with ssl\n", __FILE__, __LINE__));
    return 0;
  }

  LOG(("%s:%d:ssl_open(): connect with ssl %d\n",
       __FILE__, __LINE__, stream->socket));
  
  stream->ssl = ssl;
  
  return stream->socket >= 0;
}

/**
 * Read for ssl.
 */
static int
ssl_read(stream_t *s, void *buf, int length)
{
  SSL *ssl = s->ssl;

  if (! ssl)
    return -1;

  return SSL_read(ssl, buf, length);
}

/**
 * Write for non-ssl.
 */
static int
ssl_write(stream_t *s, const void *buf, int length)
{
  SSL *ssl = s->ssl;

  if (! ssl)
    return -1;

  return SSL_write(ssl, (char *) buf, length);
}

/**
 * Close for ssl.
 */
static int
ssl_close(int socket, void *ssl)
{
  if (ssl)
    SSL_free(ssl);
  
  return closesocket(socket);
}
#endif

void
cse_close(stream_t *s, char *msg)
{
  int socket = s->socket;
  s->socket = -1;
  
  if (socket >= 0) {
    LOG(("%s:%d:cse_close(): close %d %s\n", __FILE__, __LINE__, socket, msg));

    cse_kill_socket_cleanup(socket, s->web_pool);

    /* config read/save has no cluster_srun */
    if (s->cluster_srun)
      s->cluster_srun->srun->close(socket, s->ssl);
    else
      closesocket(socket);
  }
}

#ifdef WIN32

static int
cse_connect(struct sockaddr_in *sin, srun_t *srun)
{
  unsigned int sock;
  unsigned long is_nonblock;

  sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock == INVALID_SOCKET) {
    ERR(("%s:%d:cse_connect(): mod_caucho can't create socket.\n",
	 __FILE__, __LINE__));
    return -1; /* bad socket */
  }

  is_nonblock = 1;
  ioctlsocket(sock, FIONBIO, &is_nonblock);
  if (connect(sock, (struct sockaddr *) sin, sizeof(struct sockaddr_in))) {
    WSAEVENT event = WSACreateEvent();
    WSANETWORKEVENTS networkResult;
    int result;

    WSAEventSelect(sock, event, FD_CONNECT);
    result = WSAWaitForMultipleEvents(1, &event, 0,
                                      srun->connect_timeout * 1000, 0);
    WSAEnumNetworkEvents(sock, event, &networkResult);
    WSAEventSelect(sock, 0, 0);
    WSACloseEvent(event);

    if (result != WSA_WAIT_EVENT_0 ||
 	networkResult.iErrorCode[FD_CONNECT_BIT] != NO_ERROR) {
      closesocket(sock);

      return -1;
    }
  }

  is_nonblock = 0;
  ioctlsocket(sock, FIONBIO, &is_nonblock);
  LOG(("%s:%d:cse_connect(): connect %d\n", __FILE__, __LINE__, sock));

  return sock;
}

#else

static int
cse_connect(struct sockaddr_in *sin, srun_t *srun)
{
  int sock;
  fd_set write_fds;
  struct timeval timeout;
  int flags;
  int error = 0;
  unsigned int len = sizeof(error);

  sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock < 0) {
    ERR(("%s:%d:cse_connect(): mod_caucho can't create socket.\n",
	 __FILE__, __LINE__));
    return -1; /* bad socket */
  }

  if (sock < FD_SETSIZE) {
    flags = fcntl(sock, F_GETFL);
    fcntl(sock, F_SETFL, O_NONBLOCK|flags);
    FD_ZERO(&write_fds);
    FD_SET(sock, &write_fds);
  }

  timeout.tv_sec = srun->connect_timeout;
  timeout.tv_usec = 0;

  if (! connect(sock, (const struct sockaddr *) sin, sizeof(*sin))) {
    if (sock < FD_SETSIZE) {
      fcntl(sock, F_SETFL, flags);
    }

    return sock;
  }
  else if (FD_SETSIZE <= sock) {
    ERR(("%s:%d:cse_connect(): failed %x %d %d\n",
	 __FILE__, __LINE__,
	 sin->sin_addr.s_addr,
	 ntohs(sin->sin_port), errno));

    close(sock);
    
    return -1;
  }
  /*
   * Solaris can return other errno for a connection that will succeed,
   * see bug #1415.  So we avoid the extra error checking here and only
   * check after the select() call.
   */
  /*
  else if (errno != EWOULDBLOCK && errno != EINPROGRESS) {
    ERR(("%s:%d:cse_connect(): connect quickfailed %x %d %d\n",
	 __FILE__, __LINE__,
	 sin->sin_addr.s_addr,
	 ntohs(sin->sin_port), errno));
    
    close(sock);

    return -1;
  }
  */
  else if (select(sock + 1, 0, &write_fds, 0, &timeout) <= 0) {
    ERR(("%s:%d:cse_connect(): timeout %x %d %d\n",
	 __FILE__, __LINE__,
	 sin->sin_addr.s_addr,
	 ntohs(sin->sin_port), errno));

    fcntl(sock, F_SETFL, flags);

    close(sock);
    
    return -1;
  }
  else if (! FD_ISSET(sock, &write_fds)
	   || getsockopt(sock, SOL_SOCKET, SO_ERROR, &error, &len) < 0
	   || error) {
    ERR(("%s:%d:cse_connect(): connect failed %x %d %d\n",
	 __FILE__, __LINE__,
	 sin->sin_addr.s_addr,
	 ntohs(sin->sin_port), errno));
    close(sock);

    return -1;
  }
  else {
    fcntl(sock, F_SETFL, flags);

    LOG(("%s:%d:cse_connect(): connect %x:%d -> %d\n",
	 __FILE__, __LINE__,
         sin->sin_addr.s_addr, ntohs(sin->sin_port), sock));
         
    return sock;
  }
}

#endif

static int
cse_connect_wait(struct sockaddr_in *sin)
{
  int sock;

  sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock < 0) {
    ERR(("%s:%d:cse_connect_wait(): mod_caucho can't create socket.\n",
	 __FILE__, __LINE__));
    return -1; /* bad socket */
  }
  
  if (! connect(sock, (const struct sockaddr *) sin, sizeof(*sin))) {
    return sock;
  }
  
  LOG(("%s:%d:cse_connect_wait(): can't connect %x %d %d\n",
       __FILE__, __LINE__,
       sin->sin_addr.s_addr,
       ntohs(sin->sin_port), errno));

  closesocket(sock);
    
  return -1;
}

int
cse_open(stream_t *s, cluster_t *cluster, cluster_srun_t *cluster_srun,
         void *web_pool, int wait)
{
  config_t *config = cluster->config;
  struct sockaddr_in sin;
  srun_t *srun = cluster_srun->srun;

  if (! srun)
    return 0;
 
  s->config = config;
  s->update_count = config->update_count;
  s->pool = config->p;
  s->web_pool = web_pool;
  s->write_length = 0;
  s->ssl = 0;
  s->read_length = 0;
  s->read_offset = 0;
  s->cluster_srun = cluster_srun;
  s->sent_data = 0;

  memset(&sin, 0, sizeof(sin));
  sin.sin_family = AF_INET;
  if (srun->host) {
    memcpy(&sin.sin_addr, srun->host, sizeof(struct in_addr));
  }
  else
    sin.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

  if (srun->port <= 0)
    srun->port = DEFAULT_PORT;

  sin.sin_port = htons((short) srun->port);

  if (wait || srun->connect_timeout <= 0)
    s->socket = cse_connect_wait(&sin);
  else
    s->socket = cse_connect(&sin, srun);

  if (s->socket < 0) {
    ERR(("%s:%d:cse_open(): open new failed %x:%d\n",
	 __FILE__, __LINE__,
	 s->socket, *srun->host, srun->port));
    return 0;
  }

  if (srun->send_buffer_size == 0) {
    int size;
    unsigned int len = sizeof(size);
    
#ifdef SO_SNDBUF
    if (getsockopt(s->socket, SOL_SOCKET, SO_SNDBUF, (char *) &size, &len) >= 0) {
      size -= 1024;

      if (size < 8192)
	size = 8192;

      srun->send_buffer_size = size;
    }
#else
    srun->send_buffer_size = 16 * 1024;
#endif
    
    LOG(("%s:%d:cse_open(): send buffer size %d\n",
	 __FILE__, __LINE__,
	 srun->send_buffer_size));
  }
  
  LOG(("%s:%d:cse_open(): open new connection %d %x:%d\n",
       __FILE__, __LINE__,
       s->socket, *srun->host, srun->port));

  return srun->open(s);
}

/**
 * Flush the results to the stream.
 *
 * @param s the buffered stream for the results.
 */
int
cse_flush(stream_t *s)
{
  unsigned char *buf = s->write_buf;
  int length = s->write_length;

  while (length > 0) {
    int len;

    /* config read/save has no cluster_srun */
    if (s->cluster_srun)
      len = s->cluster_srun->srun->write(s, buf, length);
    else
      len = write(s->socket, buf, length);

    if (len <= 0) {
      cse_close(s, "flush");

      return -1;
    }

    length -= len;
    buf += len;
  }
  
  s->sent_data = 1;
  s->write_length = 0;

  return 0;
}

/**
 * Flushes the output buffer and fills the read buffer.  The two buffers
 * are combined so we can try another srun if the request fails.
 */
int
cse_fill_buffer(stream_t *s)
{
  int len = 0;
  
  if (s->socket < 0)
    return -1;

  /* flush the buffer */
  if (s->write_length > 0) {
    LOG(("%s:%d:cse_fill_buffer(): write %d %d\n",
	 __FILE__, __LINE__, s->socket, s->write_length));

    /* config read/save has no cluster_srun */
    if (s->cluster_srun)
      len = s->cluster_srun->srun->write(s, s->write_buf, s->write_length);
    else
      len = write(s->socket, s->write_buf, s->write_length);

    if (len != s->write_length) {
      cse_close(s, "flush");

      return -1;
    }
  }

  s->read_offset = 0;
  /* config read/save has no cluster_srun */
  if (s->cluster_srun)
    s->read_length = s->cluster_srun->srun->read(s, s->read_buf, BUF_LENGTH);
  else
    s->read_length = read(s->socket, s->read_buf, BUF_LENGTH);
  
  if (s->read_length <= 0) {
    cse_close(s, "fill_buffer");
    
    return -1;
  }

  s->sent_data = 1;
  s->write_length = 0;
  
  return s->read_length;
}

int
cse_read_byte(stream_t *s)
{
  if (s->read_offset >= s->read_length) {
    if (cse_fill_buffer(s) < 0)
      return -1;
  }

  return s->read_buf[s->read_offset++];
}

void
cse_write(stream_t *s, const char *buf, int length)
{
  int write_length = s->write_length;
  
  /* XXX: writev??? */

  if (BUF_LENGTH < write_length + length) {
    if (write_length > 0) {
      if (cse_flush(s) < 0) {
        s->sent_data = 1;
        return;
      }

      write_length = 0;
    }

    if (BUF_LENGTH <= length) {
      int len;

      /* config read/save has no cluster_srun */
      if (s->cluster_srun)
	len = s->cluster_srun->srun->write(s, buf, length);
      else
	len = write(s->socket, buf, length);
      s->sent_data = 1;
			       
      if (len < 0)
	cse_close(s, "write");
      
      return;
    }
  }

  memcpy(s->write_buf + write_length, buf, length);
  s->write_length = write_length + length;
}

void
cse_write_byte(stream_t *s, int ch)
{
  /* XXX: writev??? */

  if (BUF_LENGTH < s->write_length + 1) {
    if (s->write_length > 0) {
      if (cse_flush(s) < 0) {
        s->sent_data = 1;
        return;
      }
    }
  }

  s->write_buf[s->write_length++] = ch;
}

int
cse_read_all(stream_t *s, char *buf, int len)
{
  while (len > 0) {
    int sublen;

    if (s->read_offset >= s->read_length) {
      if (cse_fill_buffer(s) < 0)
        return -1;
    }

    sublen = s->read_length - s->read_offset;
    if (len < sublen)
      sublen = len;

    memcpy(buf, s->read_buf + s->read_offset, sublen);

    buf += sublen;
    len -= sublen;
    s->read_offset += sublen;
  }

  return 1;
}

int
cse_skip(stream_t *s, int len)
{
  while (len > 0) {
    int sublen;

    if (s->read_offset >= s->read_length) {
      if (cse_fill_buffer(s) < 0)
	return -1;
    }

    sublen = s->read_length - s->read_offset;
    if (len < sublen)
      sublen = len;

    len -= sublen;
    s->read_offset += sublen;
  }

  return 1;
}

int
cse_read_limit(stream_t *s, char *buf, int buflen, int readlen)
{
  int result;
  
  if (readlen <= buflen) {
    result = cse_read_all(s, buf, readlen);
    buf[readlen] = 0;
  }
  else {
    result = cse_read_all(s, buf, buflen);
    buf[buflen - 1] = 0;
    cse_skip(s, readlen - buflen);
  }

  return result > 0 ? readlen : 0;
}

int
hmux_read_len(stream_t *s)
{
  int l1 = cse_read_byte(s) & 0xff;
  int l2 = cse_read_byte(s);
  
  if (l2 < 0)
	  return -1;
  else
	  l2 = l2 & 0xff;

  return (l1 << 8) + l2;
}

/**
 * write a packet to srun
 *
 * @param s stream to srun
 * @param code packet code
 * @param buf data buffer
 * @param length length of data in buffer
 */
void
cse_write_packet(stream_t *s, char code, const char *buf, int length)
{
  char temp[4];

  temp[0] = code;
  temp[1] = (length >> 8) & 0xff;
  temp[2] = (length) & 0xff;

  cse_write(s, temp, 3);
  if (length >= 0)
    cse_write(s, buf, length);
}

/**
 * writes a string to srun
 */
void
cse_write_string(stream_t *s, char code, const char *buf)
{
  if (buf)
    cse_write_packet(s, code, buf, strlen(buf));
}

/**
 * writes a string to srun
 */
void
hmux_write_string(stream_t *s, char code, const char *buf)
{
  if (buf)
    cse_write_packet(s, code, buf, strlen(buf));
}

/**
 * write a packet to srun
 *
 * @param s stream to srun
 * @param code packet code
 * @param int data int
 */
void
hmux_write_int(stream_t *s, char code, int i)
{
  char temp[8];

  temp[0] = code;
  temp[1] = 0;
  temp[2] = 4;
  temp[3] = (char) (i >> 24);
  temp[4] = (char) (i >> 16);
  temp[5] = (char) (i >> 8);
  temp[6] = (char) (i);

  cse_write(s, temp, 7);
}

void
hmux_start_channel(stream_t *s, unsigned short channel)
{
  cse_write_byte(s, HMUX_CHANNEL);
  cse_write_byte(s, channel >> 8);
  cse_write_byte(s, channel);
}

void
hmux_write_close(stream_t *s)
{
  cse_write_byte(s, HMUX_QUIT);
}

void
hmux_write_exit(stream_t *s)
{
  cse_write_byte(s, HMUX_EXIT);
}

int
hmux_read_string(stream_t *s, char *buf, int length)
{
  int l1, l2;
  int read_length;

  length--;

  l1 = cse_read_byte(s) & 0xff;
  l2 = cse_read_byte(s);

  if (l2 < 0) {
	  read_length = -1;
  }
  else {
	l2 = l2 & 0xff;
	read_length = (l1 << 8) + l2;
  }
  if (s->socket < 0) {
    *buf = 0;
    return -1;
  }

  if (length > read_length)
    length = read_length;

  if (cse_read_all(s, buf, length) < 0) {
    *buf = 0;
    return -1;
  }

  buf[length] = 0;

  /* scan extra */
  for (read_length -= length; read_length > 0; read_length--)
    cse_read_byte(s);

  return length;
}

int
cse_read_string(stream_t *s, char *buf, int length)
{
  int code;
  int read_length;

  length--;

  code = cse_read_byte(s);

  read_length = hmux_read_string(s, buf, length);

  if (read_length < 0)
    return -1;
  else
    return code;
}

/**
 * Decodes the first 3 characters of the session to see which
 * JVM owns it.
 */
static int
decode(char code)
{
  if ('a' <= code && code <= 'z')
    return code - 'a';
  else if ('A' <= code && code <= 'Z')
    return code - 'A' + 26;
  else if ('0' <= code && code <= '9')
    return code - '0' + 52;
  else if (code == '_')
    return 62;
  else if (code == '-')
    return 63;
  else
    return -1;
}

/**
 * Returns the session id from a cookie.
 */
int
cse_session_from_string(char *source, char *cookie, int *backup)
{
  char *match = strstr(source, cookie);

  if (match) {
    int len = strlen(cookie);

    if (match[len] == '=')
      len++;

    *backup = decode(match[len + 1]);
    
    return decode(match[len]);
  }

  return -1;
}

static srun_t *
cse_find_config_srun(config_t *config, const char *hostname, int port, int ssl)
{
  int i;
  srun_t *srun;
  
  for (i = 0; i < config->srun_capacity; i++) {
    srun = config->srun_list[i];
    
    if (! strcmp(srun->hostname, hostname) &&
	srun->port == port &&
	(srun->ssl != 0) == ssl) {
      return srun;
    }
  }

  return 0;
}

static void
cse_add_config_srun(config_t *config, srun_t *srun)
{
  int size;
  srun_t **srun_list;

  size = config->srun_capacity;

  srun_list = malloc((size + 1) * sizeof(srun_t *));

  memcpy(srun_list, config->srun_list, size * sizeof(srun_t *));

  srun_list[size] = srun;

  config->srun_list = srun_list;
  config->srun_capacity = size + 1;
}

static srun_t *
cse_add_srun(cluster_t *cluster, const char *hostname, int port, int ssl)
{
  struct hostent *hostent = 0;
  srun_t *srun = 0;
  config_t *config = cluster->config;
  int i;

  LOG(("%s:%d:cse_add_srun(): adding host %s:%d cluster=%p\n",
       __FILE__, __LINE__, hostname, port, cluster));

  srun = cse_find_config_srun(config, hostname, port, ssl);
  if (srun)
    return srun;
  
  srun = malloc(sizeof(srun_t));
  memset(srun, 0, sizeof(srun_t));

  hostent = gethostbyname(hostname);
  if (hostent && hostent->h_addr) {
    srun->hostname = strdup(hostname);
    srun->host = (struct in_addr *) malloc(sizeof (struct in_addr));
    memcpy(srun->host, hostent->h_addr, sizeof(struct in_addr));
    srun->port = port;
    srun->conn_head = 0;
    srun->conn_tail = 0;
    srun->max_sockets = 32;

    srun->connect_timeout = CONNECT_TIMEOUT;
    srun->live_time = LIVE_TIME;
    srun->dead_time = DEAD_TIME;

    srun->open = std_open;
    srun->read = std_read;
    srun->write = std_write;
    srun->close = std_close;

#ifdef OPENSSL
    if (ssl) {
      SSL_CTX* ctx;
      SSL_METHOD *meth;

      SSLeay_add_ssl_algorithms();
      meth = SSLv3_client_method();
      SSL_load_error_strings();
      ctx = SSL_CTX_new(meth);

      if (ctx) {
        srun->ssl = ctx;
        srun->open = ssl_open;
        srun->read = ssl_read;
        srun->write = ssl_write;
        srun->close = ssl_close;
      }
      else {
        ERR(("%s:%d:cse_add_srun(): can't initialize ssl",
	     __FILE__, __LINE__));
      }
    }
#endif

    srun->lock = cse_create_lock(config);
    LOG(("%s:%d:cse_add_srun(): srun lock %x\n",
	 __FILE__, __LINE__, srun->lock));

    cse_add_config_srun(config, srun);
    
    return srun;
  }

  return 0;
}

/**
 * Adds a new host to the configuration
 */
cluster_srun_t *
cse_add_cluster_server(cluster_t *cluster,
		       const char *hostname, int port, const char *id,
		       int index, int is_backup, int is_ssl)
{
  config_t *config = cluster->config;
  srun_t *srun;
  cluster_srun_t *cluster_srun;

  if (index < 0)
    index = cluster->srun_size;

  /* Resize if too many hosts. */
  while (cluster->srun_capacity <= index) {
    int capacity = cluster->srun_capacity;
    cluster_srun_t *srun_list;

    if (capacity == 0)
      capacity = 16;

    srun_list =
      (cluster_srun_t *) cse_alloc(config->p,
				   2 * capacity * sizeof(cluster_srun_t));
    
    memset(srun_list, 0, 2 * capacity * sizeof(cluster_srun_t));
    
    if (cluster->srun_list) {
      memcpy(srun_list, cluster->srun_list,
	     capacity * sizeof(cluster_srun_t));
    }
    
    cluster->srun_capacity = 2 * capacity;
    cluster->srun_list = srun_list;
  }

  srun = cse_add_srun(cluster, hostname, port, is_ssl);

  if (srun) {
    cluster_srun = &cluster->srun_list[index];
    cluster_srun->srun = srun;
    cluster_srun->is_backup = is_backup;
    cluster_srun->id = cse_strdup(config->p, id);
    cluster_srun->index = index;
    cluster_srun->is_valid = 1;

    if (cluster->srun_size <= index)
      cluster->srun_size = index + 1;

    cluster->round_robin_index = -1;
    
    return cluster_srun;
  }
  else {
    cse_error(config, "Resin can't find host %s\n", hostname);

    return 0;
  }
}

/**
 * reuse the socket
 */
static void
cse_reuse(stream_t *s, cluster_t *cluster, cluster_srun_t *srun,
          int socket, void *ssl,
	  time_t request_time, void *web_pool)
{
  config_t *config = cluster->config;
  
  s->socket = socket;
  s->ssl = ssl;
                     
  s->pool = config->p;
  s->web_pool = web_pool;
  s->config = config;
  s->update_count = config->update_count;
  s->write_length = 0;
  s->read_length = 0;
  s->read_offset = 0;

  s->cluster_srun = srun;
  s->sent_data = 0;
  
  srun->srun->is_dead = 0;
  
  LOG(("%s:%d:cse_reuse(): reopen %d\n", __FILE__, __LINE__, s->socket));
}

/**
 * Closes the idle sockets.
 *
 * Must be called from within a lock.
 */
static void
cse_close_idle(srun_t *srun, time_t now)
{
  int tail;
  int next_tail;
  
  if (! srun)
    return;

  for (tail = srun->conn_tail;
       tail != srun->conn_head;
       tail = next_tail) {
    struct conn_t *conn;
    
    next_tail = (tail + 1) % CONN_POOL_SIZE;

    conn = &srun->conn_pool[tail];

    /* from here on, it's live connections. */
    if (now < conn->last_time + srun->live_time)
      return;
    
    srun->conn_tail = next_tail;
    LOG(("%s:%d:cse_close_idle(): closing idle socket:%d\n",
	 __FILE__, __LINE__, conn->socket));
    srun->close(conn->socket, conn->ssl);
  }
}

/**
 * Try to recycle the socket so the next request can reuse it.
 */
void
cse_recycle(stream_t *s, time_t now)
{
  int socket = s->socket;
  cluster_srun_t *cluster_srun = s->cluster_srun;
  srun_t *srun = cluster_srun ? cluster_srun->srun : 0;

  if (! srun) {
    cse_close(s, "recycle");
    return;
  }
  
  cse_lock(srun->lock);

  cse_close_idle(srun, now);
  
  if (socket >= 0 && s->config->update_count == s->update_count) {
    int head = srun->conn_head;
    int next_head = (head + 1) % CONN_POOL_SIZE;

    /* If there's room in the ring, add it. */
    if (next_head != srun->conn_tail) {
      s->socket = -1;
      cse_kill_socket_cleanup(socket, s->web_pool);
      srun->conn_pool[head].socket = socket;
      srun->conn_pool[head].ssl = s->ssl;
      srun->conn_pool[head].last_time = now;
      srun->conn_head = next_head;
      cse_unlock(srun->lock);
      LOG(("%s:%d:cse_recycle(): recycle %d\n", __FILE__, __LINE__, socket));
      return;
    }
  }
  
  cse_unlock(srun->lock);
  
  if (socket >= 0) {
    LOG(("%s:%d:cse_recycle(): close2 %d update1:%d update2:%d max-sock:%d\n",
	 __FILE__, __LINE__,
         socket, s->config->update_count, s->update_count,
         srun ? srun->max_sockets : -1));
    
    cse_close(s, "recycle");
  }
}

void
close_srun(srun_t *srun, time_t now)
{
  int tail;

  cse_lock(srun->lock);

  for (tail = srun->conn_tail;
       tail != srun->conn_head;
       tail = (tail + 1) % CONN_POOL_SIZE) {
    struct conn_t *conn = &srun->conn_pool[tail];
    srun->close(conn->socket, conn->ssl);
    LOG(("%s:%d:close_srun(): close timeout %d\n",
	 __FILE__, __LINE__, srun->conn_pool[tail]));;
  }
  srun->conn_head = srun->conn_tail = 0;
  
  cse_unlock(srun->lock);
}

/**
 * Try to reuse a socket
 */
static int
cse_reuse_socket(stream_t *s, cluster_t *cluster, cluster_srun_t *cluster_srun,
		 time_t now, void *web_pool)
{
  int head;
  int next_head;
  srun_t *srun = cluster_srun->srun;

  LOG(("%s:%d:cse_reuse_socket(): reuse head:%d tail:%d\n",
       __FILE__, __LINE__, srun->conn_head, srun->conn_tail));

  if (! srun || srun->conn_head == srun->conn_tail)
    return 0;
  
  cse_lock(srun->lock);
  for (head = srun->conn_head;
       head != srun->conn_tail;
       head = next_head) {
    struct conn_t *conn;
    
    next_head = (head + CONN_POOL_SIZE - 1) % CONN_POOL_SIZE;

    conn = &srun->conn_pool[next_head];
    
    if (conn->last_time + srun->live_time < now) {
      LOG(("%s:%d:cse_reuse_socket(): closing idle socket:%d\n",
	   __FILE__, __LINE__, conn->socket));
      srun->close(conn->socket, conn->ssl);
    }
    else {
      int socket;
      void *ssl;

      socket = conn->socket;
      ssl = conn->ssl;
      srun->conn_head = next_head;

      cse_reuse(s, cluster, cluster_srun, socket, ssl, now, web_pool);
      cse_unlock(srun->lock);
      
      return 1;
    }
  }

  srun->conn_head = head;
  cse_unlock(srun->lock);

  return 0;
}

void
cse_close_sockets(config_t *config)
{
}

void
cse_close_all()
{
}

static int
select_host(cluster_t *cluster, time_t now)
{
  int size;
  int round_robin;
  int i;
  int best_srun;
  int best_cost = 0x7fffffff;
  
  size = cluster->srun_size;  
  if (size < 1)
    return -1;

  if (cluster->round_robin_index < 0) {
    srand(65521 * time(0) + getpid() + (int) cluster);
    round_robin = rand();
    if (round_robin < 0)
      round_robin = -round_robin;
    
    cluster->round_robin_index = round_robin % size;
  }

  round_robin = (cluster->round_robin_index + 1) % size;

  for (i = 0; i < size; i++) {
    cluster_srun_t *cluster_srun = &cluster->srun_list[round_robin];

    if (! cluster_srun->is_backup)
      break;

    round_robin = (round_robin + 1) % size;
  }
  
  cluster->round_robin_index = round_robin;
  best_srun = round_robin;

  for (i = 0; i < size; i++) {
    int index = (i + round_robin) % size;
    cluster_srun_t *cluster_srun = &cluster->srun_list[index];
    srun_t *srun = cluster_srun->srun;
    /* int tail; */
    int cost;

    if (! srun)
      continue;
    
    cost = srun->active_sockets;
    
    if (srun->is_dead && now < srun->fail_time + srun->dead_time)
      continue;
    else if (cost < best_cost) {
      best_srun = index;
      best_cost = cost;
    }

    /* Close idle connections. */
    /* XXX: moved to the close
    for (tail = srun->conn_tail;
         tail != srun->conn_head;
         tail = (tail + 1) % CONN_POOL_SIZE) {
      struct conn_t *conn = &srun->conn_pool[tail];
      
      if (now < conn->last_time + srun->live_time)
        break;
      
      srun->close(conn->socket, conn->ssl);
      srun->conn_tail = (tail + 1) % CONN_POOL_SIZE;
    }
    */
  }

  return best_srun;
}

/**
 * Opens any connection within the current group.
 */
static int
open_connection_group(stream_t *s, cluster_t *cluster,
                      cluster_srun_t *owner_item, int offset,
                      time_t now, void *web_pool,
                      int ignore_dead)
{
  cluster_srun_t *cluster_srun = 0;
  srun_t *srun;

  if (offset < 0)
    cluster_srun = owner_item;
  /*
  else if (owner_item->group_size < 2)
    return 0;
  else {
    int delta = offset % (owner_item->group_size - 1) + 1;
    int index = (owner_item->group_index + delta) % owner_item->group_size;

    cluster_srun = owner_item->group[index];
  }
  */
  else
    cluster_srun = owner_item;
  
  srun = cluster_srun->srun;

  if (! srun)
    return 0;

  if (cse_reuse_socket(s, cluster, cluster_srun, now, web_pool)) {
    return 1;
  }
  else if (ignore_dead &&
           srun->is_dead && now < srun->fail_time + srun->dead_time) {
  }
  else if (cse_open(s, cluster, cluster_srun, web_pool, 0)) {
    srun->is_dead = 0;
    return 1;
  }
  else {
    srun->is_dead = 1;
    srun->fail_time = now;
  }

  return 0;
}

static int
open_connection_any_host(stream_t *s, cluster_t *cluster, int host,
                         time_t now, void *web_pool, int ignore_dead)
{
  int i;

  int size = cluster->srun_size;

  /*
   * Okay, the primaries failed.  So try the secondaries.
   */
  for (i = 0; i < size; i++) {
    cluster_srun_t *cluster_srun = cluster->srun_list + (host + i) % size;
    srun_t *srun = cluster_srun->srun;

    if (! srun) {
    }
    else if (cse_reuse_socket(s, cluster, cluster_srun, now, web_pool)) {
      srun->is_dead = 0;
      return 1;
    }
    else if (ignore_dead && cluster_srun->is_backup) {
    }
    else if (ignore_dead &&
             srun->is_dead && now < srun->fail_time + srun->dead_time) {
    }
    else if (cse_open(s, cluster, cluster_srun, web_pool, 0)) {
      srun->is_dead = 0;
      return 1;
    }
    else {
      srun->is_dead = 1;
      srun->fail_time = now;
    }
  }

  return 0;
}

static int
open_session_host(stream_t *s, cluster_t *cluster,
                  int session_index, int backup_index,
                  time_t now, void *web_pool)
{
  int host;
  int size = cluster->srun_size;

  if (size > 0)
    session_index = session_index % size;

  for (host = 0; host < size; host++) {
    if (cluster->srun_list[host].index == session_index) {
      cluster_srun_t *owner = &cluster->srun_list[host];

      /* try to open a connection to the session owner */
      if (open_connection_group(s, cluster, owner, -1,
                                now, web_pool, 1))
        return 1;
      /* or the backup */
      else if (open_connection_group(s, cluster, owner, backup_index, 
                                     now, web_pool, 1))
        return 1;
#if 0      
      /* try the original, but force a connect */
      else if (open_connection_group(s, cluster, owner, -1,
                                     now, web_pool, 0))
        return 1;
      /* try the backup, but force a connect */
      else if (open_connection_group(s, cluster, owner, backup_index,
                                     now, web_pool, 0))
        return 1;
#endif

      return 0;
    }
  }

  return 0;
}

static int
open_connection(stream_t *s, cluster_t *cluster,
                int session_index, int backup_index,
                time_t now, void *web_pool)
{
  int size;
  int host;

  size = cluster->srun_size;

  if (session_index < 0)
    host = select_host(cluster, now);
  else if (open_session_host(s, cluster,
                             session_index, backup_index,
                             now, web_pool))
    return 1;
  else
    host = select_host(cluster, now);

  if (host < 0)
    return 0;

  /* try opening while ignoring dead servers and backups */
  if (open_connection_any_host(s, cluster, host, now, web_pool, 1) > 0)
    return 1;
  /* otherwise try the dead servers and backups too */
  else
    return open_connection_any_host(s, cluster, host, now, web_pool, 0) > 0;
}

int
cse_open_connection(stream_t *s, cluster_t *cluster,
                    int session_index, int backup_index,
                    time_t now, void *web_pool)
{
  config_t *config = cluster->config;

  memset(s, 0, sizeof(stream_t));
  
  s->config = config;
  s->socket = -1;
  s->update_count = config->update_count;
  s->pool = s->config->p;
  s->web_pool = web_pool;

  if (config->disable_sticky_sessions)
    session_index = -1;
  
  if (open_connection(s, cluster, session_index, backup_index, now, web_pool)) {
    cse_set_socket_cleanup(s->socket, web_pool);
    return 1;
  }
  else {
    return 0;
  }
}

int
cse_open_any_connection(stream_t *s, cluster_t *cluster, time_t now)
{
  return cse_open_connection(s, cluster, -1, -1, now,
			     cluster->config->web_pool);
}

int
cse_open_live_connection(stream_t *s, cluster_t *cluster, time_t now)
{
  int host;
  void *web_pool = cluster->config->web_pool;

  host = select_host(cluster, now);

  if (host < 0)
    return 0;

  /* open, but ignore dead servers and backups */
  return open_connection_any_host(s, cluster, host, now, web_pool, 1) > 0;
}
