CC=gcc
CFLAGS_SHLIB=-fpic
LD=/usr/bin/ld
OS=
SO=so
CPU=i386
LDFLAGS=
LDFLAGS_SHLIB=-shared -L/usr/lib
PROXY_LIBS=-lpthread
APACHE_INC=-I/usr/local/apache2/include  -I/usr/local/apache2/include 
resin_conf=/usr/local/apache2/conf/httpd.conf
resin_libexec=/usr/local/apache2/modules
LIBEXEC=libexec
resin_home=/home/ferg/ws/test-resin
JNI_INCLUDE=-I/usr/java/include -I/usr/java/include/linux
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common
CFLAGS = -g -O2 -DPOLL -DHAS_JVMTI -DHAS_JVMDI  -D_FILE_OFFSET_BITS=64 -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)
OBJ= jni_os.o jni_jvmti.o jni_jvmdi.o

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
	- mkdir $(resin_home)/$(LIBEXEC)
	cp libresin_os.$(SO) $(resin_home)/$(LIBEXEC)



