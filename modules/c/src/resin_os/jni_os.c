/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

#ifdef WIN32
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sys/time.h>
#include <pwd.h>
#include <syslog.h>
#include <netdb.h>
#endif

#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
/* probably system-dependent */
#include <jni.h>

jboolean jvmdi_can_reload_native(JNIEnv *env, jobject obj);
jboolean jvmti_can_reload_native(JNIEnv *env, jobject obj);

jint
jvmti_reload_native(JNIEnv *env,
		    jobject obj,
		    jclass cl,
		    jbyteArray buf,
		    jint offset,
		    jint length);

jint
jvmdi_reload_native(JNIEnv *env,
		    jobject obj,
		    jclass cl,
		    jbyteArray buf,
		    jint offset,
		    jint length);

int
resin_set_byte_array_region(JNIEnv *env,
			    jbyteArray buf, jint offset, jint sublen,
			    char *buffer)
{
  jbyte *cBuf;
  
  /* (*env)->SetByteArrayRegion(env, buf, offset, sublen, buffer); */
  
  cBuf = (*env)->GetPrimitiveArrayCritical(env, buf, 0);

  if (cBuf) {
    memcpy(cBuf + offset, buffer, sublen);

    (*env)->ReleasePrimitiveArrayCritical(env, buf, cBuf, 0);

    return 1;
  }
  
  return 0;
}

int
resin_get_byte_array_region(JNIEnv *env,
			    jbyteArray buf, jint offset, jint sublen,
			    char *buffer)
{
  jbyte *cBuf = (*env)->GetPrimitiveArrayCritical(env, buf, 0);

  if (cBuf) {
    memcpy(buffer, cBuf + offset, sublen);

    (*env)->ReleasePrimitiveArrayCritical(env, buf, cBuf, 0);

    return 1;
  }
  
  return 0;
}

void
resin_printf_exception(JNIEnv *env, const char *cl, const char *fmt, ...)
{
  char buf[8192];
  va_list list;
  jclass clazz;

  va_start(list, fmt);

  vsprintf(buf, fmt, list);

  va_end(list);

  if (env && ! (*env)->ExceptionOccurred(env)) {
    clazz = (*env)->FindClass(env, cl);

    if (clazz) {
      (*env)->ThrowNew(env, clazz, buf);
      return;
    }
  }

  fprintf(stderr, "%s\n", buf);
}

JNIEXPORT jboolean JNICALL
Java_com_caucho_loader_ClassEntry_canReloadNative(JNIEnv *env,
					          jobject obj)
{
  return (jvmti_can_reload_native(env, obj) ||
	  jvmdi_can_reload_native(env, obj));
}

JNIEXPORT jint JNICALL
Java_com_caucho_loader_ClassEntry_reloadNative(JNIEnv *env,
					       jobject obj,
					       jclass cl,
					       jbyteArray buf,
					       jint offset,
					       jint length)
{
  int res = jvmti_reload_native(env, obj, cl, buf, offset, length);

  if (res > 0)
    return res;

  return jvmdi_reload_native(env, obj, cl, buf, offset, length);
}

static char *
get_utf8(JNIEnv *env, jstring jaddr, char *buf, int buflen)
{
  const char *temp_string = 0;

  temp_string = (*env)->GetStringUTFChars(env, jaddr, 0);
  
  if (temp_string) {
    strncpy(buf, temp_string, buflen);
    buf[buflen - 1] = 0;
  
    (*env)->ReleaseStringUTFChars(env, jaddr, temp_string);
  }

  return buf;
}

JNIEXPORT jint JNICALL
Java_com_caucho_server_boot_ResinBoot_execDaemon(JNIEnv *env,
						 jobject obj,
						 jobjectArray j_argv,
						 jobjectArray j_envp,
						 jstring j_pwd)
{
  char **argv;
  char **envp;
  char *pwd;
  int len;
  int i;
  
  if (! j_argv)
    resin_printf_exception(env, "java/lang/NullPointerException", "argv");
  if (! j_envp)
    resin_printf_exception(env, "java/lang/NullPointerException", "argv");
  if (! j_pwd)
    resin_printf_exception(env, "java/lang/NullPointerException", "pwd");

#ifdef WIN32
  resin_printf_exception(env, "java/lang/UnsupportedOperationException", "win32");
#else
  len = (*env)->GetArrayLength(env, j_argv);
  argv = malloc((len + 1) * sizeof(char*));
  argv[len] = 0;
  
  for (i = 0; i < len; i++) {
    jstring j_string;

    j_string = (*env)->GetObjectArrayElement(env, j_argv, i);

    if (j_string) {
      int strlen = (*env)->GetStringUTFLength(env, j_string);
      
      argv[i] = (char *) malloc(strlen + 1);
    
      argv[i] = get_utf8(env, j_string, argv[i], strlen + 1);
    }
  }

  len = (*env)->GetArrayLength(env, j_envp);
  envp = malloc((len + 1) * sizeof(char*));
  envp[len] = 0;
  
  for (i = 0; i < len; i++) {
    jstring j_string;

    j_string = (*env)->GetObjectArrayElement(env, j_envp, i);

    if (j_string) {
      int strlen = (*env)->GetStringUTFLength(env, j_string);
      
      envp[i] = (char *) malloc(strlen + 1);
    
      envp[i] = get_utf8(env, j_string, envp[i], strlen + 1);
    }
  }

  {
    int strlen = (*env)->GetStringUTFLength(env, j_pwd);
    char *pwd;

    pwd = (char *) malloc(strlen + 1);
    pwd = get_utf8(env, j_pwd, pwd, strlen + 1);

    chdir(pwd);
  }

  if (fork())
    return 1;
  
  if (fork())
    exit(0);

#ifndef WIN32
  setsid();
#endif /* WIN32 */

  execve(argv[0], argv, envp);

  fprintf(stderr, "exec failed %s -> %d\n", argv[0], errno);
  exit(1);
#endif  
  return -1;
}
