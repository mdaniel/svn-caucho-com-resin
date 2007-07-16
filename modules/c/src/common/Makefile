.SUFFIXES: .o .lo

CC=gcc
CFLAGS=-g -O2 -DPOLL -D_POSIX_PTHREAD_SEMANTICS -DHAS_SOCK_TIMEOUT -DHAS_JVMTI -DHAS_JVMDI
CFLAGS_SHLIB=-DSHARED_MODULE
LIBTOOL=/Users/ferg/ws/test-resin/libtool --silent
EGREP=grep -E
LD=gcc
LD_SHLIB=cc
LDFLAGS=
LDFLAGS_SHLIB=-bundle -undefined suppress -flat_namespace -Wl,-bind_at_load -L/usr/lib
OBJ=stream.o config.o memory.o
OBJ_PICS=stream.lo config.lo memory.lo
INCLUDES= -I/usr/include

all	: common.o $(OBJ_PICS)

common.o	: $(OBJ)
	$(LD) -r -o $@ $(OBJ)

common.lo	: $(OBJ_PICS)
	$(LD) -r -o $@ $(OBJ_PICS)

.c.lo	:
	$(LIBTOOL) --mode=compile $(CC) -o $@ -c $(INCLUDES) $(CFLAGS) $<

.c.o	:
	$(CC) -c $(INCLUDES) $(CFLAGS) $<

install	:

clean	:
	- rm *.o *.lo *.la *.so

$(OBJ)	: Makefile cse.h
$(OBJ_PICS)	: Makefile cse.h
