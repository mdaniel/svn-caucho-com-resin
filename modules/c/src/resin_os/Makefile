CC=gcc
CFLAGS_SHLIB=-DSHARED_MODULE
LD=gcc
OS=
SO=jnilib
CPU=powerpc
LDFLAGS=
LDFLAGS_SHLIB=-bundle -undefined suppress -flat_namespace -L/usr/lib
PROXY_LIBS=
APACHE_INC=-I/usr/local/apache2/include  -I/usr/local/apache2/include 
resin_conf=/usr/local/apache2/conf/httpd.conf
resin_libexec=/usr/local/apache2/modules
resin_home=/Users/ferg/ws/resin
JNI_INCLUDE=-I/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home/include
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common
CFLAGS = -g -O2 -DPOLL -DHAS_JVMTI -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)
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



