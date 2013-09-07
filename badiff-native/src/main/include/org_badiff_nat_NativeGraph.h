/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_badiff_nat_NativeGraph */

#ifndef _Included_org_badiff_nat_NativeGraph
#define _Included_org_badiff_nat_NativeGraph
#ifdef __cplusplus
extern "C" {
#endif
#undef org_badiff_nat_NativeGraph_STOP
#define org_badiff_nat_NativeGraph_STOP 0L
#undef org_badiff_nat_NativeGraph_DELETE
#define org_badiff_nat_NativeGraph_DELETE 1L
#undef org_badiff_nat_NativeGraph_INSERT
#define org_badiff_nat_NativeGraph_INSERT 2L
#undef org_badiff_nat_NativeGraph_NEXT
#define org_badiff_nat_NativeGraph_NEXT 3L
/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    new0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_new0
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    compute0
 * Signature: ([B[B)V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_compute0
  (JNIEnv *, jobject, jbyteArray, jbyteArray);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    walk0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_badiff_nat_NativeGraph_walk0
  (JNIEnv *, jobject);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    flag0
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_org_badiff_nat_NativeGraph_flag0
  (JNIEnv *, jobject);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    val0
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_org_badiff_nat_NativeGraph_val0
  (JNIEnv *, jobject);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    prev0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_prev0
  (JNIEnv *, jobject);

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    free0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_free0
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
