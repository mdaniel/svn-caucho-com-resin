/*
 * Copyright (c) 1999-2008 Caucho Technology.  All rights reserved.
 *
 * @author Scott Ferguson
 */

#include <sys/types.h>
#ifdef WIN32
#ifndef _WINSOCKAPI_ 
#define _WINSOCKAPI_
#endif 
#include <windows.h>
#include <winsock2.h>
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#endif
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
/* probably system-dependent */
#include <jni.h>
#include <errno.h>

#include "resin.h"

typedef struct chunk_t {
  int bucket;
  struct chunk_t *next;
} chunk_t;

static int is_init;
static pthread_mutex_t mem_lock;
static int alloc = 0;
static chunk_t *buckets[1024];

static int
get_bucket(int size)
{
  size += sizeof(chunk_t);

  if (size < 4096)
    return (size + 255) / 256;
  else
    return ((size + 4095) / 4096) + 16;
}

static int
get_chunk_size(int size)
{
  if (size + sizeof(chunk_t) < 4096)
    return 256 * ((size + sizeof(chunk_t) + 255) / 256);
  else
    return 4096 * ((size + sizeof(chunk_t) + 4095) / 4096);
}

static void
cse_init_bucket(int size, int alloc_size)
{
  char *data = malloc(alloc_size);
  int bucket = get_bucket(size);
  int chunk_size = get_chunk_size(size);
  int i;

  if (bucket >= 1024)
    fprintf(stderr, "bad bucket size:%d bucket:%d\n", size, bucket);

  for (i = 0; i < alloc_size; i += chunk_size) {
    chunk_t *chunk = (chunk_t *) (data + i);
    chunk->bucket = bucket;
    chunk->next = buckets[bucket];
    buckets[bucket] = chunk;
  }
}

void *
cse_malloc(int size)
{
  int bucket;
  chunk_t *chunk = 0;

  bucket = get_bucket(size);
  
  pthread_mutex_lock(&mem_lock);
  chunk = buckets[bucket];
  if (chunk)
    buckets[bucket] = chunk->next;
  pthread_mutex_unlock(&mem_lock);

  if (chunk) {
  }
  else if (size + sizeof(chunk_t) <= 4096) {
    pthread_mutex_lock(&mem_lock);
    cse_init_bucket(size, 64 * 1024);
    
    chunk = buckets[bucket];
    buckets[bucket] = chunk->next;
    pthread_mutex_unlock(&mem_lock);
  }
  else {
    chunk = (chunk_t *) malloc(get_chunk_size(size));

    if (chunk == 0)
      return 0;

    chunk->bucket = bucket;
  }
  
  chunk->next = 0;
  
  return ((char *) chunk) + sizeof(chunk_t);
}

void
cse_free(void *v_data)
{
  chunk_t *chunk = (chunk_t *) (((char *) v_data) - sizeof(chunk_t));
  int bucket = chunk->bucket;

  if (bucket >= 0 && bucket < 1024) {
    pthread_mutex_lock(&mem_lock);
    chunk->next = buckets[bucket];
    buckets[bucket] = chunk;
    pthread_mutex_unlock(&mem_lock);
  }
  else
    fprintf(stderr, "no bucket\n");
}
