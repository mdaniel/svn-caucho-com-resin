.SUFFIXES: .o .lo

CC=gcc
CFLAGS=-g -O2 -DPOLL -DHAS_JVMTI -DDEBUG -Wall
CFLAGS_SHLIB=-fpic
LIBTOOL=/home/ferg/test/resin-pro/libtool --silent
LD=/usr/bin/ld
LD_SHLIB=gcc
LDFLAGS=
LDFLAGS_SHLIB=-shared -L/usr/lib
OBJ=stream.o config.o memory.o
OBJ_PICS=stream.lo config.lo memory.lo
INCLUDES= -I/usr/include -I/usr/kerberos/include

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
