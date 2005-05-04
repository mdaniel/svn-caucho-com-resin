CC=gcc
CFLAGS_SHLIB=-fpic -DSHARED_MODULE
LD=/usr/bin/ld
OS=
SO=so
CPU=i386
LDFLAGS=
LDFLAGS_SHLIB=-shared -L/usr/lib
PROXY_LIBS=-lpthread
APACHE_INC=-I/usr/local/apache1/include
resin_conf=/usr/local/apache1/conf/httpd.conf
resin_libexec=/usr/local/apache1/libexec
resin_home=/home/ferg/test/resin-pro
JNI_INCLUDE=-I/usr/java/include -I/usr/java/include/linux
INCLUDES =  -I/usr/include -I/usr/kerberos/include $(JNI_INCLUDE) -I../common
CFLAGS = -g -O2 -DPOLL -DHAS_JVMTI -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)
OBJ= jni_os.o

all	: libresin_os.$(SO)

libresin_os.so	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -o $@ $(OBJ) $(PROXY_LIBS)

libresin_os.jnilib	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -framework JavaVM -o $@ $(OBJ) $(PROXY_LIBS)

jni_os.o	: Makefile

clean	:
	rm -f *.o *.$(SO)

install	: libresin_os.$(SO)
#	cp resin $(resin_home)/bin
	- mkdir $(resin_home)/libexec
	cp libresin_os.$(SO) $(resin_home)/libexec



