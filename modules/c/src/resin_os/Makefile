CC=gcc
CFLAGS_SHLIB=-fpic
LD=/usr/bin/ld
OS=
SO=so
CPU=x86_64
LDFLAGS=
LDFLAGS_SHLIB=-shared -fPIC -L/usr/lib64
PROXY_LIBS=-lpthread
APACHE_INC=-I/usr/include/httpd  -I/usr/include/apr-0 
resin_conf=/etc/httpd/conf/httpd.conf
resin_libexec=/usr/lib64/httpd/modules
resin_home=/home/ferg/ws/resin
JNI_INCLUDE=-I/usr/java/include -I/usr/java/include/linux
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common
CFLAGS = -g -O2 -DPOLL -DB64 -m64 -fPIC -DHAS_JVMTI -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)
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
	- mkdir $(resin_home)/libexec
	cp libresin_os.$(SO) $(resin_home)/libexec



