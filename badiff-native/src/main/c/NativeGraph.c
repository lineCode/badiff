/*
 * NativeGraph.c
 *
 *  Created on: Sep 7, 2013
 *      Author: robin
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "org_badiff_nat_NativeGraph.h"

#define STOP org_badiff_nat_NativeGraph_STOP
#define DELETE org_badiff_nat_NativeGraph_DELETE
#define INSERT org_badiff_nat_NativeGraph_INSERT
#define NEXT org_badiff_nat_NativeGraph_NEXT

struct GraphData {
	char *flags;
	unsigned short int *lengths;
	int x_size;
	int y_size;
	signed char *x;
	signed char *y;
	int w_x;
	int w_y;
};

static struct GraphData *allocate_data(struct GraphData *data, signed char *x, int x_len, signed char *y, int y_len) {
	int x_size = x_len + 1, y_size = y_len + 1;

	data->x_size = x_size;
	data->y_size = y_size;

	if(data->x != NULL)
		free(data->x);
	if(data->y != NULL)
		free(data->y);

	data->x = malloc(x_size); memcpy(1 + data->x, x, x_len);
	data->y = malloc(y_size); memcpy(1 + data->y, y, y_len);

	return data;
}


static void free_data(struct GraphData *data) {
	if(data == NULL)
		return;

	if(data->x != NULL)
		free(data->x);
	if(data->y != NULL)
		free(data->y);

	free(data->lengths);
	free(data->flags);

	free(data);
}


static struct GraphData *get_data(JNIEnv *env, jobject this) {
	jclass cls = (*env)->GetObjectClass(env, this);
	jfieldID fld = (*env)->GetFieldID(env, cls, "data", "J");
	return (struct GraphData *) (*env)->GetLongField(env, this, fld);
}

static struct GraphData *set_data(JNIEnv *env, jobject this, struct GraphData *data) {
	jclass cls = (*env)->GetObjectClass(env, this);
	jfieldID fld = (*env)->GetFieldID(env, cls, "data", "J");
	(*env)->SetLongField(env, this, fld, (long)data);
	return data;
}

static void compute(struct GraphData *_data) {
	struct GraphData data = *_data;
	int x, y;

	for(y = 0; y < data.y_size; y++) {
		char *flags = data.flags + y * data.x_size;
		unsigned short *lengths = data.lengths + y * data.x_size;

		for(x = 0; x < data.x_size; x++) {
			if(x == 0 && y == 0) {
				flags[x] = STOP;
				lengths[x] = 0;
				continue;
			}
			if(x > 0 && y > 0 && data.x[x] == data.y[y]) {
				flags[x] = NEXT;
				lengths[x] = 1 + lengths[x-1-data.x_size];
				continue;
			}
			unsigned short dlen = (x > 0) ? 1 + lengths[x-1] : 65535;
			unsigned short ilen = (y > 0) ? 1 + data.lengths[x - data.x_size] : 65535;
			if(dlen <= ilen) {
				flags[x] = DELETE;
				lengths[x] = dlen;
			} else {
				flags[x] = INSERT;
				lengths[x] = ilen;
			}
		}
	}
}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    new0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_new0
  (JNIEnv *env, jobject this, jint bufSize) {
	struct GraphData *data = malloc(sizeof(struct GraphData));

	data->x = NULL;
	data->y = NULL;

	data->flags = malloc(bufSize);
	data->lengths = malloc(bufSize * sizeof(unsigned short));

	set_data(env, this, data);
}


/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    compute0
 * Signature: ([B[B)V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_compute0
(JNIEnv *env, jobject this, jbyteArray orig, jbyteArray target)
{
	struct GraphData *data = get_data(env, this);
	int x_len, y_len;
	signed char *x, *y;

	x_len = (*env)->GetArrayLength(env, orig);
	y_len = (*env)->GetArrayLength(env, target);

	x = (*env)->GetByteArrayElements(env, orig, NULL);
	y = (*env)->GetByteArrayElements(env, target, NULL);

	allocate_data(data, x, x_len, y, y_len);

	(*env)->ReleaseByteArrayElements(env, orig, x, JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env, target, y, JNI_ABORT);

	compute(data);
}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    walk0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_badiff_nat_NativeGraph_walk0
(JNIEnv *env, jobject this)
{
	struct GraphData *data = get_data(env, this);

	if(data == NULL)
		return JNI_FALSE;

	data->w_x = data->x_size - 1;
	data->w_y = data->y_size - 1;

	return JNI_TRUE;
}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    flag0
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_org_badiff_nat_NativeGraph_flag0
(JNIEnv *env, jobject this)
{
	struct GraphData *data = get_data(env, this);

	return data->flags[data->w_y * data->x_size + data->w_x];
}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    val0
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_org_badiff_nat_NativeGraph_val0
(JNIEnv *env, jobject this)
{
	struct GraphData *data = get_data(env, this);

	switch(data->flags[data->w_y * data->x_size + data->w_x]) {
	case DELETE:
		return data->x[data->w_x];
	case INSERT:
	case NEXT:
		return data->y[data->w_y];
	}
	return -1;
}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    prev0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_prev0
(JNIEnv *env, jobject this)
{
	struct GraphData *data = get_data(env, this);

	switch(data->flags[data->w_y * data->x_size + data->w_x]) {
	case DELETE:
		data->w_x--;
		break;
	case INSERT:
		data->w_y--;
		break;
	case NEXT:
		data->w_x--;
		data->w_y--;
		break;
	}

}

/*
 * Class:     org_badiff_nat_NativeGraph
 * Method:    free0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_badiff_nat_NativeGraph_free0
(JNIEnv *env, jobject this)
{
	struct GraphData *data = get_data(env, this);

	free_data(data);
}
